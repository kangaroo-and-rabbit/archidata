package test.atriasoft.archidata.dataAccess;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A persisted field whose declared type does not tell which fields are stored is refused when the model is
 * analysed, at any depth of the embedded objects; the value types, the BSON types, the concrete classes and the
 * typed collections are accepted.
 */
public class TestDeclaredTypeRefusal {

	/** Interface of the application. */
	public interface Marker {}

	/** Abstract class of the application. */
	public abstract static class Shape {
		public String name;
	}

	/** Concrete embedded class. */
	public static class Point {
		public int x;
		public int y;
	}

	/** Embedded record. */
	public record Label(
			String text,
			int size) {}

	/** Enum. */
	public enum Color {
		RED, GREEN
	}

	/** Embedded class holding an Object field. */
	public static class Wrapper {
		public String name;
		public Object inner;
	}

	/** Three levels: the last one holds an Object field. */
	public static class Level1 {
		public Level2 level2;
	}

	/** Second level. */
	public static class Level2 {
		public Level3 level3;
	}

	/** Third level. */
	public static class Level3 {
		public String ok;
		public Object bad;
	}

	/** Recursive class without refused field. */
	public static class CleanNode {
		public String value;
		public CleanNode next;
		public List<CleanNode> children;
		public Map<String, CleanNode> byName;
	}

	/** Recursive class whose refused field is met through the cycle. */
	public static class BadNode {
		public String value;
		public BadNode next;
		public Marker marker;
	}

	/** Generic embedded class: its type variable is erased to Object. */
	public static class Box<T> {
		public T value;
	}

	/** Generic embedded class: the element type of its list is a type variable. */
	public static class BoxList<T> {
		public List<T> values;
	}

	// ---- Refused entities (one refused field each) ----

	public static class RObject {
		public ObjectId _id;
		public Object payload;
	}

	public static class RInterface {
		public ObjectId _id;
		public Marker marker;
	}

	public static class RAbstract {
		public ObjectId _id;
		public Shape shape;
	}

	public static class RSerializable {
		public ObjectId _id;
		public Serializable ser;
	}

	public static class RNumber {
		public ObjectId _id;
		public Number number;
	}

	public static class RCharSequence {
		public ObjectId _id;
		public CharSequence text;
	}

	public static class RJsonNode {
		public ObjectId _id;
		public JsonNode json;
	}

	@SuppressWarnings("rawtypes")
	public static class RRawList {
		public ObjectId _id;
		public List rawList;
	}

	@SuppressWarnings("rawtypes")
	public static class RRawSet {
		public ObjectId _id;
		public Set rawSet;
	}

	@SuppressWarnings("rawtypes")
	public static class RRawMap {
		public ObjectId _id;
		public Map rawMap;
	}

	public static class RWildcard {
		public ObjectId _id;
		public List<?> wild;
	}

	public static class RBoundedWildcard {
		public ObjectId _id;
		public List<? extends Point> points;
	}

	public static class RCollection {
		public ObjectId _id;
		public Collection<String> values;
	}

	public static class RIterable {
		public ObjectId _id;
		public Iterable<String> values;
	}

	public static class RQueue {
		public ObjectId _id;
		public Queue<String> values;
	}

	public static class RDeque {
		public ObjectId _id;
		public ArrayDeque<String> values;
	}

	public static class RListOfObject {
		public ObjectId _id;
		public List<Object> payloads;
	}

	public static class RMapOfObject {
		public ObjectId _id;
		public Map<String, Object> free;
	}

	public static class RSetOfInterface {
		public ObjectId _id;
		public Set<Marker> markers;
	}

	public static class RListOfListOfObject {
		public ObjectId _id;
		public List<List<Object>> nested;
	}

	@SuppressWarnings("rawtypes")
	public static class RMapOfListOfRawMap {
		public ObjectId _id;
		public Map<String, List<Map>> nested;
	}

	public static class REmbedded {
		public ObjectId _id;
		public Wrapper wrapper;
	}

	public static class RListOfEmbedded {
		public ObjectId _id;
		public List<Wrapper> wrappers;
	}

	public static class RMapOfEmbedded {
		public ObjectId _id;
		public Map<String, Wrapper> byKey;
	}

	public static class RDeep {
		public ObjectId _id;
		public Level1 level1;
	}

	public static class RCycle {
		public ObjectId _id;
		public BadNode node;
	}

	public static class RGeneric {
		public ObjectId _id;
		public Box<String> box;
	}

	public static class RGenericList {
		public ObjectId _id;
		public BoxList<String> box;
	}

	/** The property is persisted through its getter. */
	public static class RGetter {
		public ObjectId _id;

