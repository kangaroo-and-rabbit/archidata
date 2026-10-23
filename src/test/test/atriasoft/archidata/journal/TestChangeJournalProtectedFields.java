package test.atriasoft.archidata.journal;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.journal.ChangeJournalEngine;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-03: the journal never copies the {@code @DataNotRead} fields, and keeps {@code @DataEncrypt} encrypted. */
public class TestChangeJournalProtectedFields {
	private static final String COLLECTION = "journalProtected";
	private static final String NESTED_COLLECTION = "journalProtectedNested";
	private static final String POLY_COLLECTION = "journalProtectedPoly";
	private static final String BASE_COLLECTION = "journalProtectedBase";
	private static final String MULTI_COLLECTION = "journalProtectedMulti";
	private static final String RAW_COLLECTION = "journalProtectedRaw";

	/** Journalized entity with secrets. */
	@Collection(COLLECTION)
	public static class Account extends OIDGenericDataSoftDelete {
		public String login;
		@DataNotRead
		public String passwordHash;
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
		public Credentials parent;
	}

	/** Embedded record with a hidden component. */
	public record ApiKey(
			String label,
			@DataNotRead String secret) {}

	/** Journalized entity with secrets inside embedded objects. */
	@Collection(NESTED_COLLECTION)
	public static class NestedAccount extends OIDGenericDataSoftDelete {
		public String login;
		public Credentials credentials;
		public List<Credentials> history;
		public Map<String, ApiKey> keys;
		public Map<String, List<Credentials>> keyHistory;
	}

	/** Concrete base class of an embedded value. */
	public static class Base {
		public String label;
	}

	/** Entity with a top-level polymorphic field: refused when its model is analysed. */
	@Collection(POLY_COLLECTION)
	public static class PolyAccount extends OIDGenericDataSoftDelete {
		public String login;
		public Object payload;
	}

	/** Entity with a concrete base class that holds a subclass. */
	@Collection(BASE_COLLECTION)
	public static class BaseHolder extends OIDGenericDataSoftDelete {
		public String login;
		public Base base;
	}

	/** Embedded class holding a polymorphic field. */
	public static class Wrapper {
		public String name;
		public Object inner;
	}

	/** Entity with a raw map and an embedded polymorphic field: refused when its model is analysed. */
	@Collection(RAW_COLLECTION)
	public static class RawAccount extends OIDGenericDataSoftDelete {
		public String login;
		@SuppressWarnings("rawtypes")
		public Map rawMap;
		public Wrapper wrapper;
	}

	/** First model of a collection shared by two classes. */
	@Collection(MULTI_COLLECTION)
	public static class MultiA extends OIDGenericDataSoftDelete {
		public Credentials credentials;
	}

	/** Second model of the same collection, unaware of the field of the first one. */
	@Collection(MULTI_COLLECTION)
	public static class MultiB extends OIDGenericDataSoftDelete {
		public Base base;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
		JournalTestTools.dropJournal();
	}

