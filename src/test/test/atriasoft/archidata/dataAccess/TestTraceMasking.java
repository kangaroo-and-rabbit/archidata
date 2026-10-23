package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import test.atriasoft.archidata.ConfigureDb;

/** D-09: the TRACE logs of the reads mask the values of the protected fields. */
public class TestTraceMasking {

	/** Entity with a clear, a hidden and an encrypted field. */
	@Collection("traceMasking")
	public static class Traced {
		@Id
		public ObjectId _id = null;
		public String name;
		@DataNotRead
		public String hiddenToken;
		@DataEncrypt
		public String encrypted;
	}

	/** Embedded POJO with a hidden and an encrypted field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String passwordHash;
		@DataEncrypt
		public String pin;
	}

	/** Embedded record with a hidden component. */
	public record ApiKey(
			String label,
			@DataNotRead String secret) {}

	/** Concrete base class of an embedded value. */
	public static class Base {
		public String label;
	}

	/** Subclass adding a field the declared type {@link Base} does not know. */
	public static class Derived extends Base {
		public String extra;
	}

	/** Entity embedding an object, a list and a map of objects with protected fields. */
	@Collection("traceMaskingNested")
	public static class TracedNested {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public List<Credentials> history;
		public Map<String, ApiKey> map;
		public Base base;
		@DataNotRead
		public String hiddenToken;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Test
	public void testTraceMasksProtectedValues() throws Exception {
		final Traced entity = new Traced();
		entity.name = "visible-name";
		entity.hiddenToken = "hidden-token-value";
		entity.encrypted = "clear-secret";
		ConfigureDb.da.insert(entity);

		final Logger logger = (Logger) LoggerFactory.getLogger(DBAccessMongo.class);
		final Level previous = logger.getLevel();
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.TRACE);
		try {
			ConfigureDb.da.gets(Traced.class, new ReadAllColumn(), new Condition(
					Filters.and(Filters.eq("name", "visible-name"), Filters.eq("hiddenToken", "hidden-token-value"))));
		} finally {
			logger.setLevel(previous);
			logger.detachAppender(appender);
		}
		final String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
				.collect(Collectors.joining("\n"));
		Assertions.assertTrue(logs.contains("filter ="), logs);
		Assertions.assertTrue(logs.contains("receive data from DB"), logs);
		Assertions.assertTrue(logs.contains("visible-name"), logs);
		Assertions.assertTrue(logs.contains("***"), logs);
		Assertions.assertFalse(logs.contains("hidden-token-value"), logs);
		Assertions.assertFalse(logs.contains("clear-secret"), logs);
		final List<ILoggingEvent> traces = appender.list;
		Assertions.assertFalse(traces.isEmpty());
	}

	private static Credentials credentials(final String user, final String hash, final String pin) {
		final Credentials out = new Credentials();
		out.user = user;
		out.passwordHash = hash;
		out.pin = pin;
		return out;
	}

