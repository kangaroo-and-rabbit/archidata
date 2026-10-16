package test.atriasoft.archidata.dataAccess.decorators;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-02: {@code @DataNotRead} also hides the fields of embedded sub-objects, unless ReadAllColumn. */
class TestSubObjectDataNotRead {

	/** Embedded POJO with a hidden field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String passwordHash;
	}

	/** Embedded record with a hidden component. */
	public record ApiKey(
			String label,
			@DataNotRead String secret) {}

	/** Embedded POJO without hidden field of its own, embedding one that has some. */
	public static class Profile {
		public String city;
		public Credentials backup;
	}

	/** Entity embedding the sub-objects. */
	@Collection("subObjectDataNotRead")
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public ApiKey key;
		public List<Credentials> history;
		public Profile profile;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	private static Credentials credentials(final String user, final String hash) {
		final Credentials out = new Credentials();
		out.user = user;
		out.passwordHash = hash;
		return out;
	}

	@Test
	void testNestedNotReadFieldsHiddenByDefault() throws Exception {
		final Account account = new Account();
		account.name = "acc";
		account.credentials = credentials("bob", "hash-1");
		account.key = new ApiKey("main", "key-secret");
		account.history = List.of(credentials("old", "hash-0"));
		final ObjectId id = ConfigureDb.da.insert(account)._id;

		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertEquals("acc", read.name);
		Assertions.assertEquals("bob", read.credentials.user);
		Assertions.assertNull(read.credentials.passwordHash);
		Assertions.assertEquals("main", read.key.label());
		Assertions.assertNull(read.key.secret());
		Assertions.assertEquals("old", read.history.get(0).user);
		Assertions.assertNull(read.history.get(0).passwordHash);
		Assertions.assertNull(ConfigureDb.da.gets(Account.class).get(0).credentials.passwordHash);

		final Account readAll = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("hash-1", readAll.credentials.passwordHash);
		Assertions.assertEquals("key-secret", readAll.key.secret());
		Assertions.assertEquals("hash-0", readAll.history.get(0).passwordHash);

		// The flag does not leak to the next read of the same thread.
		Assertions.assertNull(ConfigureDb.da.getById(Account.class, id).credentials.passwordHash);
	}

	private static Document raw(final ObjectId id) throws Exception {
		return ConfigureDb.da.getInterface().getDatabase().getCollection("subObjectDataNotRead")
				.find(new Document("_id", id)).first();
	}

	private static ObjectId insertFull() throws Exception {
		final Account account = new Account();
		account.name = "acc";
		account.credentials = credentials("bob", "hash-1");
		account.key = new ApiKey("main", "key-secret");
		account.history = List.of(credentials("old", "hash-0"));
		account.profile = new Profile();
		account.profile.city = "Paris";
		account.profile.backup = credentials("backup", "hash-b");
		return ConfigureDb.da.insert(account)._id;
	}

	@Test
	void testReadThenFullUpdateKeepsNestedSecrets() throws Exception {
		final ObjectId id = insertFull();

		// get -> change another field -> updateById without FilterValue
		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertNull(read.credentials.passwordHash);
		read.name = "renamed";
		ConfigureDb.da.updateById(read, id);

		final Account after = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("renamed", after.name);
		Assertions.assertEquals("bob", after.credentials.user);
		Assertions.assertEquals("hash-1", after.credentials.passwordHash);
		Assertions.assertEquals("main", after.key.label());
		Assertions.assertEquals("key-secret", after.key.secret());
		Assertions.assertEquals("hash-0", after.history.get(0).passwordHash);
		Assertions.assertEquals("Paris", after.profile.city);
		Assertions.assertEquals("hash-b", after.profile.backup.passwordHash);
		// In the stored document itself.
		Assertions.assertEquals("hash-1", raw(id).get("credentials", Document.class).getString("passwordHash"));
	}

	@Test
	void testFullUpdateWritesTheVisibleNestedFields() throws Exception {
		final ObjectId id = insertFull();
		final Account read = ConfigureDb.da.getById(Account.class, id);
		read.credentials.user = "alice";
		read.key = new ApiKey("renamed", null);
		read.profile.city = null;
		read.profile.backup.user = "backup2";
		// A list of objects with hidden fields is not rewritten by a full update.
		read.history = List.of(credentials("new", null));
		ConfigureDb.da.updateById(read, id);

		final Account after = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("alice", after.credentials.user);
		Assertions.assertEquals("hash-1", after.credentials.passwordHash);
		Assertions.assertEquals("renamed", after.key.label());
		Assertions.assertEquals("key-secret", after.key.secret());
		Assertions.assertNull(after.profile.city);
		Assertions.assertFalse(raw(id).get("profile", Document.class).containsKey("city"));
		Assertions.assertEquals("backup2", after.profile.backup.user);
		Assertions.assertEquals("hash-b", after.profile.backup.passwordHash);
		Assertions.assertEquals(1, after.history.size());
		Assertions.assertEquals("old", after.history.get(0).user);
		Assertions.assertEquals("hash-0", after.history.get(0).passwordHash);

		// Named in a FilterValue, the field is rewritten as a whole (hidden fields included).
		ConfigureDb.da.updateById(read, id, new FilterValue("history"));
		final Account named = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("new", named.history.get(0).user);
		Assertions.assertNull(named.history.get(0).passwordHash);
		Assertions.assertEquals("hash-1", named.credentials.passwordHash);
	}

	@Test
	void testFullUpdateNullEmbeddedObjects() throws Exception {
		// Stored without the embedded objects: the dot notation creates them.
		final Account bare = new Account();
		bare.name = "bare";
		final ObjectId id = ConfigureDb.da.insert(bare)._id;
		final Account read = ConfigureDb.da.getById(Account.class, id);
		Assertions.assertNull(read.credentials);
		read.credentials = credentials("carol", "ignored");
		read.profile = new Profile();
		read.profile.city = "Lyon";
		ConfigureDb.da.updateById(read, id);
		final Account after = ConfigureDb.da.getById(Account.class, id, new ReadAllColumn());
		Assertions.assertEquals("carol", after.credentials.user);
		// The hidden field is not written by a full update, even when set.
		Assertions.assertNull(after.credentials.passwordHash);
		Assertions.assertEquals("Lyon", after.profile.city);
		Assertions.assertNull(after.profile.backup);

		// Set to null, the embedded object is removed (an explicit null is a deletion, as for any field).
		after.credentials = null;
		ConfigureDb.da.updateById(after, id);
		Assertions.assertFalse(raw(id).containsKey("credentials"));
		Assertions.assertNull(ConfigureDb.da.getById(Account.class, id, new ReadAllColumn()).credentials);
	}
}