	@BeforeEach
	public void resetJournal() {
		JournalTestTools.dropJournal();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Test
	public void testProtectedFieldsAreNotJournalized() throws Exception {
		final Account account = new Account();
		account.login = "bob";
		account.passwordHash = "hash-secret-value";
		account.encrypted = "clear-encrypted-value";
		ConfigureDb.da.insert(account);
		// Another collection captured by name, with an explicit exclusion.
		JournalTestTools.database().getCollection("journalRawTokens")
				.insertOne(new Document("token", "raw-token").append("label", "kept").append("updatedAt", new Date()));

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(Account.class);
		engine.addCollection("journalRawTokens");
		engine.excludeFields("journalRawTokens", "token");
		Assertions.assertEquals(Set.of("passwordHash"), engine.getExcludedFields(COLLECTION));
		engine.run();

		final List<Document> entries = JournalTestTools.journalEntries();
		Assertions.assertEquals(2, entries.size());
		for (final Document entry : entries) {
			final String data = entry.getString("data");
			Assertions.assertFalse(data.contains("hash-secret-value"), data);
			Assertions.assertFalse(data.contains("clear-encrypted-value"), data);
			Assertions.assertFalse(data.contains("raw-token"), data);
			if (COLLECTION.equals(entry.getString("collectionName"))) {
				final Document captured = Document.parse(data);
				Assertions.assertEquals("bob", captured.getString("login"));
				Assertions.assertFalse(captured.containsKey("passwordHash"));
				// Stored (encrypted) form, and the system fields are kept.
				Assertions.assertNotNull(captured.get("encrypted"));
				Assertions.assertTrue(captured.containsKey("updatedAt"));
				Assertions.assertTrue(captured.containsKey("deleted"));
			} else {
				Assertions.assertTrue(data.contains("kept"), data);
			}
		}
	}

	private static Credentials credentials(final String user, final String hash, final String pin) {
		final Credentials out = new Credentials();
		out.user = user;
		out.passwordHash = hash;
		out.pin = pin;
		return out;
	}

	/** CS-06: the {@code @DataNotRead} fields of embedded objects, lists and maps are not journalized. */
	@Test
	public void testNestedProtectedFieldsAreNotJournalized() throws Exception {
		final NestedAccount account = new NestedAccount();
		account.login = "alice";
		account.credentials = credentials("alice-user", "embedded-hash-secret", "embedded-pin-clear");
		account.credentials.parent = credentials("parent-user", "parent-hash-secret", "parent-pin-clear");
		account.history = List.of(credentials("old-1", "list-hash-secret-1", "list-pin-clear-1"),
				credentials("old-2", "list-hash-secret-2", "list-pin-clear-2"));
		account.keys = Map.of("main", new ApiKey("main-label", "map-secret-value"));
		account.keyHistory = Map.of("k", List.of(credentials("deep-user", "deep-hash-secret", "deep-pin-clear")));
		ConfigureDb.da.insert(account);

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(NestedAccount.class);
		engine.run();

		final List<Document> entries = JournalTestTools.journalEntries();
		Assertions.assertEquals(1, entries.size());
		final String data = entries.get(0).getString("data");
		for (final String secret : List.of("embedded-hash-secret", "parent-hash-secret", "list-hash-secret-1",
				"list-hash-secret-2", "map-secret-value", "deep-hash-secret")) {
			Assertions.assertFalse(data.contains(secret), secret + " journalized in: " + data);
		}
		// The encrypted fields are never copied in clear.
		for (final String clear : List.of("embedded-pin-clear", "list-pin-clear-1", "deep-pin-clear")) {
			Assertions.assertFalse(data.contains(clear), clear + " journalized in: " + data);
		}

		final Document captured = Document.parse(data);
		Assertions.assertEquals("alice", captured.getString("login"));
		final Document credentials = captured.get("credentials", Document.class);
		Assertions.assertEquals("alice-user", credentials.getString("user"));
		Assertions.assertFalse(credentials.containsKey("passwordHash"));
		Assertions.assertNotNull(credentials.get("pin"));
		final Document parent = credentials.get("parent", Document.class);
		Assertions.assertEquals("parent-user", parent.getString("user"));
		Assertions.assertFalse(parent.containsKey("passwordHash"));
		final List<Document> history = captured.getList("history", Document.class);
		Assertions.assertEquals(2, history.size());
		Assertions.assertEquals("old-1", history.get(0).getString("user"));
		Assertions.assertFalse(history.get(0).containsKey("passwordHash"));
		Assertions.assertFalse(history.get(1).containsKey("passwordHash"));
		final Document main = captured.get("keys", Document.class).get("main", Document.class);
		Assertions.assertEquals("main-label", main.getString("label"));
		Assertions.assertFalse(main.containsKey("secret"));
		final Document deep = captured.get("keyHistory", Document.class).getList("k", Document.class).get(0);
		Assertions.assertEquals("deep-user", deep.getString("user"));
		Assertions.assertFalse(deep.containsKey("passwordHash"));
		// The system fields are kept.
		Assertions.assertTrue(captured.containsKey("updatedAt"));
		Assertions.assertTrue(captured.containsKey("deleted"));
	}

	private static String capturedData(final ChangeJournalEngine engine) throws Exception {
		engine.run();
		final List<Document> entries = JournalTestTools.journalEntries();
		Assertions.assertEquals(1, entries.size());
		return entries.get(0).getString("data");
	}

	/** An entity with an Object field, a raw map or an embedded Object field cannot be journalized: refused. */
	@Test
	public void testPolymorphicModelsAreRefused() throws Exception {
		for (final Class<?> clazz : List.of(PolyAccount.class, RawAccount.class)) {
			final ChangeJournalEngine engine = new ChangeJournalEngine();
			final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
					() -> engine.addClass(clazz));
			Throwable cause = ex;
			while (cause.getCause() != null) {
				cause = cause.getCause();
			}
			Assertions.assertTrue(cause.getMessage().startsWith("Unsupported field type on " + clazz.getName()),
					cause.getMessage());
		}
	}

	/** A field written by an older version under a concrete base class, unknown to it, is not journalized. */
	@Test
	public void testFieldAddedByASubclassIsNotJournalized() throws Exception {
		// Today the writer stores the fields of the declared type only: the document comes from an older version.
		JournalTestTools.database().getCollection(BASE_COLLECTION).deleteMany(new Document());
		JournalTestTools.database().getCollection(BASE_COLLECTION)
				.insertOne(new Document("login", "dave")
						.append("base", new Document("label", "base-label").append("extra", "extra-secret-value"))
						.append("updatedAt", new Date()));

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(BaseHolder.class);
		final String data = capturedData(engine);
		Assertions.assertFalse(data.contains("extra-secret-value"), data);
		Assertions.assertEquals("base-label", Document.parse(data).get("base", Document.class).getString("label"));
	}

	/** Two classes on one collection: every one of them is applied, the last one does not replace the first. */
	@Test
	public void testSeveralClassesOnOneCollectionAreAllApplied() throws Exception {
		JournalTestTools.database().getCollection(MULTI_COLLECTION).deleteMany(new Document());
		JournalTestTools.database().getCollection(MULTI_COLLECTION)
				.insertOne(new Document("credentials",
						new Document("user", "multi-user").append("passwordHash", "multi-hash-secret"))
								.append("base", new Document("label", "multi-label")).append("updatedAt", new Date()));

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(MultiA.class, MultiB.class);
		final String data = capturedData(engine);
		Assertions.assertFalse(data.contains("multi-hash-secret"), data);
		Assertions.assertTrue(data.contains("multi-label"), data);
	}
}
