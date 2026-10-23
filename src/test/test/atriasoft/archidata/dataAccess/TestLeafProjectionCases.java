package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.dataAccess.options.Skip;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;

import test.atriasoft.archidata.ConfigureDb;

/**
 * Cases of the leaf projection of the reads: null list elements against the updates, records, renamed and
 * inherited sub-fields, sets, map keys, relation expansion, {@code getsRaw}, pagination, {@code OptionSpecifyType}
 * and the cache of the paths.
 */
public class TestLeafProjectionCases {
	private static final String COLLECTION = "leafProjectionCases";
	private static final String TARGET = "leafProjectionTarget";
	private static final String OWNER = "leafProjectionOwner";
	private static final String FOREST = "leafProjectionForest";
	private static final String FOLDER = "leafProjectionFolder";
	private static final String PRESENCE = "_archidata_presence_";

	/** Embedded object with a hidden sub-field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String password;
	}

	/** Record with a hidden component. */
	public record RecordCredentials(
			String user,
			@DataNotRead String password) {}

	/** Sub-fields stored under another name. */
	public static class Renamed {
		@Field("u")
		public String user;
		@DataNotRead
		@Field("pw")
		public String password;
	}

	/** Hidden sub-field declared by the parent class. */
	public static class BaseCredentials {
		public String user;
		@DataNotRead
		public String password;
	}

	/** Declared type of the field: inherits the sub-fields of its parent. */
	public static class ChildCredentials extends BaseCredentials {
		public String extra;
	}

