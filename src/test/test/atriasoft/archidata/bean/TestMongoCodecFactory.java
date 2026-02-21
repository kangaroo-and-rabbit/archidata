package test.atriasoft.archidata.bean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.atriasoft.archidata.dataAccess.model.codec.MongoTypeReader;
import org.atriasoft.archidata.dataAccess.model.codec.MongoTypeWriter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MongoCodecFactory} — verifies that pre-compiled writers and readers
 * correctly convert between Java types and MongoDB-compatible types.
 */
class TestMongoCodecFactory {

	// ===== Writer tests =====

	@Test
	void testWriterIdentityTypes() throws Exception {
		// Types that MongoDB driver handles natively: should return the same object
		assertWriterIdentity(String.class, "hello");
		assertWriterIdentity(Long.class, 42L);
		assertWriterIdentity(Integer.class, 42);
		assertWriterIdentity(Short.class, (short) 42);
		assertWriterIdentity(Float.class, 3.14f);
		assertWriterIdentity(Double.class, 3.14);
		assertWriterIdentity(Boolean.class, true);
		assertWriterIdentity(Date.class, new Date());
		assertWriterIdentity(ObjectId.class, new ObjectId());
	}

	@Test
	void testWriterPrimitiveTypes() throws Exception {
		assertWriterIdentity(long.class, 42L);
		assertWriterIdentity(int.class, 42);
		assertWriterIdentity(short.class, (short) 42);
		assertWriterIdentity(float.class, 3.14f);
		assertWriterIdentity(double.class, 3.14);
		assertWriterIdentity(boolean.class, true);
	}

	@Test
	void testWriterUUID() throws Exception {
		final UUID uuid = UUID.randomUUID();
		assertWriterIdentity(UUID.class, uuid);
	}