		public Object getComputed() {
			return null;
		}
	}

	/** Abstract entity: checked like a concrete one. */
	public abstract static class RAbstractRoot {
		public ObjectId _id;
		public Object payload;
	}

	public static class RArrayOfEmbedded {
		public ObjectId _id;
		public Point[] points;
	}

	public static class RArrayOfObject {
		public ObjectId _id;
		public Object[] values;
	}

	public static class RMatrixOfInterface {
		public ObjectId _id;
		public Serializable[][] values;
	}

	public static class RListOfArray {
		public ObjectId _id;
		public List<Point[]> pointArrays;
	}

	/** Holder of declared types, for the fail-closed answer of the protection check. */
	public static class TypeHolder {
		public List<Object> anyList;
		public Map<String, Serializable> anyMap;
		public Map<String, List<Marker>> markerLists;
	}

	/** A class of the application extending a JDK class archidata does not model. */
	public static class Money extends BigDecimal {
		private static final long serialVersionUID = 1L;
		@DataNotRead
		public String secret = "s3cr3t";

		public Money() {
			super("1");
		}
	}

	/** A class of the application implementing a BSON interface. */
	public static class AppBson implements Bson {
		@DataNotRead
		public String secret = "s3cr3t";

		@Override
		public <T> BsonDocument toBsonDocument(final Class<T> documentClass, final CodecRegistry codecRegistry) {
			return new BsonDocument();
		}
	}

	/** A class of a library is walked as an embedded object: one of its properties is abstract. */
	public static class RThirdParty {
		public ObjectId _id;
		public com.mongodb.client.model.geojson.Point location;
	}

	// ---- Accepted entity ----

	public static class Accepted {
		public ObjectId _id;
		public int primitiveInt;
		public long primitiveLong;
		public boolean primitiveBoolean;
		public double primitiveDouble;
		public Integer boxedInt;
		public Long boxedLong;
		public Float boxedFloat;
		public Short boxedShort;
		public Character character;
		public String text;
		public UUID uuid;
		public ObjectId oid;
		public Date date;
		public Instant instant;
		public OffsetDateTime offsetDateTime;
		public LocalDate localDate;
		public LocalTime localTime;
		public Color color;
		public Document document;
		public List<Document> documents;
		public Map<String, Document> documentByKey;
		public Decimal128 decimal128;
		public byte[] bytes;
		public String[] stringArray;
		public int[][] matrix;
		public BigDecimal[] decimals;
		public Document[] documentArray;
		public Color[] colors;
		public BigDecimal bigDecimal;
		public LocalDateTime localDateTime;
		public Optional<String> optional;
		public ObjectNode objectNode;
		public List<String> strings;
		public Set<Integer> integers;
		public Map<String, Long> longByKey;
		public Map<ObjectId, Long> longByOid;
		public Map<Color, String> stringByColor;
		public List<List<String>> stringLists;
		public Map<String, List<Point>> pointLists;
		public Point point;
		public Label label;
		public List<Point> points;
		public Set<Label> labels;
		public Map<String, Point> pointByKey;
		public CleanNode tree;
	}

	private static String errorOf(final Class<?> clazz) {
		final IntrospectionException ex = Assertions.assertThrows(IntrospectionException.class,
				() -> DbClassModel.of(clazz));
		Assertions.assertEquals("DB model creation failed for " + clazz.getSimpleName(), ex.getMessage());
		final DataAccessException cause = Assertions.assertInstanceOf(DataAccessException.class, ex.getCause());
		final String message = cause.getMessage();
		Assertions.assertTrue(message.startsWith("Unsupported field type on " + clazz.getName() + ": field "), message);
		Assertions.assertTrue(message.endsWith("(see MIGRATION.md)"), message);
		return message;
	}

	private static void assertRefused(final Class<?> clazz, final String... fragments) {
		final String message = errorOf(clazz);
		for (final String fragment : fragments) {
			Assertions.assertTrue(message.contains(fragment), "'" + fragment + "' not in: " + message);
		}
	}

	private static final String ANY_CLASS = "can hold any class (Object, an interface or an abstract class)";
	private static final String REPLACEMENT = "declare a concrete class (or a List, Set or Map of one), or"
			+ " org.bson.Document for free-form content";

	@Test
	public void testObjectIsRefused() {
		final String message = errorOf(RObject.class);
		Assertions.assertEquals("Unsupported field type on " + RObject.class.getName()
				+ ": field RObject.payload (path payload) is declared java.lang.Object, which " + ANY_CLASS
				+ ", and archidata stores and reads an embedded value by its declared type only; " + REPLACEMENT
				+ " (see MIGRATION.md)", message);
	}

