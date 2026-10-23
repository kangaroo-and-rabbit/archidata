package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptedEnvelope;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Projections;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import test.atriasoft.archidata.ConfigureDb;

/**
 * Reads project the embedded objects down to their leaf fields: a protected sub-field the read does not return
 * is not transferred from the database, and the Java result stays the same.
 */
public class TestLeafProjection {
	private static final String COLLECTION = "leafProjection";
	private static final String PRESENCE = "._archidata_presence_";

	/** Embedded object with a hidden and an encrypted sub-field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String password;
		@DataEncrypt
		public String apiKey;
	}

	/** Reads {@link Credentials} with an {@code OptionSpecifyType}: the encrypted sub-field declared plain. */
	public static class PlainApiKey {
		public String user;
		public String apiKey;
	}

	/** Reads {@link Credentials} with an {@code OptionSpecifyType}: the encrypted sub-field declared encrypted. */
	public static class EncryptedApiKey {
		public String user;
		@DataEncrypt
		public String apiKey;
	}

	/** Reads {@link Credentials} with an {@code OptionSpecifyType}: the encrypted sub-field declared as a number. */
	public static class NumberApiKey {
		public String user;
		public Integer apiKey;
	}

	/** Embedded object without protected field. */
	public static class Plain {
		public String label;
		public Integer size;
	}

	/** Embedded object whose only field is hidden. */
	public static class SecretOnly {
		@DataNotRead
		public String secret;
	}

	/** Embedded object holding an embedded object with protected fields. */
	public static class Holder {
		public String name;
		public Credentials credentials;
	}

	/** Recursive embedded type. */
	public static class Node {
		public String label;
		@DataNotRead
		public String secret;
		public Node child;
	}

	/** Wide embedded types: 4 * 4 * 4 * 5 = 320 leaf paths, more than the budget of one field. */
	public static class Wide4 {
		public String a;
		public String b;
		public String c;
		public String d;
		public String e;
	}

	public static class Wide3 {
		public Wide4 a;
		public Wide4 b;
		public Wide4 c;
		public Wide4 d;
	}

	public static class Wide2 {
		public Wide3 a;
		public Wide3 b;
		public Wide3 c;
		public Wide3 d;
	}

	public static class Wide1 {
		public Wide2 a;
		public Wide2 b;
		public Wide2 c;
		public Wide2 d;
	}

	/** Binary tree with a protected sub-field: 255 labels and 256 cut positions, more than the budget. */
	public static class Branch {
		public String label;
		@DataNotRead
		public String secret;
		public Branch left;
		public Branch right;
	}

