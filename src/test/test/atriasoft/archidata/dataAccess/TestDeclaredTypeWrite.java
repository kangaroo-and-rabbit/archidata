package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.CodeWithScope;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.mongodb.DBRef;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import test.atriasoft.archidata.ConfigureDb;

/**
 * An embedded value is written with the fields of its declared type, at any depth: a subclass loses the fields
 * it adds (one warning per field and class), on insert, full update and partial update.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDeclaredTypeWrite {
	private static final String COLLECTION = "declaredTypeWrite";

	/** Concrete base class of an embedded value. */
	public static class Base {
		public String label;
	}

	/** Subclass adding a visible and a hidden field. */
	public static class Derived extends Base {
		public String extra;
		@DataNotRead
		public String secret;
	}

	/** Base class with a protected field: a full update writes it field by field. */
	public static class ProtectedBase {
		public String label;
		@DataNotRead
		public String hidden;
	}

	/** Subclass of {@link ProtectedBase}. */
	public static class ProtectedDerived extends ProtectedBase {
		public String extra;
	}

	/** A class of the application extending a JDK class archidata does not model. */
	public static class Money extends BigDecimal {
		private static final long serialVersionUID = 1L;
		@DataNotRead
		public String secret = "money-secret";

		public Money() {
			super("3");
		}
	}

	/** Embedded class holding a base-class field. */
	public static class Holder {
		public String name;
		public Base inner;
	}

	/** Entity whose embedded values are declared with base classes. */
	@Collection(COLLECTION)
	public static class Account {
		@Id
		public ObjectId _id = null;
		public String name;
		public Base base;
		public List<Base> bases;
		public Map<String, Base> byKey;
		public Holder holder;
		public ProtectedBase pbase;
		public Document free;
		public List<Document> frees;
		public BigDecimal price;
	}

	/** Enum of the application, accepted in a free-form document (stored by its name). */
	public enum Color {
		RED
	}

	private ListAppender<ILoggingEvent> appender;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@BeforeEach
	public void captureWarnings() {
		this.appender = new ListAppender<>();
		this.appender.start();
		((Logger) LoggerFactory.getLogger(MongoCodecFactory.class)).addAppender(this.appender);
	}

	@AfterEach
	public void releaseWarnings() {
		((Logger) LoggerFactory.getLogger(MongoCodecFactory.class)).detachAppender(this.appender);
	}

	private List<String> warnings() {
		return this.appender.list.stream().filter(event -> event.getLevel() == Level.WARN)
				.map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
	}

	private static Derived derived(final String label) {
		final Derived out = new Derived();
		out.label = label;
		out.extra = label + "-extra";
		out.secret = label + "-secret";
		return out;
	}

	private static Document stored(final ObjectId id) {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION).find(new Document("_id", id))
				.first();
	}

	// First: the warnings are logged once per place for the whole process.
	@Order(1)
	@Test
	public void testInsertStoresTheDeclaredFieldsOnly() throws Exception {
		final Account account = new Account();
		account.name = "insert";
		account.base = derived("b");
		account.bases = new ArrayList<>(List.of(derived("l0"), derived("l1")));
		account.byKey = Map.of("k", derived("m"));
		account.holder = new Holder();
		account.holder.name = "holder";
		account.holder.inner = derived("h");
		final ProtectedDerived pderived = new ProtectedDerived();
		pderived.label = "p";
		pderived.hidden = "p-hidden";
		pderived.extra = "p-extra";
		account.pbase = pderived;
		final Account inserted = ConfigureDb.da.insert(account);
		// Twice: still one warning per place.
		final Account second = new Account();
		second.base = derived("b2");
		ConfigureDb.da.insert(second);

		final Document doc = stored(inserted._id);
		Assertions.assertEquals(new Document("label", "b"), doc.get("base"));
		Assertions.assertEquals(List.of(new Document("label", "l0"), new Document("label", "l1")), doc.get("bases"));
		Assertions.assertEquals(new Document("k", new Document("label", "m")), doc.get("byKey"));
		Assertions.assertEquals(new Document("name", "holder").append("inner", new Document("label", "h")),
				doc.get("holder"));
		Assertions.assertEquals(new Document("label", "p").append("hidden", "p-hidden"), doc.get("pbase"));

		// Read back as the declared type.
		final Account read = ConfigureDb.da.getById(Account.class, inserted._id, new ReadAllColumn());
		Assertions.assertEquals(Base.class, read.base.getClass());
		Assertions.assertEquals("b", read.base.label);
		Assertions.assertEquals(Base.class, read.bases.get(1).getClass());
		Assertions.assertEquals("h", read.holder.inner.label);

		final List<String> warnings = warnings();
		final String prefix = Account.class.getName();
		final String derivedName = Derived.class.getName();
		final String baseName = Base.class.getName();
		Assertions.assertEquals(1, warnings.stream().filter(w -> w.startsWith("Field " + prefix + ".base ")).count(),
				warnings.toString());
		Assertions.assertTrue(warnings
				.contains("Field " + prefix + ".base is declared " + baseName + " but holds a " + derivedName
						+ ": only the fields of Base are stored, the fields Derived adds are dropped ([extra,"
						+ " secret])")
				|| warnings.contains("Field " + prefix + ".base is declared " + baseName + " but holds a " + derivedName
						+ ": only the fields of Base are stored, the fields Derived adds"
						+ " are dropped ([secret, extra])"),
				warnings.toString());
		Assertions.assertEquals(1, warnings.stream()
				.filter(w -> w.startsWith("An element of field " + prefix + ".bases is declared " + baseName)).count(),
				warnings.toString());
		Assertions.assertEquals(1, warnings.stream()
				.filter(w -> w.startsWith("A value of field " + prefix + ".byKey is declared " + baseName)).count(),
				warnings.toString());
		Assertions.assertEquals(1,
				warnings.stream()
						.filter(w -> w.startsWith("Field " + Holder.class.getName() + ".inner is declared " + baseName))
						.count(),
				warnings.toString());
		Assertions.assertEquals(1, warnings.stream().filter(w -> w.startsWith("Field " + prefix + ".pbase ")).count(),
				warnings.toString());
		// Names only, never a value.
		for (final String warning : warnings) {
			Assertions.assertFalse(warning.contains("-secret") || warning.contains("-extra"), warning);
		}
	}

	@Order(2)
	@Test
	public void testUpdatesStoreTheDeclaredFieldsOnly() throws Exception {
		final Account account = new Account();
		account.name = "update";
		final ProtectedBase pbase = new ProtectedBase();
		pbase.label = "p0";
		pbase.hidden = "kept-hidden";
		account.pbase = pbase;
		final Account inserted = ConfigureDb.da.insert(account);

		// Full update: the embedded object with a protected field is written field by field (dot notation).
		final Account full = new Account();
		full.name = "update";
		full.base = derived("full");
		final ProtectedDerived pderived = new ProtectedDerived();
		pderived.label = "p1";
		pderived.extra = "p1-extra";
		full.pbase = pderived;
		ConfigureDb.da.updateById(full, inserted._id);
		Document doc = stored(inserted._id);
		Assertions.assertEquals(new Document("label", "full"), doc.get("base"));
		Assertions.assertEquals(new Document("label", "p1").append("hidden", "kept-hidden"), doc.get("pbase"));

		// Partial update.
		final Account partial = new Account();
		partial.bases = List.of(derived("part"));
		partial.base = derived("partial");
		ConfigureDb.da.updateById(partial, inserted._id, new FilterValue(List.of("bases", "base")));
		doc = stored(inserted._id);
		Assertions.assertEquals(new Document("label", "partial"), doc.get("base"));
		Assertions.assertEquals(List.of(new Document("label", "part")), doc.get("bases"));
	}

	@Order(3)
	@Test
	public void testDocumentRoundTrip() throws Exception {
		final Document free = new Document("text", "v").append("number", 42).append("nothing", null)
				.append("nested", new Document("deep", new Document("leaf", true)))
				.append("list", Arrays.asList(1, null, new Document("x", "y"), List.of("a", "b")));
		final Account account = new Account();
		account.name = "document";
		account.free = free;
		final Account inserted = ConfigureDb.da.insert(account);
		Assertions.assertEquals(free, stored(inserted._id).get("free"));
		final Account read = ConfigureDb.da.getById(Account.class, inserted._id);
		Assertions.assertEquals(free, read.free);
	}

	@Order(4)
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testValueOfAnotherClassIsRefused() {
		final Account account = new Account();
		account.name = "polluted";
		final List raw = new ArrayList<>();
		raw.add("not a Base");
		account.bases = raw;
		final Exception ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(account));
		Assertions.assertTrue(
				ex.getMessage()
						.endsWith("An element of field " + Account.class.getName() + ".bases is declared "
								+ Base.class.getName()
								+ " but holds a java.lang.String, which is not one: it cannot be stored"),
				ex.getMessage());
	}

	@Order(5)
	@Test
	public void testApplicationClassUnderAnUnmodeledTypeIsRefused() throws Exception {
		final Account account = new Account();
		account.name = "opaque";
		account.price = new Money();
		final Exception ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(account));
		Assertions.assertTrue(
				ex.getMessage().endsWith("Field " + Account.class.getName()
						+ ".price is declared java.math.BigDecimal, a type archidata does not model, but holds a "
						+ Money.class.getName()
						+ ", a class of the application: it cannot be stored there; declare the field with that class"),
				ex.getMessage());
		Assertions.assertNull(ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("name", "opaque")).first());
	}

	@Order(6)
	@Test
	public void testApplicationClassInAFreeFormDocumentIsRefused() throws Exception {
		final Derived pojo = derived("doc");
		final String refused = ", a class of the application: the driver would store all its fields (@DataNotRead and"
				+ " @DataEncrypt ones included, in clear); put only values, Documents, Lists and Maps in a Document, or"
				+ " declare a field with that class";
		final Account direct = new Account();
		direct.name = "free-pojo";
		direct.free = new Document("label", "ok").append("pojo", pojo);
		Exception ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(direct));
		Assertions.assertTrue(ex.getMessage()
				.endsWith("Field " + Account.class.getName()
						+ ".free is a free-form org.bson.Document holding, under 'pojo', a " + Derived.class.getName()
						+ refused),
				ex.getMessage());
		// At any depth, through lists and maps.
		final Account nested = new Account();
		nested.name = "free-pojo";
		nested.free = new Document("list", List.of(1, new Document("deep", Map.of("k", pojo))));
		ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(nested));
		Assertions.assertTrue(ex.getMessage().contains("holding, under 'list.1.deep.k', a " + Derived.class.getName()),
				ex.getMessage());
		// In a list of documents.
		final Account inList = new Account();
		inList.name = "free-pojo";
		inList.frees = List.of(new Document("x", 1), new Document("pojo", pojo));
		ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(inList));
		Assertions.assertTrue(
				ex.getMessage()
						.contains("An element of field " + Account.class.getName()
								+ ".frees is a free-form org.bson.Document holding, under 'pojo', a "),
				ex.getMessage());
		Assertions.assertNull(ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("name", "free-pojo")).first());

		// An update is checked the same way.
		final Account account = new Account();
		account.name = "free-update";
		final Account inserted = ConfigureDb.da.insert(account);
		final Account update = new Account();
		update.free = new Document("pojo", pojo);
		ex = Assertions.assertThrows(Exception.class,
				() -> ConfigureDb.da.updateById(update, inserted._id, new FilterValue(List.of("free"))));
		Assertions.assertTrue(ex.getMessage().contains(".free is a free-form org.bson.Document holding, under 'pojo'"),
				ex.getMessage());
		Assertions.assertNull(stored(inserted._id).get("free"));

		// Values, enums, classes of the JDK, nested documents, lists and maps are accepted.
		final Account accepted = new Account();
		accepted.name = "free-accepted";
		accepted.free = new Document("color", Color.RED).append("when", Instant.ofEpochMilli(1000))
				.append("amount", new BigDecimal("1.5")).append("bytes", new byte[] { 1, 2 })
				.append("nested", List.of(Map.of("k", new Document("leaf", "v"))))
				.append("where", new Point(new Position(2.35, 48.85)));
		final Account written = ConfigureDb.da.insert(accepted);
		final Document free = (Document) stored(written._id).get("free");
		Assertions.assertEquals("RED", free.get("color"));
		// A class of the driver is stored by its own codec (GeoJSON here).
		Assertions.assertEquals(new Document("type", "Point").append("coordinates", List.of(2.35, 48.85)),
				free.get("where"));
		Assertions.assertEquals(List.of(new Document("k", new Document("leaf", "v"))), free.get("nested"));
	}

	@Order(7)
	@Test
	public void testFreeFormDocumentAcceptsAnAllowlistOnly() throws Exception {
		final Derived pojo = derived("wrapped");
		final String notAccepted = ", which is not a value archidata accepts in a Document (a BSON or JDK value, an"
				+ " enum, a Document or a Map with String keys, a List, a Collection or an array of them): what it holds"
				+ " would not be checked; put only values, Documents, Lists and Maps in a Document, or declare a field"
				+ " with the class it holds";
		// A JDK wrapper is refused whatever it holds: its content would not be walked.
		final Map<String, Object> refused = new LinkedHashMap<>();
		refused.put("java.util.concurrent.atomic.AtomicReference", new AtomicReference<Object>(pojo));
		refused.put("java.util.Optional", Optional.of("value"));
		refused.put("java.util.KeyValueHolder", Map.entry("k", pojo));
		refused.put("java.lang.StringBuilder", new StringBuilder("text"));
		refused.put("java.util.HashMap with a java.lang.Integer key", new HashMap<>(Map.of(1, "one")));
		refused.put("com.fasterxml.jackson.databind.node.ObjectNode", JsonNodeFactory.instance.objectNode());
		// The exact class counts: a subclass of Date is not one of the listed values.
		refused.put("java.sql.Timestamp", new Timestamp(1000));
		for (final Map.Entry<String, Object> one : refused.entrySet()) {
			final Account account = new Account();
			account.name = "free-refused";
			account.free = new Document("ok", 1).append("list", List.of(new Document("x", one.getValue())));
			final Exception ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(account));
			Assertions.assertTrue(ex.getMessage()
					.endsWith("Field " + Account.class.getName()
							+ ".free is a free-form org.bson.Document holding, under 'list.0.x', a " + one.getKey()
							+ notAccepted),
					ex.getMessage());
		}
		// Walked: arrays of objects, the scope of a CodeWithScope, the id of a DBRef.
		final List<Document> walked = List.of(new Document("array", new Object[] { "a", pojo }),
				new Document("code", new CodeWithScope("f()", new Document("scope", pojo))),
				new Document("ref", new DBRef("other", pojo)));
		final List<String> paths = List.of("array.1", "code.scope", "ref.$id");
		for (int iii = 0; iii < walked.size(); iii++) {
			final Account account = new Account();
			account.name = "free-refused";
			account.free = walked.get(iii);
			final Exception ex = Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(account));
			Assertions.assertTrue(ex.getMessage().contains("holding, under '" + paths.get(iii) + "', a "
					+ Derived.class.getName() + ", a class of the application"), ex.getMessage());
		}
		Assertions.assertNull(ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.find(new Document("name", "free-refused")).first());

		// Accepted: BSON and JDK values, enums, documents, maps with String keys, collections and arrays of them.
		final Account accepted = new Account();
		accepted.name = "free-allowlist";
		final ObjectId oid = new ObjectId();
		final UUID uuid = UUID.randomUUID();
		accepted.free = new Document("oid", oid).append("uuid", uuid).append("date", new Date(1000))
				.append("decimal", new Decimal128(new BigDecimal("2.5"))).append("binary", new Binary(new byte[] { 3 }))
				.append("bson", new BsonDocument("b", new BsonString("v"))).append("day", LocalDate.of(2026, 1, 2))
				.append("long", 5L).append("char", 'c').append("bool", true).append("set", new TreeSet<>(Set.of("s")))
				.append("bytes", new byte[] { 4 }).append("when", OffsetDateTime.parse("2026-01-02T03:04:05Z"))
				.append("map", new HashMap<>(Map.of("k", List.of(Color.RED))));
		final Document free = (Document) stored(ConfigureDb.da.insert(accepted)._id).get("free");
		Assertions.assertEquals(oid, free.get("oid"));
		Assertions.assertEquals(uuid, free.get("uuid"));
		Assertions.assertEquals(new Decimal128(new BigDecimal("2.5")), free.get("decimal"));
		Assertions.assertEquals("v", ((Document) free.get("bson")).get("b"));
		Assertions.assertEquals(List.of("s"), free.get("set"));
		Assertions.assertEquals(new Binary(new byte[] { 4 }), free.get("bytes"));
		Assertions.assertEquals(new Document("k", List.of("RED")), free.get("map"));
	}

	@Order(8)
	@Test
	@SuppressWarnings("deprecation")
	public void testIsWrittenByRuntimeClassIsKeptDeprecated() throws Exception {
		// Nothing is written by the class of its value any more, whatever the declared type.
		Assertions.assertFalse(MongoCodecFactory.isWrittenByRuntimeClass(TypeInfo.ofRaw(Base.class)));
		Assertions.assertFalse(MongoCodecFactory.isWrittenByRuntimeClass(TypeInfo.ofRaw(ProtectedBase.class)));
		Assertions.assertFalse(MongoCodecFactory.isWrittenByRuntimeClass(TypeInfo.ofRaw(String.class)));
		Assertions.assertNotNull(MongoCodecFactory.class.getMethod("isWrittenByRuntimeClass", TypeInfo.class)
				.getAnnotation(Deprecated.class));
	}
}