	@Test
	public void testInterfacesAndAbstractClassesAreRefused() {
		assertRefused(RInterface.class, "field RInterface.marker (path marker) is declared "
				+ Marker.class.getTypeName() + ", which " + ANY_CLASS, REPLACEMENT);
		assertRefused(RAbstract.class,
				"(path shape) is declared " + Shape.class.getTypeName() + ", which " + ANY_CLASS);
		assertRefused(RSerializable.class, "(path ser) is declared java.io.Serializable, which " + ANY_CLASS);
		assertRefused(RNumber.class, "(path number) is declared java.lang.Number, which " + ANY_CLASS);
		assertRefused(RCharSequence.class, "(path text) is declared java.lang.CharSequence, which " + ANY_CLASS);
		assertRefused(RJsonNode.class,
				"(path json) is declared com.fasterxml.jackson.databind.JsonNode, which " + ANY_CLASS);
	}

	@Test
	public void testRawAndUnresolvedCollectionsAreRefused() {
		assertRefused(RRawList.class,
				"(path rawList) is declared java.util.List, which is a raw type whose element type is unknown",
				"declare the element type as a concrete class");
		assertRefused(RRawSet.class,
				"(path rawSet) is declared java.util.Set, which is a raw type whose element type is unknown");
		assertRefused(RRawMap.class,
				"(path rawMap) is declared java.util.Map, which is a raw type whose value type is unknown");
		assertRefused(RWildcard.class, "(path wild) is declared java.util.List<?>, which has an element type that"
				+ " cannot be resolved (a wildcard or a type variable)");
		assertRefused(RBoundedWildcard.class, "(path points) is declared java.util.List<? extends "
				+ Point.class.getTypeName() + ">, which has an element type that cannot be resolved");
	}

	@Test
	public void testOtherCollectionsAreRefused() {
		final String notListOrSet = "which is a collection that is neither a List nor a Set: only List, Set and Map"
				+ " are stored as collections; declare a List or a Set";
		assertRefused(RCollection.class,
				"(path values) is declared java.util.Collection<java.lang.String>, " + notListOrSet);
		assertRefused(RIterable.class,
				"(path values) is declared java.lang.Iterable<java.lang.String>, " + notListOrSet);
		assertRefused(RQueue.class, "(path values) is declared java.util.Queue<java.lang.String>, " + notListOrSet);
		assertRefused(RDeque.class,
				"(path values) is declared java.util.ArrayDeque<java.lang.String>, " + notListOrSet);
	}

	@Test
	public void testElementAndValueTypesAreChecked() {
		assertRefused(RListOfObject.class, "(path payloads) is declared java.util.List<java.lang.Object>, whose"
				+ " element type java.lang.Object " + ANY_CLASS);
		assertRefused(RMapOfObject.class, "(path free.<key>) is declared java.util.Map<java.lang.String,"
				+ " java.lang.Object>, whose value type java.lang.Object " + ANY_CLASS);
		assertRefused(RSetOfInterface.class, "whose element type " + Marker.class.getTypeName() + " " + ANY_CLASS);
		assertRefused(RListOfListOfObject.class,
				"(path nested) is declared"
						+ " java.util.List<java.util.List<java.lang.Object>>, whose element type java.lang.Object "
						+ ANY_CLASS);
		assertRefused(RMapOfListOfRawMap.class, "(path nested.<key>) is declared",
				"whose element type java.util.Map is a raw type whose value type is unknown");
	}

	@Test
	public void testEmbeddedClassesAreCheckedAtAnyDepth() {
		assertRefused(REmbedded.class, "field Wrapper.inner (path wrapper.inner) is declared java.lang.Object");
		assertRefused(RListOfEmbedded.class, "field Wrapper.inner (path wrappers.inner) is declared java.lang.Object");
		assertRefused(RMapOfEmbedded.class,
				"field Wrapper.inner (path byKey.<key>.inner) is declared java.lang.Object");
		assertRefused(RDeep.class, "field Level3.bad (path level1.level2.level3.bad) is declared java.lang.Object");
		assertRefused(RCycle.class,
				"field BadNode.marker (path node.marker) is declared " + Marker.class.getTypeName());
		// A type variable is erased to Object: refused, named as declared.
		assertRefused(RGeneric.class, "field Box.value (path box.value) is declared T, which " + ANY_CLASS);
		assertRefused(RGenericList.class, "field BoxList.values (path box.values) is declared java.util.List<T>,"
				+ " which has an element type that cannot be resolved");
		assertRefused(RGetter.class, "field RGetter.computed (path computed) is declared java.lang.Object");
	}

