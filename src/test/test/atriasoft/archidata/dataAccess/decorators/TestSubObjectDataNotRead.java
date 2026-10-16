package test.atriasoft.archidata.dataAccess.decorators;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
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

	/** Entity embedding the sub-objects. */
	@Collection("subObjectDataNotRead")
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Credentials credentials;
		public ApiKey key;
		public List<Credentials> history;
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
}
