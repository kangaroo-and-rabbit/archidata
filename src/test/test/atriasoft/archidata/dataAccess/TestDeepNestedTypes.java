package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.model.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.ComplexSubObject;
import test.atriasoft.archidata.dataAccess.model.DeepNestedModel;
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;
import test.atriasoft.archidata.dataAccess.model.NestedSubObject;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDeepNestedTypes {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeepNestedTypes.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private static ComplexSubObject createComplexSubObject(
			final String name,
			final Integer count,
			final Boolean active,
			final Enum2ForTest status,
			final List<String> tags) {
		return new ComplexSubObject(name, count, active, status, tags);
	}

	// ========================================================================
	// CRUD tests on complex nested types
	// ========================================================================

	@Order(1)
	@Test
	public void testSimpleObject() throws Exception {
		LOGGER.info("Test insert/get of a rich POJO (ComplexSubObject)");
		final DeepNestedModel test = new DeepNestedModel();
		test.simpleObject = createComplexSubObject("alpha", 42, true, Enum2ForTest.ENUM_VALUE_1,
				List.of("tag1", "tag2", "tag3"));

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.getOid());
		Assertions.assertEquals(test.simpleObject, insertedData.simpleObject);

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getOid());
		Assertions.assertEquals(test.simpleObject, retrieve.simpleObject);
	}

	@Order(2)
	@Test
	public void testNestedObject() throws Exception {
		LOGGER.info("Test insert/get of object with sub-sub-object (3 levels)");
		final DeepNestedModel test = new DeepNestedModel();
		final ComplexSubObject inner = createComplexSubObject("inner-name", 99, false, Enum2ForTest.ENUM_VALUE_3,
				List.of("deep", "nested"));
		test.nestedObject = new NestedSubObject("outer-label", inner);

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.getOid());
		Assertions.assertEquals(test.nestedObject, insertedData.nestedObject);

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(test.nestedObject, retrieve.nestedObject);
		// Verify deep fields explicitly
		Assertions.assertEquals("outer-label", retrieve.nestedObject.label);
		Assertions.assertEquals("inner-name", retrieve.nestedObject.inner.name);
		Assertions.assertEquals(99, retrieve.nestedObject.inner.count);
		Assertions.assertEquals(false, retrieve.nestedObject.inner.active);
		Assertions.assertEquals(Enum2ForTest.ENUM_VALUE_3, retrieve.nestedObject.inner.status);
		Assertions.assertEquals(List.of("deep", "nested"), retrieve.nestedObject.inner.tags);
	}

	@Order(3)
	@Test
	public void testMapOfObjects() throws Exception {
		LOGGER.info("Test insert/get of Map<String, ComplexSubObject>");
		final DeepNestedModel test = new DeepNestedModel();
		test.mapOfObjects = new HashMap<>();
		test.mapOfObjects.put("first",
				createComplexSubObject("obj1", 10, true, Enum2ForTest.ENUM_VALUE_1, List.of("a")));
		test.mapOfObjects.put("second",
				createComplexSubObject("obj2", 20, false, Enum2ForTest.ENUM_VALUE_2, List.of("b", "c")));

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.mapOfObjects);
		Assertions.assertEquals(2, insertedData.mapOfObjects.size());
		Assertions.assertEquals(test.mapOfObjects.get("first"), insertedData.mapOfObjects.get("first"));
		Assertions.assertEquals(test.mapOfObjects.get("second"), insertedData.mapOfObjects.get("second"));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(2, retrieve.mapOfObjects.size());
		Assertions.assertEquals(test.mapOfObjects.get("first"), retrieve.mapOfObjects.get("first"));
		Assertions.assertEquals(test.mapOfObjects.get("second"), retrieve.mapOfObjects.get("second"));
	}

	@Order(4)
	@Test
	public void testMapOfMapOfObjects() throws Exception {
		LOGGER.info("Test insert/get of Map<String, Map<String, ComplexSubObject>>");
		final DeepNestedModel test = new DeepNestedModel();
		test.mapOfMapOfObjects = new HashMap<>();

		final Map<String, ComplexSubObject> innerMap1 = new HashMap<>();
		innerMap1.put("k1", createComplexSubObject("nested1", 1, true, Enum2ForTest.ENUM_VALUE_1, List.of("x")));
		innerMap1.put("k2", createComplexSubObject("nested2", 2, false, Enum2ForTest.ENUM_VALUE_2, List.of("y")));

		final Map<String, ComplexSubObject> innerMap2 = new HashMap<>();
		innerMap2.put("k3", createComplexSubObject("nested3", 3, true, Enum2ForTest.ENUM_VALUE_4, List.of("z")));

		test.mapOfMapOfObjects.put("groupA", innerMap1);
		test.mapOfMapOfObjects.put("groupB", innerMap2);

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.mapOfMapOfObjects);
		Assertions.assertEquals(2, insertedData.mapOfMapOfObjects.size());
		Assertions.assertEquals(2, insertedData.mapOfMapOfObjects.get("groupA").size());
		Assertions.assertEquals(1, insertedData.mapOfMapOfObjects.get("groupB").size());
		Assertions.assertEquals(innerMap1.get("k1"), insertedData.mapOfMapOfObjects.get("groupA").get("k1"));
		Assertions.assertEquals(innerMap1.get("k2"), insertedData.mapOfMapOfObjects.get("groupA").get("k2"));
		Assertions.assertEquals(innerMap2.get("k3"), insertedData.mapOfMapOfObjects.get("groupB").get("k3"));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.mapOfMapOfObjects);
		Assertions.assertEquals(2, retrieve.mapOfMapOfObjects.size());
		Assertions.assertEquals(innerMap1.get("k1"), retrieve.mapOfMapOfObjects.get("groupA").get("k1"));
		Assertions.assertEquals(innerMap1.get("k2"), retrieve.mapOfMapOfObjects.get("groupA").get("k2"));
		Assertions.assertEquals(innerMap2.get("k3"), retrieve.mapOfMapOfObjects.get("groupB").get("k3"));
	}

	@Order(5)
	@Test
	public void testMapOfListOfObjects() throws Exception {
		LOGGER.info("Test insert/get of Map<String, List<ComplexSubObject>>");
		final DeepNestedModel test = new DeepNestedModel();
		test.mapOfListOfObjects = new HashMap<>();

		final List<ComplexSubObject> list1 = new ArrayList<>();
		list1.add(createComplexSubObject("item1", 10, true, Enum2ForTest.ENUM_VALUE_1, List.of("a")));
		list1.add(createComplexSubObject("item2", 20, false, Enum2ForTest.ENUM_VALUE_2, List.of("b")));

		final List<ComplexSubObject> list2 = new ArrayList<>();
		list2.add(createComplexSubObject("item3", 30, true, Enum2ForTest.ENUM_VALUE_5, List.of("c", "d")));

		test.mapOfListOfObjects.put("listA", list1);
		test.mapOfListOfObjects.put("listB", list2);

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.mapOfListOfObjects);
		Assertions.assertEquals(2, insertedData.mapOfListOfObjects.size());
		Assertions.assertEquals(2, insertedData.mapOfListOfObjects.get("listA").size());
		Assertions.assertEquals(1, insertedData.mapOfListOfObjects.get("listB").size());
		Assertions.assertEquals(list1.get(0), insertedData.mapOfListOfObjects.get("listA").get(0));
		Assertions.assertEquals(list1.get(1), insertedData.mapOfListOfObjects.get("listA").get(1));
		Assertions.assertEquals(list2.get(0), insertedData.mapOfListOfObjects.get("listB").get(0));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.mapOfListOfObjects);
		Assertions.assertEquals(2, retrieve.mapOfListOfObjects.size());
		Assertions.assertEquals(list1.get(0), retrieve.mapOfListOfObjects.get("listA").get(0));
		Assertions.assertEquals(list1.get(1), retrieve.mapOfListOfObjects.get("listA").get(1));
		Assertions.assertEquals(list2.get(0), retrieve.mapOfListOfObjects.get("listB").get(0));
	}

	@Order(6)
	@Test
	public void testListOfObjects() throws Exception {
		LOGGER.info("Test insert/get of List<ComplexSubObject> with rich fields");
		final DeepNestedModel test = new DeepNestedModel();
		test.listOfObjects = new ArrayList<>();
		test.listOfObjects
				.add(createComplexSubObject("elem1", 100, true, Enum2ForTest.ENUM_VALUE_1, List.of("t1", "t2")));
		test.listOfObjects.add(createComplexSubObject("elem2", 200, false, Enum2ForTest.ENUM_VALUE_3, List.of("t3")));
		test.listOfObjects.add(createComplexSubObject("elem3", 300, true, Enum2ForTest.ENUM_VALUE_5, List.of()));

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.listOfObjects);
		Assertions.assertEquals(3, insertedData.listOfObjects.size());
		Assertions.assertEquals(test.listOfObjects.get(0), insertedData.listOfObjects.get(0));
		Assertions.assertEquals(test.listOfObjects.get(1), insertedData.listOfObjects.get(1));
		Assertions.assertEquals(test.listOfObjects.get(2), insertedData.listOfObjects.get(2));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.listOfObjects);
		Assertions.assertEquals(3, retrieve.listOfObjects.size());
		Assertions.assertEquals(test.listOfObjects.get(0), retrieve.listOfObjects.get(0));
		Assertions.assertEquals(test.listOfObjects.get(1), retrieve.listOfObjects.get(1));
		Assertions.assertEquals(test.listOfObjects.get(2), retrieve.listOfObjects.get(2));
	}

	@Order(7)
	@Test
	public void testListOfMaps() throws Exception {
		LOGGER.info("Test insert/get of List<Map<String, Integer>>");
		final DeepNestedModel test = new DeepNestedModel();
		test.listOfMaps = new ArrayList<>();

		final Map<String, Integer> map1 = new HashMap<>();
		map1.put("score", 95);
		map1.put("level", 5);

		final Map<String, Integer> map2 = new HashMap<>();
		map2.put("score", 80);
		map2.put("level", 3);
		map2.put("bonus", 10);

		test.listOfMaps.add(map1);
		test.listOfMaps.add(map2);

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.listOfMaps);
		Assertions.assertEquals(2, insertedData.listOfMaps.size());
		Assertions.assertEquals(map1, insertedData.listOfMaps.get(0));
		Assertions.assertEquals(map2, insertedData.listOfMaps.get(1));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.listOfMaps);
		Assertions.assertEquals(2, retrieve.listOfMaps.size());
		Assertions.assertEquals(map1, retrieve.listOfMaps.get(0));
		Assertions.assertEquals(map2, retrieve.listOfMaps.get(1));
	}

	@Order(8)
	@Test
	public void testSetOfStrings() throws Exception {
		LOGGER.info("Test insert/get of Set<String>");
		final DeepNestedModel test = new DeepNestedModel();
		test.setOfStrings = new HashSet<>();
		test.setOfStrings.add("apple");
		test.setOfStrings.add("banana");
		test.setOfStrings.add("cherry");

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.setOfStrings);
		Assertions.assertEquals(3, insertedData.setOfStrings.size());
		Assertions.assertTrue(insertedData.setOfStrings.contains("apple"));
		Assertions.assertTrue(insertedData.setOfStrings.contains("banana"));
		Assertions.assertTrue(insertedData.setOfStrings.contains("cherry"));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.setOfStrings);
		Assertions.assertEquals(3, retrieve.setOfStrings.size());
		Assertions.assertTrue(retrieve.setOfStrings.contains("apple"));
		Assertions.assertTrue(retrieve.setOfStrings.contains("banana"));
		Assertions.assertTrue(retrieve.setOfStrings.contains("cherry"));
	}

	@Order(9)
	@Test
	public void testMapOfSets() throws Exception {
		LOGGER.info("Test insert/get of Map<String, Set<String>>");
		final DeepNestedModel test = new DeepNestedModel();
		test.mapOfSets = new HashMap<>();

		final Set<String> set1 = new HashSet<>();
		set1.add("red");
		set1.add("green");

		final Set<String> set2 = new HashSet<>();
		set2.add("circle");
		set2.add("square");
		set2.add("triangle");

		test.mapOfSets.put("colors", set1);
		test.mapOfSets.put("shapes", set2);

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.mapOfSets);
		Assertions.assertEquals(2, insertedData.mapOfSets.size());
		Assertions.assertEquals(set1, insertedData.mapOfSets.get("colors"));
		Assertions.assertEquals(set2, insertedData.mapOfSets.get("shapes"));

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.mapOfSets);
		Assertions.assertEquals(2, retrieve.mapOfSets.size());
		Assertions.assertEquals(set1, retrieve.mapOfSets.get("colors"));
		Assertions.assertEquals(set2, retrieve.mapOfSets.get("shapes"));
	}

	@Order(10)
	@Test
	public void testNullFields() throws Exception {
		LOGGER.info("Test that null fields in sub-objects are handled correctly");
		final DeepNestedModel test = new DeepNestedModel();
		// ComplexSubObject with some null fields
		test.simpleObject = new ComplexSubObject();
		test.simpleObject.name = "only-name";
		// count, active, status, tags are all null

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.simpleObject);
		Assertions.assertEquals("only-name", insertedData.simpleObject.name);
		Assertions.assertNull(insertedData.simpleObject.count);
		Assertions.assertNull(insertedData.simpleObject.active);
		Assertions.assertNull(insertedData.simpleObject.status);
		Assertions.assertNull(insertedData.simpleObject.tags);

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.simpleObject);
		Assertions.assertEquals("only-name", retrieve.simpleObject.name);
		Assertions.assertNull(retrieve.simpleObject.count);
		Assertions.assertNull(retrieve.simpleObject.active);
		Assertions.assertNull(retrieve.simpleObject.status);
		Assertions.assertNull(retrieve.simpleObject.tags);
	}

	@Order(11)
	@Test
	public void testEmptyCollections() throws Exception {
		LOGGER.info("Test empty collections (List, Map, Set)");
		final DeepNestedModel test = new DeepNestedModel();
		test.listOfObjects = new ArrayList<>();
		test.mapOfObjects = new HashMap<>();
		test.setOfStrings = new HashSet<>();
		test.listOfMaps = new ArrayList<>();
		test.mapOfSets = new HashMap<>();

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.listOfObjects);
		Assertions.assertEquals(0, insertedData.listOfObjects.size());
		Assertions.assertNotNull(insertedData.mapOfObjects);
		Assertions.assertEquals(0, insertedData.mapOfObjects.size());
		Assertions.assertNotNull(insertedData.setOfStrings);
		Assertions.assertEquals(0, insertedData.setOfStrings.size());

		// Retrieve from DB
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		// Note: empty collections may be null or empty after deserialization
		// depending on MongoDB driver behavior
		if (retrieve.listOfObjects != null) {
			Assertions.assertEquals(0, retrieve.listOfObjects.size());
		}
		if (retrieve.mapOfObjects != null) {
			Assertions.assertEquals(0, retrieve.mapOfObjects.size());
		}
		if (retrieve.setOfStrings != null) {
			Assertions.assertEquals(0, retrieve.setOfStrings.size());
		}
	}

	@Order(12)
	@Test
	public void testUpdateNestedField() throws Exception {
		LOGGER.info("Test update of a field containing a nested object");
		final DeepNestedModel test = new DeepNestedModel();
		test.simpleObject = createComplexSubObject("before", 1, true, Enum2ForTest.ENUM_VALUE_1, List.of("old"));

		final DeepNestedModel insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.getOid());

		// Update the simpleObject field
		final DeepNestedModel updatePayload = new DeepNestedModel();
		updatePayload.simpleObject = createComplexSubObject("after", 999, false, Enum2ForTest.ENUM_VALUE_5,
				List.of("new1", "new2"));

		ConfigureDb.da.updateById(updatePayload, insertedData.getOid());

		// Retrieve and verify updated
		final DeepNestedModel retrieve = ConfigureDb.da.getById(DeepNestedModel.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.simpleObject);
		Assertions.assertEquals("after", retrieve.simpleObject.name);
		Assertions.assertEquals(999, retrieve.simpleObject.count);
		Assertions.assertEquals(false, retrieve.simpleObject.active);
		Assertions.assertEquals(Enum2ForTest.ENUM_VALUE_5, retrieve.simpleObject.status);
		Assertions.assertEquals(List.of("new1", "new2"), retrieve.simpleObject.tags);
	}

	// ========================================================================
	// Sub-field query tests (MongoDB dot notation)
	// ========================================================================

	@Order(20)
	@Test
	public void testQuerySubField() throws Exception {
		LOGGER.info("Test query on sub-field: simpleObject.name");
		// Insert two entries with different simpleObject.name
		final DeepNestedModel entry1 = new DeepNestedModel();
		entry1.simpleObject = createComplexSubObject("findme", 10, true, Enum2ForTest.ENUM_VALUE_1, List.of("q"));

		final DeepNestedModel entry2 = new DeepNestedModel();
		entry2.simpleObject = createComplexSubObject("ignoreme", 20, false, Enum2ForTest.ENUM_VALUE_2, List.of("q"));

		final DeepNestedModel inserted1 = ConfigureDb.da.insert(entry1);
		ConfigureDb.da.insert(entry2);

		// Query by sub-field
		final List<DeepNestedModel> results = ConfigureDb.da.gets(DeepNestedModel.class,
				new Condition(Filters.eq("simpleObject.name", "findme")));

		Assertions.assertNotNull(results);
		Assertions.assertFalse(results.isEmpty());
		// All results should have simpleObject.name == "findme"
		for (final DeepNestedModel result : results) {
			Assertions.assertEquals("findme", result.simpleObject.name);
		}
		// The entry we just inserted should be in the results
		final boolean found = results.stream().anyMatch(r -> r.getOid().equals(inserted1.getOid()));
		Assertions.assertTrue(found, "Expected to find entry1 in query results");
	}

	@Order(21)
	@Test
	public void testQueryDeepSubField() throws Exception {
		LOGGER.info("Test query on deep sub-field: nestedObject.inner.name");
		final DeepNestedModel entry1 = new DeepNestedModel();
		final ComplexSubObject inner1 = createComplexSubObject("deep-target", 5, true, Enum2ForTest.ENUM_VALUE_3,
				List.of());
		entry1.nestedObject = new NestedSubObject("label1", inner1);

		final DeepNestedModel entry2 = new DeepNestedModel();
		final ComplexSubObject inner2 = createComplexSubObject("deep-other", 6, false, Enum2ForTest.ENUM_VALUE_4,
				List.of());
		entry2.nestedObject = new NestedSubObject("label2", inner2);

		final DeepNestedModel inserted1 = ConfigureDb.da.insert(entry1);
		ConfigureDb.da.insert(entry2);

		// Query by deep sub-field
		final List<DeepNestedModel> results = ConfigureDb.da.gets(DeepNestedModel.class,
				new Condition(Filters.eq("nestedObject.inner.name", "deep-target")));

		Assertions.assertNotNull(results);
		Assertions.assertFalse(results.isEmpty());
		for (final DeepNestedModel result : results) {
			Assertions.assertEquals("deep-target", result.nestedObject.inner.name);
		}
		final boolean found = results.stream().anyMatch(r -> r.getOid().equals(inserted1.getOid()));
		Assertions.assertTrue(found, "Expected to find entry1 in deep sub-field query results");
	}

	@Order(22)
	@Test
	public void testQueryMapValueField() throws Exception {
		LOGGER.info("Test query on map value field: mapOfObjects.myKey.name");
		final DeepNestedModel entry1 = new DeepNestedModel();
		entry1.mapOfObjects = new HashMap<>();
		entry1.mapOfObjects.put("myKey",
				createComplexSubObject("map-target", 77, true, Enum2ForTest.ENUM_VALUE_1, List.of()));

		final DeepNestedModel entry2 = new DeepNestedModel();
		entry2.mapOfObjects = new HashMap<>();
		entry2.mapOfObjects.put("myKey",
				createComplexSubObject("map-other", 88, false, Enum2ForTest.ENUM_VALUE_2, List.of()));

		final DeepNestedModel inserted1 = ConfigureDb.da.insert(entry1);
		ConfigureDb.da.insert(entry2);

		// Query by map value sub-field
		final List<DeepNestedModel> results = ConfigureDb.da.gets(DeepNestedModel.class,
				new Condition(Filters.eq("mapOfObjects.myKey.name", "map-target")));

		Assertions.assertNotNull(results);
		Assertions.assertFalse(results.isEmpty());
		for (final DeepNestedModel result : results) {
			Assertions.assertNotNull(result.mapOfObjects);
			Assertions.assertNotNull(result.mapOfObjects.get("myKey"));
			Assertions.assertEquals("map-target", result.mapOfObjects.get("myKey").name);
		}
		final boolean found = results.stream().anyMatch(r -> r.getOid().equals(inserted1.getOid()));
		Assertions.assertTrue(found, "Expected to find entry1 in map value query results");
	}

	@Order(23)
	@Test
	public void testQuerySubFieldNumericComparison() throws Exception {
		LOGGER.info("Test query on sub-field with numeric comparison: simpleObject.count > N");
		final DeepNestedModel entry1 = new DeepNestedModel();
		entry1.simpleObject = createComplexSubObject("high", 1000, true, Enum2ForTest.ENUM_VALUE_1, List.of());

		final DeepNestedModel entry2 = new DeepNestedModel();
		entry2.simpleObject = createComplexSubObject("low", 1, true, Enum2ForTest.ENUM_VALUE_1, List.of());

		final DeepNestedModel inserted1 = ConfigureDb.da.insert(entry1);
		ConfigureDb.da.insert(entry2);

		// Query where simpleObject.count > 500
		final List<DeepNestedModel> results = ConfigureDb.da.gets(DeepNestedModel.class,
				new Condition(Filters.gt("simpleObject.count", 500)));

		Assertions.assertNotNull(results);
		Assertions.assertFalse(results.isEmpty());
		for (final DeepNestedModel result : results) {
			Assertions.assertNotNull(result.simpleObject);
			Assertions.assertTrue(result.simpleObject.count > 500);
		}
		final boolean found = results.stream().anyMatch(r -> r.getOid().equals(inserted1.getOid()));
		Assertions.assertTrue(found, "Expected to find entry1 with count=1000 in results");
	}

	@Order(24)
	@Test
	public void testQuerySubFieldBoolean() throws Exception {
		LOGGER.info("Test query on sub-field boolean: simpleObject.active");
		final DeepNestedModel entry1 = new DeepNestedModel();
		entry1.simpleObject = createComplexSubObject("active-entry", 1, true, Enum2ForTest.ENUM_VALUE_1, List.of());

		final DeepNestedModel entry2 = new DeepNestedModel();
		entry2.simpleObject = createComplexSubObject("inactive-entry", 2, false, Enum2ForTest.ENUM_VALUE_2, List.of());

		ConfigureDb.da.insert(entry1);
		ConfigureDb.da.insert(entry2);

		// Query where simpleObject.active == false
		final List<DeepNestedModel> results = ConfigureDb.da.gets(DeepNestedModel.class,
				new Condition(Filters.eq("simpleObject.active", false)));

		Assertions.assertNotNull(results);
		Assertions.assertFalse(results.isEmpty());
		for (final DeepNestedModel result : results) {
			Assertions.assertNotNull(result.simpleObject);
			Assertions.assertEquals(false, result.simpleObject.active);
		}
	}

}