	@Test
	void testWriterInstant() throws Exception {
		// Use millisecond-precision Instant since Date only has millisecond precision
		final Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(Instant.class));
		final Object result = writer.toMongo(now);
		Assertions.assertInstanceOf(Date.class, result);
		Assertions.assertEquals(now, ((Date) result).toInstant());
	}

	@Test
	void testWriterLocalDate() throws Exception {
		final LocalDate date = LocalDate.of(2026, 2, 20);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(LocalDate.class));
		final Object result = writer.toMongo(date);
		Assertions.assertEquals("2026-02-20", result);
	}

	@Test
	void testWriterLocalTime() throws Exception {
		final LocalTime time = LocalTime.of(14, 30, 0);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(LocalTime.class));
		final Object result = writer.toMongo(time);
		Assertions.assertEquals(time.toNanoOfDay(), result);
	}

	@Test
	void testWriterEnum() throws Exception {
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(TestEnum.class));
		Assertions.assertEquals("VALUE_A", writer.toMongo(TestEnum.VALUE_A));
		Assertions.assertEquals("VALUE_B", writer.toMongo(TestEnum.VALUE_B));
	}

	@Test
	void testWriterList() throws Exception {
		final TypeInfo listOfString = new TypeInfo(List.class, String.class, null, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(listOfString);
		final List<String> input = List.of("a", "b", "c");
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(List.class, result);
		Assertions.assertEquals(List.of("a", "b", "c"), result);
	}

	@Test
	void testWriterSet() throws Exception {
		final TypeInfo setOfInt = new TypeInfo(Set.class, Integer.class, null, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(setOfInt);
		final Set<Integer> input = Set.of(1, 2, 3);
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(List.class, result);
		final List<?> resultList = (List<?>) result;
		Assertions.assertEquals(3, resultList.size());
		Assertions.assertTrue(resultList.containsAll(List.of(1, 2, 3)));
	}

	@Test
	void testWriterMap() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, String.class, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(mapType);
		final Map<String, String> input = Map.of("key1", "val1", "key2", "val2");
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(Document.class, result);
		final Document doc = (Document) result;
		Assertions.assertEquals("val1", doc.get("key1"));
		Assertions.assertEquals("val2", doc.get("key2"));
	}

	@Test
	void testWriterMapWithIntegerKey() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, Integer.class, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(mapType);
		final Map<Integer, String> input = Map.of(1, "one", 2, "two");
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(Document.class, result);
		final Document doc = (Document) result;
		Assertions.assertEquals("one", doc.get("1"));
		Assertions.assertEquals("two", doc.get("2"));
	}

	@Test
	void testWriterMapWithEnumKey() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, TestEnum.class, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(mapType);
		final Map<TestEnum, String> input = Map.of(TestEnum.VALUE_A, "a", TestEnum.VALUE_B, "b");
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(Document.class, result);
		final Document doc = (Document) result;
		Assertions.assertEquals("a", doc.get("VALUE_A"));
		Assertions.assertEquals("b", doc.get("VALUE_B"));
	}

	@Test
	void testWriterListOfInstant() throws Exception {
		final TypeInfo listOfInstant = new TypeInfo(List.class, Instant.class, null, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(listOfInstant);
		final Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
		final List<Instant> input = List.of(now);
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(List.class, result);
		final List<?> resultList = (List<?>) result;
		Assertions.assertEquals(1, resultList.size());
		Assertions.assertInstanceOf(Date.class, resultList.get(0));
		Assertions.assertEquals(now, ((Date) resultList.get(0)).toInstant());
	}

	@Test
	void testWriterListWithNull() throws Exception {
		final TypeInfo listOfString = new TypeInfo(List.class, String.class, null, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(listOfString);
		final List<String> input = new ArrayList<>();
		input.add("a");
		input.add(null);
		input.add("c");
		final Object result = writer.toMongo(input);
		Assertions.assertInstanceOf(List.class, result);
		final List<?> resultList = (List<?>) result;
		Assertions.assertEquals(3, resultList.size());
		Assertions.assertEquals("a", resultList.get(0));
		Assertions.assertNull(resultList.get(1));
		Assertions.assertEquals("c", resultList.get(2));
	}

	// ===== Reader tests =====

	@Test
	void testReaderIdentityTypes() throws Exception {
		assertReaderIdentity(String.class, "hello");
		assertReaderIdentity(Boolean.class, true);
		assertReaderIdentity(Date.class, new Date());
		assertReaderIdentity(ObjectId.class, new ObjectId());
	}

	@Test
	void testReaderLongCoercion() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Long.class));
		Assertions.assertEquals(42L, reader.fromMongo(42L));
		Assertions.assertEquals(42L, reader.fromMongo(42));       // Integer → Long
		Assertions.assertEquals(42L, reader.fromMongo((short) 42)); // Short → Long
	}

	@Test
	void testReaderIntegerCoercion() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Integer.class));
		Assertions.assertEquals(42, reader.fromMongo(42));
		Assertions.assertEquals(42, reader.fromMongo(42L));       // Long → Integer
		Assertions.assertEquals(42, reader.fromMongo((short) 42)); // Short → Integer
	}

	@Test
	void testReaderShortCoercion() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Short.class));
		Assertions.assertEquals((short) 42, reader.fromMongo((short) 42));
		Assertions.assertEquals((short) 42, reader.fromMongo(42));  // Integer → Short
		Assertions.assertEquals((short) 42, reader.fromMongo(42L)); // Long → Short
	}

	@Test
	void testReaderFloatCoercion() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Float.class));
		Assertions.assertEquals(3.14f, reader.fromMongo(3.14f));
		Assertions.assertEquals(3.14f, (float) (Float) reader.fromMongo(3.14), 0.01f); // Double → Float
	}

	@Test
	void testReaderDoubleCoercion() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Double.class));
		Assertions.assertEquals(3.14, reader.fromMongo(3.14));
		Assertions.assertEquals(3.14f, (double) (Double) reader.fromMongo(3.14f), 0.01); // Float → Double
	}

	@Test
	void testReaderInstant() throws Exception {
		// Use millisecond-precision Instant since Date only has millisecond precision
		final Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
		final Date mongoDate = Date.from(now);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Instant.class));
		Assertions.assertEquals(now, reader.fromMongo(mongoDate));
	}

	@Test
	void testReaderLocalDate() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(LocalDate.class));
		final LocalDate result = (LocalDate) reader.fromMongo("2026-02-20");
		Assertions.assertEquals(LocalDate.of(2026, 2, 20), result);
	}

	@Test
	void testReaderLocalTime() throws Exception {
		final LocalTime time = LocalTime.of(14, 30, 0);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(LocalTime.class));
		final LocalTime result = (LocalTime) reader.fromMongo(time.toNanoOfDay());
		Assertions.assertEquals(time, result);
	}

	@Test
	void testReaderEnum() throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(TestEnum.class));
		Assertions.assertEquals(TestEnum.VALUE_A, reader.fromMongo("VALUE_A"));
		Assertions.assertEquals(TestEnum.VALUE_B, reader.fromMongo("VALUE_B"));
	}

	@Test
	void testReaderEnumUnknownValue() {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(TestEnum.class));
		Assertions.assertThrows(Exception.class, () -> reader.fromMongo("UNKNOWN_VALUE"));
	}

	@Test
	void testReaderList() throws Exception {
		final TypeInfo listOfString = new TypeInfo(List.class, String.class, null, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(listOfString);
		final List<String> mongoValue = List.of("a", "b", "c");
		final Object result = reader.fromMongo(mongoValue);
		Assertions.assertInstanceOf(List.class, result);
		Assertions.assertEquals(List.of("a", "b", "c"), result);
	}

	@Test
	void testReaderSet() throws Exception {
		final TypeInfo setOfInt = new TypeInfo(Set.class, Integer.class, null, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(setOfInt);
		final List<Integer> mongoValue = List.of(1, 2, 3); // MongoDB stores Set as List
		final Object result = reader.fromMongo(mongoValue);
		Assertions.assertInstanceOf(Set.class, result);
		Assertions.assertEquals(Set.of(1, 2, 3), result);
	}

	@Test
	void testReaderMap() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, String.class, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(mapType);
		final Document mongoDoc = new Document("key1", "val1").append("key2", "val2");
		final Object result = reader.fromMongo(mongoDoc);
		Assertions.assertInstanceOf(Map.class, result);
		final Map<?, ?> map = (Map<?, ?>) result;
		Assertions.assertEquals("val1", map.get("key1"));
		Assertions.assertEquals("val2", map.get("key2"));
	}

	@Test
	void testReaderMapWithIntegerKey() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, Integer.class, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(mapType);
		final Document mongoDoc = new Document("1", "one").append("2", "two");
		final Object result = reader.fromMongo(mongoDoc);
		Assertions.assertInstanceOf(Map.class, result);
		final Map<?, ?> map = (Map<?, ?>) result;
		Assertions.assertEquals("one", map.get(1));
		Assertions.assertEquals("two", map.get(2));
	}

	@Test
	void testReaderMapWithEnumKey() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, String.class, TestEnum.class, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(mapType);
		final Document mongoDoc = new Document("VALUE_A", "a").append("VALUE_B", "b");
		final Object result = reader.fromMongo(mongoDoc);
		Assertions.assertInstanceOf(Map.class, result);
		final Map<?, ?> map = (Map<?, ?>) result;
		Assertions.assertEquals("a", map.get(TestEnum.VALUE_A));
		Assertions.assertEquals("b", map.get(TestEnum.VALUE_B));
	}

	@Test
	void testReaderListWithNull() throws Exception {
		final TypeInfo listOfString = new TypeInfo(List.class, String.class, null, null);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(listOfString);
		final List<String> mongoValue = new ArrayList<>();
		mongoValue.add("a");
		mongoValue.add(null);
		mongoValue.add("c");
		final Object result = reader.fromMongo(mongoValue);
		Assertions.assertInstanceOf(List.class, result);
		final List<?> list = (List<?>) result;
		Assertions.assertEquals("a", list.get(0));
		Assertions.assertNull(list.get(1));
		Assertions.assertEquals("c", list.get(2));
	}

	// ===== Roundtrip tests (write then read) =====

	@Test
	void testRoundtripInstant() throws Exception {
		// Use millisecond-precision Instant since Date only has millisecond precision
		final Instant original = Instant.ofEpochMilli(System.currentTimeMillis());
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(Instant.class));
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(Instant.class));
		final Object mongo = writer.toMongo(original);
		final Object back = reader.fromMongo(mongo);
		Assertions.assertEquals(original, back);
	}

	@Test
	void testRoundtripLocalDate() throws Exception {
		final LocalDate original = LocalDate.of(2026, 2, 20);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(LocalDate.class));
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(LocalDate.class));
		final Object mongo = writer.toMongo(original);
		final Object back = reader.fromMongo(mongo);
		Assertions.assertEquals(original, back);
	}

	@Test
	void testRoundtripLocalTime() throws Exception {
		final LocalTime original = LocalTime.of(14, 30, 15);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(LocalTime.class));
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(LocalTime.class));
		final Object mongo = writer.toMongo(original);
		final Object back = reader.fromMongo(mongo);
		Assertions.assertEquals(original, back);
	}

	@Test
	void testRoundtripEnum() throws Exception {
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(TestEnum.class));
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(TestEnum.class));
		for (final TestEnum value : TestEnum.values()) {
			final Object mongo = writer.toMongo(value);
			final Object back = reader.fromMongo(mongo);
			Assertions.assertEquals(value, back);
		}
	}

	@Test
	void testRoundtripList() throws Exception {
		final TypeInfo listOfLong = new TypeInfo(List.class, Long.class, null, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(listOfLong);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(listOfLong);
		final List<Long> original = List.of(1L, 2L, 3L);
		final Object mongo = writer.toMongo(original);
		final Object back = reader.fromMongo(mongo);
		Assertions.assertEquals(original, back);
	}

	@Test
	void testRoundtripMap() throws Exception {
		final TypeInfo mapType = new TypeInfo(Map.class, Integer.class, String.class, null);
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(mapType);
		final MongoTypeReader reader = MongoCodecFactory.buildReader(mapType);
		final Map<String, Integer> original = Map.of("a", 1, "b", 2);
		final Object mongo = writer.toMongo(original);
		final Object back = reader.fromMongo(mongo);
		Assertions.assertEquals(original, back);
	}

	// ===== TypeInfo.fromType tests =====

	@Test
	void testTypeInfoFromTypeClass() {
		final TypeInfo ti = TypeInfo.fromType(String.class);
		Assertions.assertEquals(String.class, ti.rawType());
		Assertions.assertNull(ti.elementType());
	}

	// ===== Helper =====

	private void assertWriterIdentity(final Class<?> type, final Object value) throws Exception {
		final MongoTypeWriter writer = MongoCodecFactory.buildWriter(TypeInfo.ofRaw(type));
		final Object result = writer.toMongo(value);
		Assertions.assertSame(value, result, "Writer for " + type.getSimpleName() + " should return identity");
	}

	private void assertReaderIdentity(final Class<?> type, final Object value) throws Exception {
		final MongoTypeReader reader = MongoCodecFactory.buildReader(TypeInfo.ofRaw(type));
		final Object result = reader.fromMongo(value);
		Assertions.assertSame(value, result, "Reader for " + type.getSimpleName() + " should return identity");
	}

	// ===== Test model =====

	public enum TestEnum {
		VALUE_A, VALUE_B, VALUE_C
	}
}