	@Collection(COLLECTION)
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public Plain plain;
		public SecretOnly secretOnly;
		public Holder holder;
		public List<Credentials> history;
		public List<List<Credentials>> historyGrid;
		public List<Plain> plains;
		public Map<String, Credentials> byKey;
		public Map<String, Document> docs;
		public Document free;
		public Node node;
		public Wide1 wide;
		public Branch branch;
	}

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
		ConfigBaseVariable.setDataDecryptEnable("false");
	}

	@AfterEach
	public void resetDecrypt() {
		ConfigBaseVariable.setDataDecryptEnable("false");
	}

	@AfterAll
	public static void clear() throws IOException {
		ConfigBaseVariable.setDataDecryptEnable("false");
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	private static List<String> projection(final QueryOption... options) throws Exception {
		final List<String> out = ConfigureDb.da.generateSelectField(Account.class, new QueryOptions(options));
		assertNoCollision(out);
		return out;
	}

	private static Set<String> projectionSet(final QueryOption... options) throws Exception {
		return Set.copyOf(projection(options));
	}

	private static Credentials credentials(final String user) {
		final Credentials out = new Credentials();
		out.user = user;
		out.password = user + "-password";
		out.apiKey = user + "-apikey";
		return out;
	}

	private static void assertNoCollision(final List<String> paths) {
		for (final String path : paths) {
			for (final String other : paths) {
				Assertions.assertFalse(other.startsWith(path + "."), path + " and " + other + " in " + paths);
			}
			Assertions.assertEquals(1, paths.stream().filter(path::equals).count(), path + " twice in " + paths);
		}
	}

	@Test
	public void testEmbeddedObjectIsProjectedToItsLeaves() throws Exception {
		final List<String> paths = projection();
		assertNoCollision(paths);
		Assertions.assertTrue(paths.contains("credentials.user"), paths.toString());
		Assertions.assertFalse(paths.contains("credentials"), paths.toString());
		Assertions.assertFalse(paths.contains("credentials.password"), paths.toString());
		// Decryption disabled: the encrypted value would be left null, it does not travel either.
		Assertions.assertFalse(paths.contains("credentials.apiKey"), paths.toString());
		Assertions.assertTrue(paths.containsAll(List.of("plain.label", "plain.size", "holder.name",
				"holder.credentials.user", "secretOnly" + PRESENCE)), paths.toString());
		// Maps and free-form content are included whole.
		Assertions.assertTrue(paths.containsAll(List.of("byKey", "docs", "free", "name", "_id")), paths.toString());
		// A list is developed only when the read hides a sub-field of its elements.
		Assertions.assertTrue(paths.containsAll(List.of("history.user", "historyGrid.user", "plains")),
				paths.toString());
		Assertions.assertFalse(paths.contains("history"), paths.toString());

		ConfigBaseVariable.setDataDecryptEnable("true");
		final List<String> decrypted = projection();
		Assertions.assertTrue(decrypted.contains("credentials.apiKey"), decrypted.toString());
		Assertions.assertFalse(decrypted.contains("credentials.password"), decrypted.toString());

		final List<String> all = projection(new ReadAllColumn());
		assertNoCollision(all);
		Assertions.assertTrue(all.containsAll(List.of("credentials.user", "credentials.password", "credentials.apiKey",
				"secretOnly.secret", "holder.credentials.password")), all.toString());
		// Nothing hidden in the elements any more: the lists are read whole (their null elements kept).
		Assertions.assertTrue(all.containsAll(List.of("history", "historyGrid")), all.toString());
	}

	@Test
	public void testRecursiveAndWideTypesAreCut() throws Exception {
		final List<String> paths = projection();
		final String child = ".child";
		Assertions.assertTrue(paths.contains("node" + child.repeat(7) + ".label"), paths.toString());
		// Eight levels below the field: included whole.
		Assertions.assertTrue(paths.contains("node" + child.repeat(8)), paths.toString());
		for (final String path : paths) {
			Assertions.assertFalse(path.startsWith("node" + child.repeat(8) + "."), path);
		}
		// More than 256 leaves for one field: included whole.
		Assertions.assertTrue(paths.contains("wide"), paths.toString());
		Assertions.assertFalse(paths.stream().anyMatch(path -> path.startsWith("wide.")), paths.toString());
		Assertions.assertTrue(paths.contains("branch"), paths.toString());
		Assertions.assertFalse(paths.stream().anyMatch(path -> path.startsWith("branch.")), paths.toString());
	}

	@Test
	public void testAWholePartHoldingAProtectedSubFieldIsWarnedOncePerField() throws Exception {
		final Logger logger = (Logger) LoggerFactory
				.getLogger("org.atriasoft.archidata.dataAccess.model.LeafProjection");
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			// Forgets the paths and the WARNs already written by the other tests.
			DbClassModel.clearCache();
			projection();
			projection();
			projection(new FilterValue("node.child"));
			projection(new FilterValue("wide.a"));
		} finally {
			logger.detachAppender(appender);
		}
		final List<String> warns = appender.list.stream().filter(event -> event.getLevel() == Level.WARN)
				.map(ILoggingEvent::getFormattedMessage).toList();
		final List<String> debugs = appender.list.stream().filter(event -> event.getLevel() == Level.DEBUG)
				.map(ILoggingEvent::getFormattedMessage).toList();
		final String prefix = "Field " + Account.class.getName() + ".";
		// node: cut at 8 levels, the part included whole holds Node.secret; once, whatever the reads.
		// branch: more than 256 paths, included whole with Branch.secret.
		Assertions.assertEquals(2, warns.size(), warns.toString());
		Assertions.assertTrue(
				warns.stream()
						.anyMatch(message -> message.startsWith(prefix + "node ") && message.contains(
								"projected whole at 'node" + ".child".repeat(8) + "' because it is deeper than 8")),
				warns.toString());
		Assertions
				.assertTrue(
						warns.stream()
								.anyMatch(message -> message.startsWith(prefix + "branch ") && message.contains(
										"projected whole at 'branch' because it develops into more than 256")),
						warns.toString());
		// wide: nothing protected below, a DEBUG only.
		Assertions.assertFalse(warns.stream().anyMatch(message -> message.contains("wide")), warns.toString());
		Assertions.assertTrue(
				debugs.stream()
						.anyMatch(message -> message.startsWith("Field 'wide'") && message.endsWith("projected whole")),
				debugs.toString());
	}

	@Test
	public void testFilterValuePathsAreResolvedInTheModel() throws Exception {
		Assertions.assertEquals(Set.of("_id", "credentials.user"), projectionSet(new FilterValue("credentials")));
		Assertions.assertEquals(Set.of("_id", "credentials" + PRESENCE),
				projectionSet(new FilterValue("credentials.password")));
		Assertions.assertEquals(Set.of("_id", "credentials.password"),
				projectionSet(new ReadAllColumn(), new FilterValue("credentials.password")));
		// A key the model does not know is not transferred any more.
		Assertions.assertEquals(Set.of("_id", "credentials" + PRESENCE),
				projectionSet(new FilterValue("credentials.legacyExtra")));
		// Through a list (no segment consumed), a map key, and down to the leaves of the value.
		Assertions.assertEquals(Set.of("_id", "history.user"), projectionSet(new FilterValue("history.user")));
		Assertions.assertEquals(Set.of("_id", "byKey.k.user"), projectionSet(new FilterValue("byKey.k")));
		Assertions.assertEquals(Set.of("_id", "holder.credentials.user"),
				projectionSet(new FilterValue("holder.credentials")));
		// Below free-form content the path is pushed as it is.
		Assertions.assertEquals(Set.of("_id", "free.any.path"), projectionSet(new FilterValue("free.any.path")));
		// The widest path wins, never both.
		Assertions.assertEquals(Set.of("_id", "docs.k"), projectionSet(new FilterValue("docs.k", "docs.k.x")));
		Assertions.assertEquals(Set.of("_id", "credentials.user"),
				projectionSet(new FilterValue("credentials", "credentials.user")));
	}

	@Test
	public void testProtectedSubFieldsDoNotTransit() throws Exception {
		final Account account = new Account();
		account.name = "transit";
		account.credentials = credentials("root");
		account.secretOnly = new SecretOnly();
		account.secretOnly.secret = "only-secret";
		account.holder = new Holder();
		account.holder.name = "holder";
		account.holder.credentials = credentials("held");
		account.history = new ArrayList<>(Arrays.asList(credentials("h0"), null, credentials("h2")));
		account.plains = new ArrayList<>(Arrays.asList(new Plain(), null));
		final Account inserted = ConfigureDb.da.insert(account);

		// The raw document MongoDB answers to the projection archidata sends.
		final Document received = ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("_id", inserted._id)).projection(Projections.include(projection())).first();
		final String json = received.toJson();
		for (final String secret : List.of("root-password", "only-secret", "held-password", "h0-password",
				"h2-password", "\"password\"", "\"secret\"", "\"apiKey\"")) {
			Assertions.assertFalse(json.contains(secret), secret + " transited: " + json);
		}
		Assertions.assertEquals(new Document(), received.get("secretOnly"));

		// Same Java result as before, except the null elements of a list developed to its leaves.
		final Account read = ConfigureDb.da.getById(Account.class, inserted._id);
		Assertions.assertEquals("root", read.credentials.user);
		Assertions.assertNull(read.credentials.password);
		Assertions.assertNull(read.credentials.apiKey);
		Assertions.assertNotNull(read.secretOnly);
		Assertions.assertNull(read.secretOnly.secret);
		Assertions.assertEquals("held", read.holder.credentials.user);
		Assertions.assertEquals(2, read.history.size());
		Assertions.assertEquals("h2", read.history.get(1).user);
		Assertions.assertEquals(2, read.plains.size());
		Assertions.assertNull(read.plains.get(1));
		Assertions.assertNull(read.node);

		ConfigBaseVariable.setDataDecryptEnable("true");
		final Account all = ConfigureDb.da.getById(Account.class, inserted._id, new ReadAllColumn());
		Assertions.assertEquals("root-password", all.credentials.password);
		Assertions.assertEquals("root-apikey", all.credentials.apiKey);
		Assertions.assertEquals("only-secret", all.secretOnly.secret);
		Assertions.assertEquals("held-password", all.holder.credentials.password);
		Assertions.assertEquals(3, all.history.size());
		Assertions.assertNull(all.history.get(1));
		Assertions.assertEquals("h2-password", all.history.get(2).password);

		// A partial read naming a hidden sub-field rebuilds the object empty, as before.
		final Account partial = ConfigureDb.da.getById(Account.class, inserted._id,
				new FilterValue("credentials.password"));
		Assertions.assertNotNull(partial.credentials);
		Assertions.assertNull(partial.credentials.user);
		Assertions.assertNull(partial.credentials.password);
	}

	@Test
	public void testLegacyKeysOfEmbeddedObjectsDoNotTransit() throws Exception {
		// Written by a version up to v0.51.3 (a subclass stored its own fields).
		final ObjectId id = new ObjectId();
		ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.insertOne(new Document("_id", id).append("name", "legacy")
						.append("credentials", new Document("user", "u").append("subclassSecret", "legacy-secret"))
						.append("secretOnly", null).append("plain", "not a document"));
		final Document received = ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("_id", id)).projection(Projections.include(projection())).first();
		Assertions.assertFalse(received.toJson().contains("legacy-secret"), received.toJson());
		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertEquals("u", read.credentials.user);
		Assertions.assertNull(read.secretOnly);
		// A scalar stored where an embedded object is declared is not returned by a sub-path projection.
		Assertions.assertNull(read.plain);
	}

	/** The first element of the history, decoded with the class of the option (not a Credentials). */
	private static Object firstOfHistory(final ObjectId id, final OptionSpecifyType option) throws Exception {
		final List<?> history = ConfigureDb.da.getById(Account.class, id, option).history;
		return history.get(0);
	}

	@Test
	public void testOptionSpecifyTypeOnAnEncryptedSubField() throws Exception {
		final Account account = new Account();
		account.history = new ArrayList<>(List.of(credentials("h0")));
		final ObjectId id = ConfigureDb.da.insert(account)._id;
		final OptionSpecifyType asPlain = new OptionSpecifyType("history", PlainApiKey.class, true);
		final OptionSpecifyType asEncrypted = new OptionSpecifyType("history", EncryptedApiKey.class, true);
		final OptionSpecifyType asNumber = new OptionSpecifyType("history", NumberApiKey.class, true);

		// Decryption disabled: the encrypted sub-field of Credentials is neither transferred nor returned.
		Assertions.assertFalse(projection(asPlain).contains("history.apiKey"));
		Assertions.assertNull(((PlainApiKey) firstOfHistory(id, asPlain)).apiKey);
		Assertions.assertNull(((NumberApiKey) firstOfHistory(id, asNumber)).apiKey);

		// Decryption enabled: transferred, then decoded by the reader of the class of the option, which decrypts only
		// when that class marks the sub-field @DataEncrypt too.
		ConfigBaseVariable.setDataDecryptEnable("true");
		Assertions.assertTrue(projection(asPlain).contains("history.apiKey"));
		final String stored = ((PlainApiKey) firstOfHistory(id, asPlain)).apiKey;
		Assertions.assertNotEquals("h0-apikey", stored);
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(stored), stored);
		Assertions.assertEquals("h0-apikey", ((EncryptedApiKey) firstOfHistory(id, asEncrypted)).apiKey);
		// Another type than String cannot decode the stored envelope: the read fails.
		Assertions.assertThrows(DataAccessException.class, () -> firstOfHistory(id, asNumber));
	}
}