	@Test
	public void testRefusedModelStaysRefused() {
		final String first = errorOf(REmbedded.class);
		final String second = errorOf(REmbedded.class);
		Assertions.assertEquals(first, second);
	}

	@Test
	public void testAcceptedShapes() throws Exception {
		final DbClassModel model = DbClassModel.of(Accepted.class);
		Assertions.assertNotNull(model.findByDbFieldName("tree"));
		Assertions.assertNotNull(DbClassModel.of(CleanNode.class));
	}

	@Test
	public void testAbstractRootIsChecked() {
		assertRefused(RAbstractRoot.class, "field RAbstractRoot.payload (path payload) is declared java.lang.Object",
				ANY_CLASS);
	}

	@Test
	public void testArraysOfModeledOrAnyClassAreRefused() {
		final String array = "is an array of a class of the application (or of a type that can hold any class), which"
				+ " archidata does not store element by element: only List, Set and Map are stored as collections;"
				+ " declare a List or a Set";
		assertRefused(RArrayOfEmbedded.class, "field RArrayOfEmbedded.points (path points) is declared "
				+ Point.class.getName() + "[], which " + array);
		assertRefused(RArrayOfObject.class, "is declared java.lang.Object[], which " + array);
		assertRefused(RMatrixOfInterface.class, "is declared java.io.Serializable[][], which " + array);
		assertRefused(RListOfArray.class, "whose element type " + Point.class.getName() + "[] " + array);
	}

	@Test
	public void testLibraryClassesAreWalkedAsEmbeddedObjects() {
		// Any class outside the JDK, Jackson and BSON is modeled field by field: a polymorphic property of a
		// library class (always null or not) refuses the entity.
		assertRefused(RThirdParty.class,
				"field Point.coordinateReferenceSystem (path location.coordinateReferenceSystem)"
						+ " is declared com.mongodb.client.model.geojson.CoordinateReferenceSystem, which "
						+ ANY_CLASS);
	}

	@Test
	public void testUnresolvableTypesCountAsHoldingProtectedFields() throws Exception {
		// Not reachable from an entity (refused at analysis): fail closed all the same.
		Assertions.assertTrue(MongoCodecFactory.hasProtectedSubFields(TypeInfo.ofRaw(Object.class)));
		Assertions.assertTrue(MongoCodecFactory.hasProtectedSubFields(TypeInfo.ofRaw(Shape.class)));
		Assertions.assertTrue(MongoCodecFactory.hasProtectedSubFields(TypeInfo.ofRaw(List.class)));
		for (final String name : List.of("anyList", "anyMap", "markerLists")) {
			Assertions.assertTrue(MongoCodecFactory
					.hasProtectedSubFields(TypeInfo.fromType(TypeHolder.class.getField(name).getGenericType())), name);
		}
		// A concrete class without protected field, and a class archidata does not model: nothing protected.
		Assertions.assertFalse(MongoCodecFactory.hasProtectedSubFields(TypeInfo.ofRaw(Point.class)));
		Assertions.assertFalse(MongoCodecFactory.hasProtectedSubFields(TypeInfo.ofRaw(BigDecimal.class)));
	}

	@Test
	public void testApplicationValueUnderAnUnmodeledTypeIsRefused() {
		final String expected = ", a type archidata does not model, but holds a ";
		DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> MongoCodecFactory.buildWriter(TypeInfo.ofRaw(BigDecimal.class)).toMongo(new Money()));
		Assertions.assertEquals(
				"A value is declared java.math.BigDecimal" + expected + Money.class.getName()
						+ ", a class of the application: it cannot be stored there; declare the field with that class",
				ex.getMessage());
		ex = Assertions.assertThrows(DataAccessException.class, () -> MongoCodecFactory
				.buildWriter(TypeInfo.ofRaw(BigDecimal[].class)).toMongo(new Money[] { new Money() }));
		Assertions.assertTrue(ex.getMessage().contains(expected + "[L" + Money.class.getName() + ";"), ex.getMessage());
		ex = Assertions.assertThrows(DataAccessException.class,
				() -> MongoCodecFactory.buildWriter(TypeInfo.ofRaw(Bson.class)).toMongo(new AppBson()));
		Assertions.assertTrue(
				ex.getMessage().startsWith(
						"A value is declared org.bson.conversions.Bson" + expected + AppBson.class.getName()),
				ex.getMessage());
		// A value of the JDK class itself is written as before (by its own class).
		Assertions.assertDoesNotThrow(
				() -> MongoCodecFactory.buildWriter(TypeInfo.ofRaw(BigDecimal.class)).toMongo(new BigDecimal("2")));
	}
}