	@Collection(COLLECTION)
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public Credentials backup;
		public List<Credentials> history;
		public RecordCredentials rec;
		public Renamed renamed;
		public ChildCredentials child;
		public Set<Credentials> credSet;
		public Map<String, Credentials> byKey;
	}

	/**
	 * Class an {@code OptionSpecifyType} reads {@link Credentials} with: declares the protected sub-field without
	 * protection, and a sub-field {@code Credentials} does not know.
	 */
	public static class OpenCredentials {
		public String user;
		public String password;
		public String extra;
	}

	/** Class an {@code OptionSpecifyType} reads {@link Credentials} with: does not declare the protected sub-field. */
	public static class UserOnly {
		public String user;
	}

	/** Element holding an embedded object with a protected sub-field. */
	public static class Holder {
		public String name;
		public Credentials credentials;
	}

	/** Reads {@link Holder}: its embedded object does not declare the protected sub-field. */
	public static class UserOnlyHolder {
		public String name;
		public UserOnly credentials;
	}

	@Collection(FOLDER)
	public static class Folder {
		@Id
		public ObjectId _id = null;
		public List<Holder> holders;
	}

	/** Recursive embedded type with a protected sub-field at every level. */
	public static class Node {
		public String label;
		@DataNotRead
		public String secret;
		public Node child;
	}

	/** The same tree without protection. */
	public static class OpenNode {
		public String label;
		public String secret;
		public OpenNode child;
	}

	@Collection(FOREST)
	public static class Forest {
		@Id
		public ObjectId _id = null;
		public List<Node> nodes;
	}

	@Collection(TARGET)
	public static class Target {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
	}

	@Collection(OWNER)
	public static class Owner {
		@Id
		public ObjectId _id = null;
		public String label;
		@ManyToOneDoc(targetEntity = Target.class, remoteField = "", addLinkWhenCreate = false, removeLinkWhenDelete = false, updateLinkWhenUpdate = false)
		@Field("targetId")
		public Target target;
	}

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void clear() throws IOException {
		ConfigureDb.clear();
	}

	private static MongoDatabase database() {
		return ConfigureDb.da.getInterface().getDatabase();
	}

	private static MongoCollection<Document> raw(final String name) {
		return database().getCollection(name);
	}

	private static List<String> projection(final QueryOption... options) throws Exception {
		return ConfigureDb.da.generateSelectField(Account.class, new QueryOptions(options));
	}

	private static Set<String> projectionSet(final QueryOption... options) throws Exception {
		return Set.copyOf(projection(options));
	}

	private static Credentials credentials(final String user) {
		final Credentials out = new Credentials();
		out.user = user;
		out.password = user + "-password";
		return out;
	}

	@Test
	public void testNullListElementsAgainstTheUpdates() throws Exception {
		final Account account = new Account();
		account.name = "nulls";
		account.history = new ArrayList<>(Arrays.asList(credentials("h0"), null, credentials("h2")));
		final ObjectId id = ConfigureDb.da.insert(account)._id;
		final Document before = raw(COLLECTION).find(new Document("_id", id)).first();
		Assertions.assertEquals(3, before.getList("history", Object.class).size());

		// The default read projects the list down to its leaves: MongoDB drops the null element, indexes shift.
		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertEquals(2, read.history.size());
		Assertions.assertEquals("h2", read.history.get(1).user);
		Assertions.assertNull(read.history.get(1).password);

		// A full update does not write a list of objects holding protected sub-fields: the document is unchanged.
		ConfigureDb.da.updateById(read, id);
		Assertions.assertEquals(before, raw(COLLECTION).find(new Document("_id", id)).first());

		// Naming the list rewrites it from what was read: the null element is gone, h2 moves from index 2 to 1,
		// and the hidden sub-fields of the elements (never read) are lost, as before this version.
		ConfigureDb.da.updateById(read, id, new FilterValue(List.of("history")));
		final List<Document> after = raw(COLLECTION).find(new Document("_id", id)).first().getList("history",
				Document.class);
		Assertions.assertEquals(2, after.size());
		Assertions.assertEquals("h2", after.get(1).get("user"));
		Assertions.assertFalse(after.get(0).containsKey("password"), after.toString());
	}

	@Test
	public void testRecordsRenamesInheritanceAndSets() throws Exception {
		final List<String> paths = projection();
		Assertions.assertTrue(paths.containsAll(List.of("rec.user", "renamed.u", "child.user", "child.extra",
				"credSet.user", "credentials.user", "backup.user")), paths.toString());
		for (final String hidden : List.of("rec", "rec.password", "renamed", "renamed.pw", "child", "child.password",
				"credSet", "credSet.password", "credentials", "backup")) {
			Assertions.assertFalse(paths.contains(hidden), hidden + " in " + paths);
		}
		Assertions.assertTrue(
				projection(new ReadAllColumn()).containsAll(
						List.of("rec.password", "renamed.pw", "child.password", "credSet", "credentials.password")),
				projection(new ReadAllColumn()).toString());

		final Account account = new Account();
		account.rec = new RecordCredentials("r", "r-password");
		account.renamed = new Renamed();
		account.renamed.user = "n";
		account.renamed.password = "n-password";
		account.child = new ChildCredentials();
		account.child.user = "c";
		account.child.password = "c-password";
		account.child.extra = "c-extra";
		account.credSet = new HashSet<>(List.of(credentials("s1"), credentials("s2")));
		final ObjectId id = ConfigureDb.da.insert(account)._id;

		final Document received = raw(COLLECTION).find(new Document("_id", id)).projection(Projections.include(paths))
				.first();
		Assertions.assertFalse(received.toJson().contains("password"), received.toJson());
		Assertions.assertEquals(new Document("u", "n"), received.get("renamed"));

		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertEquals("r", read.rec.user());
		Assertions.assertNull(read.rec.password());
		Assertions.assertEquals("n", read.renamed.user);
		Assertions.assertNull(read.renamed.password);
		Assertions.assertEquals("c", read.child.user);
		Assertions.assertEquals("c-extra", read.child.extra);
		Assertions.assertNull(read.child.password);
		Assertions.assertEquals(2, read.credSet.size());
		for (final Credentials one : read.credSet) {
			Assertions.assertNotNull(one.user);
			Assertions.assertNull(one.password);
		}

		final Account all = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("r-password", all.rec.password());
		Assertions.assertEquals("n-password", all.renamed.password);
		Assertions.assertEquals("c-password", all.child.password);
	}

	@Test
	public void testFilterValueOnAMapKey() throws Exception {
		Assertions.assertEquals(Set.of("_id", "byKey.k.user"), projectionSet(new FilterValue("byKey.k")));
		Assertions.assertEquals(Set.of("_id", "byKey.k.user", "byKey.k.password"),
				projectionSet(new ReadAllColumn(), new FilterValue("byKey.k")));

		final Account account = new Account();
		account.byKey = new LinkedHashMap<>();
		account.byKey.put("k", credentials("k"));
		account.byKey.put("other", credentials("o"));
		final ObjectId id = ConfigureDb.da.insert(account)._id;

		final Account read = ConfigureDb.da.getById(Account.class, id, new FilterValue("byKey.k"));
		Assertions.assertEquals(Set.of("k"), read.byKey.keySet());
		Assertions.assertEquals("k", read.byKey.get("k").user);
		Assertions.assertNull(read.byKey.get("k").password);
		final Account all = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn(), new FilterValue("byKey.k"));
		Assertions.assertEquals(Set.of("k"), all.byKey.keySet());
		Assertions.assertEquals("k-password", all.byKey.get("k").password);
		// Without FilterValue a map is read whole (its keys are dynamic), the hidden sub-fields dropped in Java.
		final Account whole = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertEquals(Set.of("k", "other"), whole.byKey.keySet());
		Assertions.assertNull(whole.byKey.get("other").password);
	}

	@Test
	public void testRelationExpansionGetsRawAndPaginationSendTheLeafProjection() throws Exception {
		final ObjectId targetId = new ObjectId();
		raw(TARGET).insertOne(new Document("_id", targetId).append("name", "t").append("credentials",
				new Document("user", "t-user").append("password", "t-password")));
		final ObjectId ownerId = new ObjectId();
		raw(OWNER).insertOne(new Document("_id", ownerId).append("label", "o").append("targetId", targetId));

		final String namespace = database().getName() + "." + TARGET;
		database().runCommand(new Document("profile", 2));
		try {
			final Owner owner = ConfigureDb.da.getById(Owner.class, ownerId);
			Assertions.assertEquals("t-user", owner.target.credentials.user);
			Assertions.assertNull(owner.target.credentials.password);
			final List<Object> rawList = ConfigureDb.da.getsRaw(Target.class);
			Assertions.assertEquals(1, rawList.size());
			final List<Target> page = ConfigureDb.da.gets(Target.class, new Skip(0), new Limit(10));
			Assertions.assertEquals("t-user", page.get(0).credentials.user);
		} finally {
			database().runCommand(new Document("profile", 0));
		}
		final List<Document> finds = raw("system.profile").find(new Document("ns", namespace).append("op", "query"))
				.into(new ArrayList<>());
		Assertions.assertEquals(3, finds.size(), finds.toString());
		for (final Document find : finds) {
			final Document projection = find.get("command", Document.class).get("projection", Document.class);
			Assertions.assertNotNull(projection, find.toJson());
			Assertions.assertTrue(projection.containsKey("credentials.user"), projection.toJson());
			Assertions.assertFalse(projection.containsKey("credentials"), projection.toJson());
			Assertions.assertFalse(projection.containsKey("credentials.password"), projection.toJson());
		}
	}

	@Test
	public void testOptionSpecifyTypeKeepsTheProtectionOfTheStoredClass() throws Exception {
		// history is a List<Credentials> (password @DataNotRead) read as a List<OpenCredentials> (password plain):
		// projected by the leaves of OpenCredentials, less what Credentials protects.
		final OptionSpecifyType asOpen = new OptionSpecifyType("history", OpenCredentials.class, true);
		Assertions.assertEquals(Set.of("history.user", "history.extra"),
				Set.copyOf(projection(asOpen).stream().filter(path -> path.startsWith("history")).toList()));
		// The read returns the protected sub-fields: nothing hidden in either class, the list is read whole.
		Assertions.assertTrue(projection(new ReadAllColumn(), asOpen).contains("history"));
		// A sub-path through the protected sub-field: the element stays present and empty.
		Assertions.assertEquals(Set.of("_id", "history." + PRESENCE),
				projectionSet(new FilterValue("history.password"), asOpen));
		// Ignored by the read (a field not declared Object, without isList): ignored by the projection too.
		final List<String> ignored = projection(new OptionSpecifyType("credentials", OpenCredentials.class));
		Assertions.assertTrue(ignored.contains("credentials.user"), ignored.toString());
		Assertions.assertFalse(ignored.contains("credentials"), ignored.toString());

		final ObjectId id = new ObjectId();
		raw(COLLECTION).insertOne(new Document("_id", id).append("history",
				List.of(new Document("user", "u0").append("password", "p0-secret").append("extra", "e0"),
						new Document("user", "u1").append("password", "p1-secret"))));
		final String namespace = database().getName() + "." + COLLECTION;
		database().runCommand(new Document("profile", 2));
		final Account read;
		try {
			read = ConfigureDb.da.getById(Account.class, id, asOpen);
		} finally {
			database().runCommand(new Document("profile", 0));
		}
		final List<Document> finds = raw("system.profile").find(new Document("ns", namespace).append("op", "query"))
				.into(new ArrayList<>());
		Assertions.assertFalse(finds.isEmpty());
		for (final Document find : finds) {
			final Document sent = find.get("command", Document.class).get("projection", Document.class);
			Assertions.assertTrue(sent.containsKey("history.user"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("history"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("history.password"), sent.toJson());
		}
		// The elements are OpenCredentials (the list is filled by the class of the option).
		final List<?> history = read.history;
		final OpenCredentials first = (OpenCredentials) history.get(0);
		Assertions.assertEquals("u0", first.user);
		Assertions.assertEquals("e0", first.extra);
		Assertions.assertNull(first.password);
		Assertions.assertNull(((OpenCredentials) history.get(1)).password);

		final Account all = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn(), asOpen);
		final List<?> allHistory = all.history;
		Assertions.assertEquals("p0-secret", ((OpenCredentials) allHistory.get(0)).password);
	}

	@Test
	public void testOptionSpecifyTypeWithAClassLackingTheProtectedSubField() throws Exception {
		// history is a List<Credentials> read as a List<UserOnly>, which does not declare password: the list is still
		// projected down to the leaves, the protected sub-field of Credentials is not transferred.
		final OptionSpecifyType asUser = new OptionSpecifyType("history", UserOnly.class, true);
		Assertions.assertEquals(Set.of("history.user"),
				Set.copyOf(projection(asUser).stream().filter(path -> path.startsWith("history")).toList()));
		// The read returns the protected sub-fields: the list is read whole.
		Assertions.assertTrue(projection(new ReadAllColumn(), asUser).contains("history"));
		// The same one level down: the embedded object of the elements does not declare password.
		final OptionSpecifyType asUserOnlyHolder = new OptionSpecifyType("holders", UserOnlyHolder.class, true);
		Assertions.assertEquals(Set.of("_id", "holders.name", "holders.credentials.user"),
				Set.copyOf(ConfigureDb.da.generateSelectField(Folder.class, new QueryOptions(asUserOnlyHolder))));

		final ObjectId id = new ObjectId();
		raw(COLLECTION).insertOne(new Document("_id", id).append("history",
				List.of(new Document("user", "u0").append("password", "p0-secret"),
						new Document("user", "u1").append("password", "p1-secret"))));
		final ObjectId folderId = new ObjectId();
		raw(FOLDER).insertOne(new Document("_id", folderId).append("holders", List.of(new Document("name", "h0")
				.append("credentials", new Document("user", "u0").append("password", "p0-secret")))));
		final String namespace = database().getName() + "." + COLLECTION;
		final String folderNamespace = database().getName() + "." + FOLDER;
		database().runCommand(new Document("profile", 2));
		final Account read;
		final Folder folder;
		try {
			read = ConfigureDb.da.getById(Account.class, id, asUser);
			folder = ConfigureDb.da.getById(Folder.class, folderId, asUserOnlyHolder);
		} finally {
			database().runCommand(new Document("profile", 0));
		}
		final List<Document> finds = raw("system.profile").find(new Document("ns", namespace).append("op", "query"))
				.into(new ArrayList<>());
		Assertions.assertFalse(finds.isEmpty());
		for (final Document find : finds) {
			final Document sent = find.get("command", Document.class).get("projection", Document.class);
			Assertions.assertTrue(sent.containsKey("history.user"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("history"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("history.password"), sent.toJson());
		}
		final List<Document> folderFinds = raw("system.profile")
				.find(new Document("ns", folderNamespace).append("op", "query")).into(new ArrayList<>());
		Assertions.assertFalse(folderFinds.isEmpty());
		for (final Document find : folderFinds) {
			final Document sent = find.get("command", Document.class).get("projection", Document.class);
			Assertions.assertTrue(sent.containsKey("holders.credentials.user"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("holders"), sent.toJson());
			Assertions.assertFalse(sent.containsKey("holders.credentials"), sent.toJson());
		}
		final List<?> history = read.history;
		Assertions.assertEquals(2, history.size());
		Assertions.assertEquals("u0", ((UserOnly) history.get(0)).user);
		final List<?> holders = folder.holders;
		Assertions.assertEquals("u0", ((UserOnlyHolder) holders.get(0)).credentials.user);
	}

	@Test
	public void testOptionSpecifyTypeFiltersWhatTheProjectionIncludesWhole() throws Exception {
		// Recursive: cut at 8 levels, the part included whole holds the protected sub-field of Node.
		final OptionSpecifyType asOpen = new OptionSpecifyType("nodes", OpenNode.class, true);
		final List<String> paths = ConfigureDb.da.generateSelectField(Forest.class, new QueryOptions(asOpen));
		Assertions.assertTrue(paths.contains("nodes" + ".child".repeat(8)), paths.toString());
		Assertions.assertFalse(paths.contains("nodes.secret"), paths.toString());

		Document deepest = new Document("label", "l9").append("secret", "s9-secret");
		for (int level = 8; level >= 0; level--) {
			deepest = new Document("label", "l" + level).append("secret", "s" + level + "-secret").append("child",
					deepest);
		}
		final ObjectId id = new ObjectId();
		raw(FOREST).insertOne(new Document("_id", id).append("nodes", List.of(deepest)));
		final Forest read = ConfigureDb.da.getById(Forest.class, id, asOpen);
		final List<?> nodes = read.nodes;
		OpenNode node = (OpenNode) nodes.get(0);
		for (int level = 0; level <= 9; level++) {
			Assertions.assertEquals("l" + level, node.label);
			// Transferred below the cut, removed by the declared class before OpenNode decodes it.
			Assertions.assertNull(node.secret, "level " + level);
			node = node.child;
		}
		Assertions.assertNull(node);
	}

	@Test
	public void testCachedPathsFollowTheFieldAndTheFlags() throws Exception {
		final List<String> first = projection();
		// Same declared type under two fields, and a renamed column: each gets its own prefix.
		Assertions.assertTrue(first.containsAll(List.of("credentials.user", "backup.user")), first.toString());
		final List<String> renamed = projection(new OptionRenameColumn("backup", "spare"));
		Assertions.assertTrue(renamed.contains("spare.user"), renamed.toString());
		Assertions.assertFalse(renamed.stream().anyMatch(path -> path.startsWith("backup")), renamed.toString());
		// The flags are part of the key.
		Assertions.assertTrue(projection(new ReadAllColumn()).contains("backup.password"));
		Assertions.assertFalse(projection().contains("backup.password"));
		// Emptying the model cache empties the paths: same result once rebuilt.
		DbClassModel.clearCache();
		Assertions.assertEquals(first, projection());
	}
}