	/** Runs the action with the TRACE level on the data layer and returns what was logged. */
	private static String traceOf(final ThrowingRunnable action) throws Exception {
		final Logger logger = (Logger) LoggerFactory.getLogger(DBAccessMongo.class);
		final Level previous = logger.getLevel();
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.TRACE);
		try {
			action.run();
		} finally {
			logger.setLevel(previous);
			logger.detachAppender(appender);
		}
		return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining("\n"));
	}

	/** An action that may throw. */
	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	@Test
	public void testTraceMasksProtectedFieldsOfEmbeddedObjects() throws Exception {
		final TracedNested entity = new TracedNested();
		entity.name = "nested-name";
		entity.credentials = credentials("nested-user", "embedded-hash-value", "embedded-pin-value");
		entity.history = List.of(credentials("list-user", "list-hash-value", "list-pin-value"));
		entity.map = Map.of("k1", new ApiKey("map-label", "map-secret-value"));
		ConfigureDb.da.insert(entity);

		final String logs = traceOf(() -> ConfigureDb.da.gets(TracedNested.class, new ReadAllColumn(),
				new Condition(Filters.eq("name", "nested-name"))));
		Assertions.assertTrue(logs.contains("receive data from DB"), logs);
		// The visible values of the sub-documents stay readable.
		Assertions.assertTrue(logs.contains("nested-user"), logs);
		Assertions.assertTrue(logs.contains("list-user"), logs);
		Assertions.assertTrue(logs.contains("map-label"), logs);
		Assertions.assertTrue(logs.contains("***"), logs);
		// The protected ones, in an embedded object, a list and a map, are masked.
		Assertions.assertFalse(logs.contains("embedded-hash-value"), logs);
		Assertions.assertFalse(logs.contains("list-hash-value"), logs);
		Assertions.assertFalse(logs.contains("map-secret-value"), logs);
	}

	@Test
	public void testTraceMasksDottedFilterKeys() throws Exception {
		final String logs = traceOf(() -> {
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.eq("credentials.passwordHash", "dotted-hash-value")));
			ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("credentials.pin", "dotted-pin-value")));
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.eq("history.0.passwordHash", "indexed-hash-value")));
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.eq("history.passwordHash", "implicit-hash-value")));
			ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("map.k1.secret", "dotted-map-secret")));
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.and(Filters.eq("name", "other-name"), Filters.or(
							Filters.eq("credentials.passwordHash", "and-or-hash-value"),
							Filters.elemMatch("history", Filters.eq("passwordHash", "elem-match-hash-value"))))));
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.eq("credentials", new Document("passwordHash", "whole-hash-value"))));
		});
		Assertions.assertTrue(logs.contains("other-name"), logs);
		Assertions.assertTrue(logs.contains("***"), logs);
		for (final String secret : List.of("dotted-hash-value", "dotted-pin-value", "indexed-hash-value",
				"implicit-hash-value", "dotted-map-secret", "and-or-hash-value", "elem-match-hash-value",
				"whole-hash-value")) {
			Assertions.assertFalse(logs.contains(secret), secret + " leaked in: " + logs);
		}
	}

	@Test
	public void testTraceMasksRenamedProtectedColumn() throws Exception {
		final QueryOption rename = new OptionRenameColumn("hiddenToken", "renamedToken");
		final String logs = traceOf(() -> ConfigureDb.da.gets(TracedNested.class, rename, new Condition(
				Filters.and(Filters.eq("name", "rename-name"), Filters.eq("renamedToken", "renamed-secret-value")))));
		Assertions.assertTrue(logs.contains("rename-name"), logs);
		Assertions.assertFalse(logs.contains("renamed-secret-value"), logs);
	}

	@Test
	public void testTraceMasksProtectedFieldOfMapValueWithDollarKey() throws Exception {
		final TracedNested entity = new TracedNested();
		entity.name = "dollar-name";
		entity.map = Map.of("$x", new ApiKey("dollar-label", "dollar-secret-value"));
		ConfigureDb.da.insert(entity);

		// A document read from the database: "$x" is a map key, not an operator.
		final String readLogs = traceOf(
				() -> ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("name", "dollar-name"))));
		Assertions.assertTrue(readLogs.contains("dollar-label"), readLogs);
		Assertions.assertFalse(readLogs.contains("dollar-secret-value"), readLogs);

		// A filter: the same key, as a dotted path and inside a sub-document.
		final String filterLogs = traceOf(() -> {
			ConfigureDb.da.gets(TracedNested.class,
					new Condition(Filters.eq("map.$x.secret", "filter-dollar-secret-1")));
			ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("map", new Document("$x",
					new Document("label", "filter-label").append("secret", "filter-dollar-secret-2")))));
		});
		Assertions.assertTrue(filterLogs.contains("filter-label"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("filter-dollar-secret-1"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("filter-dollar-secret-2"), filterLogs);
	}

	@Test
	public void testTraceMasksUnknownFieldsOfEmbeddedObjects() throws Exception {
		// The writer encodes by the declared type: "extra" of the subclass is not stored.
		final TracedNested entity = new TracedNested();
		entity.name = "unknown-new-name";
		final Derived derived = new Derived();
		derived.label = "base-label";
		derived.extra = "extra-new-value";
		entity.base = derived;
		ConfigureDb.da.insert(entity);
		final Document stored = ConfigureDb.da.getInterface().getDatabase().getCollection("traceMaskingNested")
				.find(new Document("name", "unknown-new-name")).first();
		Assertions.assertNotNull(stored);
		Assertions.assertEquals(new Document("label", "base-label"), stored.get("base"));

		// A document written by an older version (which wrote the fields of the subclass): "extra" is in the
		// database, unknown to the model, and masked.
		ConfigureDb.da.getInterface().getDatabase().getCollection("traceMaskingNested")
				.insertOne(new Document("name", "unknown-name").append("base",
						new Document("label", "base-label").append("extra", "extra-secret-value")));

		final String readLogs = traceOf(
				() -> ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("name", "unknown-name"))));
		Assertions.assertTrue(readLogs.contains("base-label"), readLogs);
		Assertions.assertFalse(readLogs.contains("extra-secret-value"), readLogs);

		final String filterLogs = traceOf(() -> {
			ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("base.extra", "filter-extra-1")));
			ConfigureDb.da.gets(TracedNested.class, new Condition(
					Filters.eq("base", new Document("label", "filter-base-label").append("extra", "filter-extra-2"))));
		});
		Assertions.assertTrue(filterLogs.contains("filter-base-label"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("filter-extra-1"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("filter-extra-2"), filterLogs);
	}

	@Test
	public void testTraceMasksLegacyMapValuesAndExpressionOperators() throws Exception {
		// A map is transferred whole: a key of a value written by an older version is unknown, and masked.
		ConfigureDb.da.getInterface().getDatabase().getCollection("traceMaskingNested").insertOne(
				new Document("name", "raw-name").append("map", new Document("k", new Document("label", "raw-label")
						.append("secret", "raw-map-secret-value").append("legacy", "legacy-map-value"))));

		final String readLogs = traceOf(
				() -> ConfigureDb.da.gets(TracedNested.class, new Condition(Filters.eq("name", "raw-name"))));
		Assertions.assertTrue(readLogs.contains("raw-name"), readLogs);
		Assertions.assertTrue(readLogs.contains("raw-label"), readLogs);
		Assertions.assertFalse(readLogs.contains("raw-map-secret-value"), readLogs);
		Assertions.assertFalse(readLogs.contains("legacy-map-value"), readLogs);

		// $expr can name any field: the whole expression is masked.
		final String filterLogs = traceOf(() -> ConfigureDb.da.gets(TracedNested.class, new Condition(
				new Document("$expr", new Document("$eq", List.of("$hiddenToken", "expr-secret-value"))))));
		Assertions.assertTrue(filterLogs.contains("***"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("expr-secret-value"), filterLogs);
	}

	@Test
	public void testTraceMasksOperatorsOutsideThePositionAllowList() throws Exception {
		// $jsonSchema is not a position operator: its whole value is masked, the visible field is kept.
		final String filterLogs = traceOf(
				() -> ConfigureDb.da.gets(TracedNested.class,
						new Condition(new Document("name", "schema-visible-name").append("$jsonSchema", new Document(
								"properties",
								new Document("hiddenToken", new Document("enum", List.of("schema-secret-value"))))))));
		Assertions.assertTrue(filterLogs.contains("schema-visible-name"), filterLogs);
		Assertions.assertFalse(filterLogs.contains("schema-secret-value"), filterLogs);
	}
}
