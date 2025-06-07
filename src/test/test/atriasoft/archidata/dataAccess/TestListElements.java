package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
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
import test.atriasoft.archidata.dataAccess.model.DataInJson;
import test.atriasoft.archidata.dataAccess.model.DataWithSubJsonList;
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestListElements {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestListElements.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testTableInsertAndRetrieve() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(DataWithSubJsonList.class);
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testListInteger() throws Exception {
		final DataWithSubJsonList test = new DataWithSubJsonList();
		test.listIntegerData = new ArrayList<>();
		test.listIntegerData.add(5);
		test.listIntegerData.add(2);
		test.listIntegerData.add(8);
		test.listIntegerData.add(6);
		test.listIntegerData.add(51);

		final DataWithSubJsonList insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.listIntegerData);
		Assertions.assertEquals(5, insertedData.listIntegerData.size());
		Assertions.assertEquals(test.listIntegerData.get(0), insertedData.listIntegerData.get(0));
		Assertions.assertEquals(test.listIntegerData.get(1), insertedData.listIntegerData.get(1));
		Assertions.assertEquals(test.listIntegerData.get(2), insertedData.listIntegerData.get(2));
		Assertions.assertEquals(test.listIntegerData.get(3), insertedData.listIntegerData.get(3));
		Assertions.assertEquals(test.listIntegerData.get(4), insertedData.listIntegerData.get(4));

		// Try to retrieve all the data:
		final DataWithSubJsonList retrieve = ConfigureDb.da.get(DataWithSubJsonList.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.listIntegerData);
		Assertions.assertEquals(5, retrieve.listIntegerData.size());
		Assertions.assertEquals(test.listIntegerData.get(0), retrieve.listIntegerData.get(0));
		Assertions.assertEquals(test.listIntegerData.get(1), retrieve.listIntegerData.get(1));
		Assertions.assertEquals(test.listIntegerData.get(2), retrieve.listIntegerData.get(2));
		Assertions.assertEquals(test.listIntegerData.get(3), retrieve.listIntegerData.get(3));
		Assertions.assertEquals(test.listIntegerData.get(4), retrieve.listIntegerData.get(4));
	}

	@Order(2)
	@Test
	public void testListLong() throws Exception {
		final DataWithSubJsonList test = new DataWithSubJsonList();
		test.listLongData = new ArrayList<>();
		test.listLongData.add(5L);
		test.listLongData.add(2L);
		test.listLongData.add(8L);
		test.listLongData.add(6L);
		test.listLongData.add(51L);

		final DataWithSubJsonList insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.listLongData);
		Assertions.assertEquals(5, insertedData.listLongData.size());
		Assertions.assertEquals(test.listLongData.get(0), insertedData.listLongData.get(0));
		Assertions.assertEquals(test.listLongData.get(1), insertedData.listLongData.get(1));
		Assertions.assertEquals(test.listLongData.get(2), insertedData.listLongData.get(2));
		Assertions.assertEquals(test.listLongData.get(3), insertedData.listLongData.get(3));
		Assertions.assertEquals(test.listLongData.get(4), insertedData.listLongData.get(4));

		// Try to retrieve all the data:
		final DataWithSubJsonList retrieve = ConfigureDb.da.get(DataWithSubJsonList.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.listLongData);
		Assertions.assertEquals(5, retrieve.listLongData.size());
		Assertions.assertEquals(test.listLongData.get(0), retrieve.listLongData.get(0));
		Assertions.assertEquals(test.listLongData.get(1), retrieve.listLongData.get(1));
		Assertions.assertEquals(test.listLongData.get(2), retrieve.listLongData.get(2));
		Assertions.assertEquals(test.listLongData.get(3), retrieve.listLongData.get(3));
		Assertions.assertEquals(test.listLongData.get(4), retrieve.listLongData.get(4));
	}

	@Order(2)
	@Test
	public void testListObject() throws Exception {
		final DataWithSubJsonList test = new DataWithSubJsonList();
		test.listObjectData = new ArrayList<>();
		test.listObjectData.add(new DataInJson("5L"));
		test.listObjectData.add(new DataInJson("2L"));
		test.listObjectData.add(new DataInJson("8L"));
		test.listObjectData.add(new DataInJson("6L"));
		test.listObjectData.add(new DataInJson("51L"));

		final DataWithSubJsonList insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.listObjectData);
		Assertions.assertEquals(5, insertedData.listObjectData.size());
		Assertions.assertEquals(test.listObjectData.get(0), insertedData.listObjectData.get(0));
		Assertions.assertEquals(test.listObjectData.get(1), insertedData.listObjectData.get(1));
		Assertions.assertEquals(test.listObjectData.get(2), insertedData.listObjectData.get(2));
		Assertions.assertEquals(test.listObjectData.get(3), insertedData.listObjectData.get(3));
		Assertions.assertEquals(test.listObjectData.get(4), insertedData.listObjectData.get(4));

		// Try to retrieve all the data:
		final DataWithSubJsonList retrieve = ConfigureDb.da.get(DataWithSubJsonList.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.listObjectData);
		Assertions.assertEquals(5, retrieve.listObjectData.size());
		Assertions.assertEquals(test.listObjectData.get(0), retrieve.listObjectData.get(0));
		Assertions.assertEquals(test.listObjectData.get(1), retrieve.listObjectData.get(1));
		Assertions.assertEquals(test.listObjectData.get(2), retrieve.listObjectData.get(2));
		Assertions.assertEquals(test.listObjectData.get(3), retrieve.listObjectData.get(3));
		Assertions.assertEquals(test.listObjectData.get(4), retrieve.listObjectData.get(4));
	}

	@Order(2)
	@Test
	public void testListEnumData() throws Exception {
		final DataWithSubJsonList test = new DataWithSubJsonList();
		test.listEnumData = new ArrayList<>();
		test.listEnumData.add(Enum2ForTest.ENUM_VALUE_1);
		test.listEnumData.add(Enum2ForTest.ENUM_VALUE_4);
		test.listEnumData.add(Enum2ForTest.ENUM_VALUE_5);
		test.listEnumData.add(Enum2ForTest.ENUM_VALUE_2);
		test.listEnumData.add(Enum2ForTest.ENUM_VALUE_3);

		final DataWithSubJsonList insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.listEnumData);
		Assertions.assertEquals(5, insertedData.listEnumData.size());
		Assertions.assertEquals(test.listEnumData.get(0), insertedData.listEnumData.get(0));
		Assertions.assertEquals(test.listEnumData.get(1), insertedData.listEnumData.get(1));
		Assertions.assertEquals(test.listEnumData.get(2), insertedData.listEnumData.get(2));
		Assertions.assertEquals(test.listEnumData.get(3), insertedData.listEnumData.get(3));
		Assertions.assertEquals(test.listEnumData.get(4), insertedData.listEnumData.get(4));

		// Try to retrieve all the data:
		final DataWithSubJsonList retrieve = ConfigureDb.da.get(DataWithSubJsonList.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.listEnumData);
		Assertions.assertEquals(5, retrieve.listEnumData.size());
		Assertions.assertEquals(test.listEnumData.get(0), retrieve.listEnumData.get(0));
		Assertions.assertEquals(test.listEnumData.get(1), retrieve.listEnumData.get(1));
		Assertions.assertEquals(test.listEnumData.get(2), retrieve.listEnumData.get(2));
		Assertions.assertEquals(test.listEnumData.get(3), retrieve.listEnumData.get(3));
		Assertions.assertEquals(test.listEnumData.get(4), retrieve.listEnumData.get(4));
	}

}
