package test.atriasoft.archidata.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.exception.ProtectedFieldEncodingException;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonExtraElements;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import test.atriasoft.archidata.ConfigureDb;

/**
 * The codecs of the MongoDB driver (automatic POJO codec, record codec) ignore {@code @DataNotRead} and
 * {@code @DataEncrypt}: the client of {@code DbIoMongo} refuses to encode an application object holding such a field,
 * at any depth among the properties the driver writes, through every raw API. Decoding is unchanged.
 */
public class TestProtectedFieldEncoding {
	private static final String COLLECTION = "protectedFieldEncoding";

	/** A POJO with a {@code @DataNotRead} field. */
	public static class Secretive {
		public String label;
		@DataNotRead
		public String secret;
	}

	/** A POJO with a {@code @DataEncrypt} field. */
	public static class Encrypted {
		public String label;
		@DataEncrypt
		public String card;
	}

	/** Holds the protected field one level down. */
	public static class Holder {
		public String name;
		public Secretive inner;
	}

	/** Holds the protected field in the elements of a list. */
	public static class ListHolder {
		public String name;
		public List<Secretive> items;
	}

	/** A record with a {@code @DataEncrypt} component. */
	public record SecretRecord(
			String label,
			@DataEncrypt String card) {}

	/** A record without protected component. */
	public record PlainRecord(
			String label,
			int count) {}

	/** A POJO without protected field. */
	public static class Plain {
		public String label;
		public int count;
	}

	/** Declared type of a field, without protected field. */
	public static class Base {
		public String label;
	}

	/** Runtime subclass adding a protected field. */
	public static class SecretChild extends Base {
		@DataNotRead
		public String secret;
	}

	/** Holds a {@link Base}, which may be a {@link SecretChild} at runtime. */
	public static class BaseHolder {
		public Base base;
	}

	/** A generic class with a protected field: as a property, the POJO codec specializes it. */
	public static class GenericBox<T> {
		public T value;
		@DataNotRead
		public String secret;
	}

	/** Holds a specialized generic class. */
	public static class BoxHolder {
		public String name;
		public GenericBox<String> box;
	}

	/** A generic holder: its field is typed by a type variable. */
	public static class Wrapper<T> {
		public T value;
	}

	/** Binds the type variable to a class with a protected field. */
	public static class SecretWrapper extends Wrapper<Secretive> {}

	/** An entity of the application on an archidata base model ({@code deleted}, {@code createdAt}...). */
	public static class SoftEntity extends OIDGenericDataSoftDelete {
		public String name;
	}

	/** Protected fields the driver never writes: transient, {@code @BsonIgnore}, private without accessor. */
	public static class Unwritten {
		public String label;
		@DataNotRead
		public transient String cache;
		@DataNotRead
		@BsonIgnore
		public String ignored;
		@DataNotRead
		private String hidden;

		/** Not an accessor: the private field stays out of the properties of the driver. */
		public void remember(final String value) {
			this.hidden = value;
		}
	}

	/** Holds objects whose protected fields the driver never writes. */
	public static class UnwrittenHolder {
		public String name;
		public List<Unwritten> items;
	}

	/** A property with a getter and no field, of a class holding a protected field. */
	public static class GetterOnly {
		public String name;

		public Secretive getComputed() {
			if (this.name == null) {
				return null;
			}
			final Secretive out = new Secretive();
			out.label = this.name;
			out.secret = this.name + "-secret";
			return out;
		}
	}

	/** Holds objects whose getter leads to a protected field. */
	public static class GetterOnlyList {
		public String name;
		public List<GetterOnly> items;
	}

	/** Keeps the keys it does not declare ({@code @BsonExtraElements}), and holds a protected field. */
	public static class WithExtras {
		public String label;
		@DataNotRead
		public String secret;
		@BsonExtraElements
		public Document extras;
	}

	private static MongoDatabase database;

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
		database = ConfigureDb.da.getInterface().getDatabase();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@BeforeEach
	public void emptyTheCollection() {
		database.getCollection(COLLECTION).deleteMany(new Document());
	}

