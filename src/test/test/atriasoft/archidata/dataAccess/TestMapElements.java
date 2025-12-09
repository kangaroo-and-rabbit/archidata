package test.atriasoft.archidata.dataAccess;

import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
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
import test.atriasoft.archidata.dataAccess.model.DataInJson;
import test.atriasoft.archidata.dataAccess.model.DataWithSubJsonMap;
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMapElements {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestMapElements.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@Order(2)
	@Test
	public void testListInteger() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapIntegerData = new HashMap<>();
		test.mapIntegerData.put("0", 5);
		test.mapIntegerData.put("1", 2);
		test.mapIntegerData.put("2", 8);
		test.mapIntegerData.put("3", 6);
		test.mapIntegerData.put("4", 51);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapIntegerData);
		Assertions.assertEquals(5, insertedData.mapIntegerData.size());
		Assertions.assertEquals(test.mapIntegerData.get("0"), insertedData.mapIntegerData.get("0"));
		Assertions.assertEquals(test.mapIntegerData.get("1"), insertedData.mapIntegerData.get("1"));
		Assertions.assertEquals(test.mapIntegerData.get("2"), insertedData.mapIntegerData.get("2"));
		Assertions.assertEquals(test.mapIntegerData.get("3"), insertedData.mapIntegerData.get("3"));
		Assertions.assertEquals(test.mapIntegerData.get("4"), insertedData.mapIntegerData.get("4"));

		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapIntegerData);
		Assertions.assertEquals(5, retrieve.mapIntegerData.size());
		Assertions.assertEquals(test.mapIntegerData.get("0"), retrieve.mapIntegerData.get("0"));
		Assertions.assertEquals(test.mapIntegerData.get("1"), retrieve.mapIntegerData.get("1"));
		Assertions.assertEquals(test.mapIntegerData.get("2"), retrieve.mapIntegerData.get("2"));
		Assertions.assertEquals(test.mapIntegerData.get("3"), retrieve.mapIntegerData.get("3"));
		Assertions.assertEquals(test.mapIntegerData.get("4"), retrieve.mapIntegerData.get("4"));
	}

	@Order(2)
	@Test
	public void testListLong() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapLongData = new HashMap<>();
		test.mapLongData.put("0", 5L);
		test.mapLongData.put("1", 2L);
		test.mapLongData.put("2", 8L);
		test.mapLongData.put("3", 6L);
		test.mapLongData.put("4", 51L);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapLongData);
		Assertions.assertEquals(5, insertedData.mapLongData.size());
		Assertions.assertEquals(test.mapLongData.get("0"), insertedData.mapLongData.get("0"));
		Assertions.assertEquals(test.mapLongData.get("1"), insertedData.mapLongData.get("1"));
		Assertions.assertEquals(test.mapLongData.get("2"), insertedData.mapLongData.get("2"));
		Assertions.assertEquals(test.mapLongData.get("3"), insertedData.mapLongData.get("3"));
		Assertions.assertEquals(test.mapLongData.get("4"), insertedData.mapLongData.get("4"));

		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapLongData);
		Assertions.assertEquals(5, retrieve.mapLongData.size());
		Assertions.assertEquals(test.mapLongData.get("0"), retrieve.mapLongData.get("0"));
		Assertions.assertEquals(test.mapLongData.get("1"), retrieve.mapLongData.get("1"));
		Assertions.assertEquals(test.mapLongData.get("2"), retrieve.mapLongData.get("2"));
		Assertions.assertEquals(test.mapLongData.get("3"), retrieve.mapLongData.get("3"));
		Assertions.assertEquals(test.mapLongData.get("4"), retrieve.mapLongData.get("4"));
	}

	@Order(2)
	@Test
	public void testListObject() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapObjectData = new HashMap<>();
		test.mapObjectData.put("0", new DataInJson("5L"));
		test.mapObjectData.put("1", new DataInJson("2L"));
		test.mapObjectData.put("2", new DataInJson("8L"));
		test.mapObjectData.put("3", new DataInJson("6L"));
		test.mapObjectData.put("4", new DataInJson("51L"));

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapObjectData);
		Assertions.assertEquals(5, insertedData.mapObjectData.size());
		Assertions.assertEquals(test.mapObjectData.get("0"), insertedData.mapObjectData.get("0"));
		Assertions.assertEquals(test.mapObjectData.get("1"), insertedData.mapObjectData.get("1"));
		Assertions.assertEquals(test.mapObjectData.get("2"), insertedData.mapObjectData.get("2"));
		Assertions.assertEquals(test.mapObjectData.get("3"), insertedData.mapObjectData.get("3"));
		Assertions.assertEquals(test.mapObjectData.get("4"), insertedData.mapObjectData.get("4"));

		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapObjectData);
		Assertions.assertEquals(5, retrieve.mapObjectData.size());
		Assertions.assertEquals(test.mapObjectData.get("0"), retrieve.mapObjectData.get("0"));
		Assertions.assertEquals(test.mapObjectData.get("1"), retrieve.mapObjectData.get("1"));
		Assertions.assertEquals(test.mapObjectData.get("2"), retrieve.mapObjectData.get("2"));
		Assertions.assertEquals(test.mapObjectData.get("3"), retrieve.mapObjectData.get("3"));
		Assertions.assertEquals(test.mapObjectData.get("4"), retrieve.mapObjectData.get("4"));
	}

	@Order(2)
	@Test
	public void testListEnumData() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapEnumData = new HashMap<>();
		test.mapEnumData.put("0", Enum2ForTest.ENUM_VALUE_1);
		test.mapEnumData.put("1", Enum2ForTest.ENUM_VALUE_4);
		test.mapEnumData.put("2", Enum2ForTest.ENUM_VALUE_5);
		test.mapEnumData.put("3", Enum2ForTest.ENUM_VALUE_2);
		test.mapEnumData.put("4", Enum2ForTest.ENUM_VALUE_3);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapEnumData);
		Assertions.assertEquals(5, insertedData.mapEnumData.size());
		Assertions.assertEquals(test.mapEnumData.get("0"), insertedData.mapEnumData.get("0"));
		Assertions.assertEquals(test.mapEnumData.get("1"), insertedData.mapEnumData.get("1"));
		Assertions.assertEquals(test.mapEnumData.get("2"), insertedData.mapEnumData.get("2"));
		Assertions.assertEquals(test.mapEnumData.get("3"), insertedData.mapEnumData.get("3"));
		Assertions.assertEquals(test.mapEnumData.get("4"), insertedData.mapEnumData.get("4"));

		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapEnumData);
		Assertions.assertEquals(5, retrieve.mapEnumData.size());
		Assertions.assertEquals(test.mapEnumData.get("0"), retrieve.mapEnumData.get("0"));
		Assertions.assertEquals(test.mapEnumData.get("1"), retrieve.mapEnumData.get("1"));
		Assertions.assertEquals(test.mapEnumData.get("2"), retrieve.mapEnumData.get("2"));
		Assertions.assertEquals(test.mapEnumData.get("3"), retrieve.mapEnumData.get("3"));
		Assertions.assertEquals(test.mapEnumData.get("4"), retrieve.mapEnumData.get("4"));
	}

	@Order(2)
	@Test
	public void testMapMapEnumData() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapMapEnumData = new HashMap<>();
		Map<String, Enum2ForTest> subMap = new HashMap<>();
		subMap.put("0", Enum2ForTest.ENUM_VALUE_1);
		subMap.put("1", Enum2ForTest.ENUM_VALUE_4);
		test.mapMapEnumData.put("A", subMap);
		subMap = new HashMap<>();
		subMap.put("2", Enum2ForTest.ENUM_VALUE_5);
		subMap.put("3", Enum2ForTest.ENUM_VALUE_2);
		subMap.put("4", Enum2ForTest.ENUM_VALUE_3);
		test.mapMapEnumData.put("B", subMap);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapMapEnumData);
		Assertions.assertEquals(2, insertedData.mapMapEnumData.size());
		Assertions.assertNotNull(insertedData.mapMapEnumData.get("A"));
		Assertions.assertEquals(2, insertedData.mapMapEnumData.get("A").size());
		Assertions.assertEquals(test.mapMapEnumData.get("A").get("0"), insertedData.mapMapEnumData.get("A").get("0"));
		Assertions.assertEquals(test.mapMapEnumData.get("A").get("1"), insertedData.mapMapEnumData.get("A").get("1"));
		Assertions.assertNotNull(insertedData.mapMapEnumData.get("B"));
		Assertions.assertEquals(3, insertedData.mapMapEnumData.get("B").size());
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("2"), insertedData.mapMapEnumData.get("B").get("2"));
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("3"), insertedData.mapMapEnumData.get("B").get("3"));
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("4"), insertedData.mapMapEnumData.get("B").get("4"));

		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapMapEnumData);
		Assertions.assertEquals(2, retrieve.mapMapEnumData.size());
		Assertions.assertNotNull(retrieve.mapMapEnumData.get("A"));
		Assertions.assertEquals(2, retrieve.mapMapEnumData.get("A").size());
		Assertions.assertEquals(test.mapMapEnumData.get("A").get("0"), retrieve.mapMapEnumData.get("A").get("0"));
		Assertions.assertEquals(test.mapMapEnumData.get("A").get("1"), retrieve.mapMapEnumData.get("A").get("1"));
		Assertions.assertNotNull(retrieve.mapMapEnumData.get("B"));
		Assertions.assertEquals(3, retrieve.mapMapEnumData.get("B").size());
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("2"), retrieve.mapMapEnumData.get("B").get("2"));
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("3"), retrieve.mapMapEnumData.get("B").get("3"));
		Assertions.assertEquals(test.mapMapEnumData.get("B").get("4"), retrieve.mapMapEnumData.get("B").get("4"));
	}

	@Order(2)
	@Test
	public void testMapKeyModifiedObjectData() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapMapKeyModifiedObjectData = new HashMap<>();
		ObjectId firstKey = new ObjectId();
		test.mapMapKeyModifiedObjectData.put(firstKey, -1L);
		ObjectId secondKey = new ObjectId();
		test.mapMapKeyModifiedObjectData.put(secondKey, 256L);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapMapKeyModifiedObjectData);
		Assertions.assertEquals(2, insertedData.mapMapKeyModifiedObjectData.size());
		Assertions.assertEquals(-1L, insertedData.mapMapKeyModifiedObjectData.get(firstKey));
		Assertions.assertEquals(256L, insertedData.mapMapKeyModifiedObjectData.get(secondKey));
		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapMapKeyModifiedObjectData);
		Assertions.assertEquals(2, retrieve.mapMapKeyModifiedObjectData.size());
		Assertions.assertEquals(-1L, retrieve.mapMapKeyModifiedObjectData.get(firstKey));
		Assertions.assertEquals(256L, retrieve.mapMapKeyModifiedObjectData.get(secondKey));
	}

	@Order(2)
	@Test
	public void testMapKeyModifiedIntegerData() throws Exception {
		final DataWithSubJsonMap test = new DataWithSubJsonMap();
		test.mapMapKeyModifiedIntegerData = new HashMap<>();
		test.mapMapKeyModifiedIntegerData.put(56, -1L);
		test.mapMapKeyModifiedIntegerData.put(66, 256L);

		final DataWithSubJsonMap insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.mapMapKeyModifiedIntegerData);
		Assertions.assertEquals(2, insertedData.mapMapKeyModifiedIntegerData.size());
		Assertions.assertEquals(-1L, insertedData.mapMapKeyModifiedIntegerData.get(56));
		Assertions.assertEquals(256L, insertedData.mapMapKeyModifiedIntegerData.get(66));
		// Try to retrieve all the data:
		final DataWithSubJsonMap retrieve = ConfigureDb.da.get(DataWithSubJsonMap.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.mapMapKeyModifiedIntegerData);
		Assertions.assertEquals(2, retrieve.mapMapKeyModifiedIntegerData.size());
		Assertions.assertEquals(-1L, retrieve.mapMapKeyModifiedIntegerData.get(56));
		Assertions.assertEquals(256L, retrieve.mapMapKeyModifiedIntegerData.get(66));
	}
}
