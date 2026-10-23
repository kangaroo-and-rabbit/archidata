package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.DirectPrimaryKey;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/**
 * A primary key declared {@code Object}, whose type is given on each call by an {@code OptionSpecifyType}, is
 * exempt from the refusal of the polymorphic types; its value must be a key value. Any other field declared
 * {@code Object} stays refused, with or without the option.
 */
public class TestUntypedPrimaryKey {
	private static final String COLLECTION = "untypedPrimaryKey";

	/** Embedded object with a hidden field. */
	public static class Credentials {
		public String user;
		@DataNotRead
		public String password;
	}

	/** Entity whose key type is chosen by the caller. */
	@Collection(COLLECTION)
	public static class Keyed {
		@Id
		public Object _id = null;
		public String name;
		public Credentials credentials;
	}

	/** A field declared Object that is not the key: refused. */
	@Collection("untypedPrimaryKeyRefused")
	public static class WithObjectField {
		@Id
		public ObjectId _id = null;
		public Object value;
	}

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void clear() throws IOException {
		ConfigureDb.clear();
	}

	private static Keyed keyed(final Object id, final String name) {
		final Keyed out = new Keyed();
		out._id = id;
		out.name = name;
		out.credentials = new Credentials();
		out.credentials.user = name + "-user";
		out.credentials.password = name + "-password";
		return out;
	}

	private static Document stored(final Object id) {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION).find(new Document("_id", id))
				.first();
	}

	@Test
	public void testTheModelIsAccepted() throws Exception {
		Assertions.assertNotNull(DbClassModel.of(Keyed.class));
		Assertions.assertEquals("_id", Filters.checkFieldName(Keyed.class, "_id"));
		// The key is projected whole, with or without its OptionSpecifyType; the embedded object to its leaves.
		Assertions.assertEquals(List.of("_id", "name", "credentials.user"),
				ConfigureDb.da.generateSelectField(Keyed.class, new QueryOptions()));
		Assertions.assertEquals(List.of("_id", "name", "credentials.user"), ConfigureDb.da
				.generateSelectField(Keyed.class, new QueryOptions(new OptionSpecifyType("_id", ObjectId.class))));
	}

	@Test
	public void testObjectIdKey() throws Exception {
		final ObjectId id = new ObjectId();
		final OptionSpecifyType type = new OptionSpecifyType("_id", ObjectId.class);
		final Keyed inserted = ConfigureDb.da.insert(keyed(id, "oid"), new DirectPrimaryKey(), type);
		Assertions.assertEquals(id, inserted._id);
		Assertions.assertEquals("oid-user", inserted.credentials.user);
		Assertions.assertNull(inserted.credentials.password);
		Assertions.assertEquals("oid-password", ((Document) stored(id).get("credentials")).get("password"));

		final Keyed read = ConfigureDb.da.getById(Keyed.class, id, type);
		Assertions.assertEquals(id, read._id);
		Assertions.assertEquals("oid", read.name);

		final Keyed update = new Keyed();
		update.name = "oid-updated";
		Assertions.assertEquals(1, ConfigureDb.da.updateById(update, id, new FilterValue(List.of("name")), type));
		Assertions.assertEquals("oid-updated", stored(id).get("name"));
	}

	@Test
	public void testStringKey() throws Exception {
		final OptionSpecifyType type = new OptionSpecifyType("_id", String.class);
		final Keyed inserted = ConfigureDb.da.insert(keyed("key-1", "text"), new DirectPrimaryKey(), type);
		Assertions.assertEquals("key-1", inserted._id);
		final Keyed read = ConfigureDb.da.getById(Keyed.class, "key-1", type);
		Assertions.assertEquals("text", read.name);
		Assertions.assertEquals("key-1", stored("key-1").get("_id"));
	}

	@Test
	public void testAKeyOfAnotherClassIsRefused() {
		final String expected = "Field " + Keyed.class.getName() + "._id is a primary key declared java.lang.Object"
				+ " but holds a ";
		final String takes = ": such a key only takes an ObjectId, a UUID, a String, a Long, an Integer or a Short";
		Exception ex = Assertions.assertThrows(Exception.class,
				() -> ConfigureDb.da.insert(keyed(new Credentials(), "pojo"), new DirectPrimaryKey()));
		Assertions.assertTrue(ex.getMessage().endsWith(expected + Credentials.class.getName() + takes),
				ex.getMessage());
		ex = Assertions.assertThrows(Exception.class,
				() -> ConfigureDb.da.insert(keyed(new Date(0), "date"), new DirectPrimaryKey()));
		Assertions.assertTrue(ex.getMessage().endsWith(expected + "java.util.Date" + takes), ex.getMessage());
		Assertions.assertNull(ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("name", "pojo")).first());
	}

	@Test
	public void testAnotherFieldDeclaredObjectStaysRefused() {
		final Exception ex = Assertions.assertThrows(Exception.class,
				() -> ConfigureDb.da.gets(WithObjectField.class, new OptionSpecifyType("value", String.class)));
		Throwable cause = ex;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		Assertions.assertTrue(
				cause.getMessage()
						.startsWith("Unsupported field type on " + WithObjectField.class.getName()
								+ ": field WithObjectField.value (path value) is declared" + " java.lang.Object"),
				cause.getMessage());
	}
}