	private static Secretive secretive() {
		final Secretive out = new Secretive();
		out.label = "label";
		out.secret = "s3cret";
		return out;
	}

	private static ObjectId insertRaw(final Document document) {
		final ObjectId id = new ObjectId();
		database.getCollection(COLLECTION).insertOne(new Document("_id", id).append("label", "before"));
		database.getCollection(COLLECTION).updateOne(Filters.eq("_id", id), new Document("$set", document));
		return id;
	}

	private static void assertRefusal(
			final String message,
			final Class<?> refused,
			final String path,
			final String annotation) {
		Assertions.assertTrue(message.contains("The MongoDB driver refuses to encode " + refused.getName()), message);
		Assertions.assertTrue(message.contains("its field '" + path + "'"), message);
		Assertions.assertTrue(message.contains("is marked @" + annotation), message);
		Assertions.assertTrue(message.contains("Write the object through DataAccess"), message);
	}

	private static String refusalOf(final Executable call) {
		return Assertions.assertThrows(ProtectedFieldEncodingException.class, call).getMessage();
	}

	@Test
	public void testUpdateBsonDocumentsRefusesAnObjectHoldingAProtectedField() throws Exception {
		final ObjectId id = insertRaw(new Document("count", 1));
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.updateBsonDocuments(COLLECTION,
						new Document("$set", new Document("x", secretive())), new Condition(Filters.eq("_id", id))));
		assertRefusal(ex.getMessage(), Secretive.class, "secret", "DataNotRead");
		Assertions.assertTrue(ex.getMessage().contains("the value would be written like any other field"),
				ex.getMessage());
		final Document stored = database.getCollection(COLLECTION).find(Filters.eq("_id", id)).first();
		Assertions.assertEquals("before", stored.getString("label"));
		Assertions.assertFalse(stored.containsKey("x"));
	}

	@Test
	public void testInsertBsonDocumentRefusesAnEncryptedField() throws Exception {
		final Encrypted encrypted = new Encrypted();
		encrypted.label = "label";
		encrypted.card = "4970 0000 0000 0000";
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.insertBsonDocument(COLLECTION, new Document("payload", encrypted)));
		assertRefusal(ex.getMessage(), Encrypted.class, "card", "DataEncrypt");
		Assertions.assertTrue(ex.getMessage().contains("the value would be written in clear"), ex.getMessage());
		Assertions.assertEquals(0, database.getCollection(COLLECTION).countDocuments());
	}

	@Test
	public void testTheRawDriverRefusesAtAnyDepth() {
		final ObjectId id = insertRaw(new Document("count", 1));
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);

		// One level down, the value null included: the refusal follows the declared type.
		final Holder empty = new Holder();
		empty.name = "empty";
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", empty))),
				Holder.class, "inner.secret", "DataNotRead");
		final Holder full = new Holder();
		full.name = "full";
		full.inner = secretive();
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", full))),
				Holder.class, "inner.secret", "DataNotRead");

		// The elements of a list, the list empty.
		final ListHolder list = new ListHolder();
		list.name = "list";
		list.items = new ArrayList<>();
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", list))),
				ListHolder.class, "items.secret", "DataNotRead");

		// A record, inside a Document.
		assertRefusal(
				refusalOf(() -> collection.updateOne(Filters.eq("_id", id),
						new Document("$set", new Document("x", new SecretRecord("label", "4970"))))),
				SecretRecord.class, "card", "DataEncrypt");

		// A runtime subclass of a declared type without protected field.
		final SecretChild child = new SecretChild();
		child.label = "child";
		child.secret = "s3cret";
		final BaseHolder baseHolder = new BaseHolder();
		baseHolder.base = child;
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", baseHolder))),
				SecretChild.class, "secret", "DataNotRead");

		// A typed collection, and a value of a filter.
		final MongoCollection<Secretive> typed = database.getCollection(COLLECTION, Secretive.class);
		assertRefusal(refusalOf(() -> typed.insertOne(secretive())), Secretive.class, "secret", "DataNotRead");
		assertRefusal(refusalOf(() -> collection.find(Filters.eq("x", full)).first()), Holder.class, "inner.secret",
				"DataNotRead");

		// An entity on an archidata base model holds @DataNotRead fields (deleted, createdAt, updatedAt).
		final SoftEntity entity = new SoftEntity();
		entity.name = "entity";
		final String entityRefusal = refusalOf(
				() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", entity)));
		Assertions.assertTrue(
				entityRefusal.contains("The MongoDB driver refuses to encode " + SoftEntity.class.getName()),
				entityRefusal);
		Assertions.assertTrue(entityRefusal.contains("is marked @DataNotRead"), entityRefusal);

		// Nothing was written, and the connection still works.
		final Document stored = collection.find(Filters.eq("_id", id)).first();
		Assertions.assertEquals("before", stored.getString("label"));
		Assertions.assertFalse(stored.containsKey("x"));
		Assertions.assertEquals(0,
				database.getCollection(COLLECTION, Secretive.class).countDocuments(Filters.eq("label", "nothing")));
	}

	@Test
	public void testAFieldTypedByATypeVariableIsCheckedByTheClassOfItsValue() {
		final ObjectId id = insertRaw(new Document("count", 1));
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		final SecretWrapper wrapper = new SecretWrapper();
		wrapper.value = secretive();
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", wrapper))),
				Secretive.class, "secret", "DataNotRead");
		// Without a value, nothing protected is written: the declared type does not reach the protected field.
		final SecretWrapper emptyWrapper = new SecretWrapper();
		collection.updateOne(Filters.eq("_id", id), Updates.set("wrapper", emptyWrapper));
		final Document stored = collection.find(Filters.eq("_id", id)).first();
		Assertions.assertEquals(new Document(), stored.get("wrapper", Document.class));
		Assertions.assertFalse(stored.containsKey("x"));
	}

	@Test
	public void testObjectsWithoutProtectedFieldStillEncode() {
		final ObjectId id = insertRaw(new Document("count", 1));
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		final Plain plain = new Plain();
		plain.label = "plain";
		plain.count = 3;
		collection.updateOne(Filters.eq("_id", id),
				Updates.combine(Updates.set("pojo", plain), Updates.set("record", new PlainRecord("record", 4))));
		final Document stored = collection.find(Filters.eq("_id", id)).first();
		Assertions.assertEquals(new Document("count", 3).append("label", "plain"),
				sorted(stored.get("pojo", Document.class)));
		Assertions.assertEquals(new Document("count", 4).append("label", "record"),
				sorted(stored.get("record", Document.class)));
		final Base base = new Base();
		base.label = "base";
		final BaseHolder holder = new BaseHolder();
		holder.base = base;
		collection.updateOne(Filters.eq("_id", id), Updates.set("holder", holder));
		Assertions.assertEquals("base", collection.find(Filters.eq("_id", id)).first().get("holder", Document.class)
				.get("base", Document.class).getString("label"));
	}

	private static Document sorted(final Document document) {
		final Document out = new Document();
		document.keySet().stream().sorted().forEach(key -> out.append(key, document.get(key)));
		return out;
	}

	@Test
	public void testDecodingIsUnchanged() {
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		final Document secretDocument = new Document("label", "label").append("secret", "s3cret");
		collection.insertOne(new Document("_id", "secretive").append("label", "label").append("secret", "s3cret"));
		collection.insertOne(new Document("_id", "holder").append("name", "holder").append("inner", secretDocument));
		collection
				.insertOne(new Document("_id", "list").append("name", "list").append("items", List.of(secretDocument)));
		collection.insertOne(new Document("_id", "record").append("label", "label").append("card", "4970"));
		collection.insertOne(new Document("_id", "box").append("name", "box").append("box",
				new Document("value", "v").append("secret", "s3cret")));

		final Secretive secretive = database.getCollection(COLLECTION, Secretive.class)
				.find(Filters.eq("_id", "secretive")).projection(Projections.excludeId()).first();
		Assertions.assertEquals("s3cret", secretive.secret);
		final Holder holder = database.getCollection(COLLECTION, Holder.class).find(Filters.eq("_id", "holder"))
				.projection(Projections.excludeId()).first();
		Assertions.assertEquals("s3cret", holder.inner.secret);
		final ListHolder list = database.getCollection(COLLECTION, ListHolder.class).find(Filters.eq("_id", "list"))
				.projection(Projections.excludeId()).first();
		Assertions.assertEquals("s3cret", list.items.get(0).secret);
		final SecretRecord record = database.getCollection(COLLECTION, SecretRecord.class)
				.find(Filters.eq("_id", "record")).projection(Projections.excludeId()).first();
		Assertions.assertEquals(new SecretRecord("label", "4970"), record);
		// The generic class is still specialized as a property (String value).
		final BoxHolder box = database.getCollection(COLLECTION, BoxHolder.class).find(Filters.eq("_id", "box"))
				.projection(Projections.excludeId()).first();
		Assertions.assertEquals("v", box.box.value);
		Assertions.assertEquals("s3cret", box.box.secret);
	}

	@Test
	public void testFieldsTheDriverDoesNotWriteAreNotChecked() {
		final ObjectId id = insertRaw(new Document("count", 1));
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		final Unwritten unwritten = new Unwritten();
		unwritten.label = "unwritten";
		unwritten.cache = "cache-secret";
		unwritten.ignored = "ignored-secret";
		unwritten.remember("hidden-secret");
		final UnwrittenHolder holder = new UnwrittenHolder();
		holder.name = "holder";
		holder.items = new ArrayList<>(List.of(unwritten));
		collection.updateOne(Filters.eq("_id", id),
				Updates.combine(Updates.set("one", unwritten), Updates.set("holder", holder)));
		final Document stored = collection.find(Filters.eq("_id", id)).first();
		Assertions.assertEquals(new Document("label", "unwritten"), stored.get("one", Document.class));
		Assertions.assertEquals(List.of(new Document("label", "unwritten")),
				stored.get("holder", Document.class).getList("items", Document.class));
		Assertions.assertFalse(stored.toJson().contains("-secret"), stored.toJson());
	}

	@Test
	public void testAGetterWithoutFieldIsChecked() {
		final ObjectId id = insertRaw(new Document("count", 1));
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		final GetterOnly getter = new GetterOnly();
		getter.name = "getter";
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", getter))),
				GetterOnly.class, "computed.secret", "DataNotRead");
		// Reached through the elements of a list, the list empty: refused by the declared types.
		final GetterOnlyList list = new GetterOnlyList();
		list.name = "list";
		list.items = new ArrayList<>();
		assertRefusal(refusalOf(() -> collection.updateOne(Filters.eq("_id", id), Updates.set("x", list))),
				GetterOnlyList.class, "items.computed.secret", "DataNotRead");
		Assertions.assertFalse(collection.find(Filters.eq("_id", id)).first().containsKey("x"));
	}

	@Test
	public void testInsertManyOnATypedCollectionIsRefused() {
		final MongoCollection<Secretive> typed = database.getCollection(COLLECTION, Secretive.class);
		assertRefusal(refusalOf(() -> typed.insertMany(List.of(secretive(), secretive()))), Secretive.class, "secret",
				"DataNotRead");
		Assertions.assertEquals(0, database.getCollection(COLLECTION).countDocuments());
	}

	@Test
	public void testExtraElementsAreStillDecoded() {
		final MongoCollection<Document> collection = database.getCollection(COLLECTION);
		collection.insertOne(new Document("_id", "extras").append("label", "label").append("secret", "s3cret")
				.append("other", 1).append("more", "m"));
		final WithExtras read = database.getCollection(COLLECTION, WithExtras.class).find(Filters.eq("_id", "extras"))
				.projection(Projections.excludeId()).first();
		Assertions.assertEquals("label", read.label);
		Assertions.assertEquals("s3cret", read.secret);
		Assertions.assertEquals(new Document("other", 1).append("more", "m"), read.extras);
		// Encoding the same object is refused.
		assertRefusal(refusalOf(() -> database.getCollection(COLLECTION, WithExtras.class).insertOne(read)),
				WithExtras.class, "secret", "DataNotRead");
	}
}
