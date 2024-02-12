package test.kar.archidata;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.model.TypesTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTypes {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypes.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		if (!"true".equalsIgnoreCase(System.getenv("TEST_E2E_MODE"))) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.dbHost = "memory";
			// for test we need to connect all time the DB
			ConfigBaseVariable.dbKeepConnected = "true";
		}
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		entry.connect();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypesTable.class);
		for (final String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			DataAccess.executeSimpleQuerry(elem);
		}
	}

	@Order(2)
	@Test
	public void testInteger() throws Exception {

		final TypesTable test = new TypesTable();
		test.intData = 95;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.intData);
		Assertions.assertEquals(insertedData.intData, retrieve.intData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(3)
	@Test
	public void testLong() throws Exception {

		final TypesTable test = new TypesTable();
		test.longData = 541684354354L;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.longData);
		Assertions.assertEquals(insertedData.longData, retrieve.longData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(4)
	@Test
	public void testfloat() throws Exception {

		final TypesTable test = new TypesTable();
		test.floatData = 153154.0f;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.floatData);
		Assertions.assertEquals(insertedData.floatData, retrieve.floatData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(5)
	@Test
	public void testDouble() throws Exception {

		final TypesTable test = new TypesTable();
		test.doubleData = 153152654654.0;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.doubleData);
		Assertions.assertEquals(insertedData.doubleData, retrieve.doubleData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(6)
	@Test
	public void testText() throws Exception {

		final TypesTable test = new TypesTable();
		test.textData = "lkjlkjlkjmlkqjsdùkljqsùmckljvùwxmckvmwlkdnfqmsjdvnmclkwsjdn;vbcm <wkdjncvm<wk:dnxcm<lwkdnc mqs<wdn:cx,<nm wlx!k:cn<;wmlx:!c;,<wmlx!:c;n<wm ldx:;c,<nwmlx:c,;<wmlx!:c;,< w";
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.textData);
		Assertions.assertEquals(insertedData.textData, retrieve.textData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(7)
	@Test
	public void testVarChar() throws Exception {

		final TypesTable test = new TypesTable();
		test.varcharData = "123456789123456789";
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.varcharData);
		Assertions.assertEquals(insertedData.varcharData, retrieve.varcharData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(8)
	@Test
	public void testBooleanTrue() throws Exception {

		final TypesTable test = new TypesTable();
		test.booleanData = true;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.booleanData);
		Assertions.assertEquals(insertedData.booleanData, retrieve.booleanData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(9)
	@Test
	public void testBooleanFalse() throws Exception {

		final TypesTable test = new TypesTable();
		test.booleanData = false;
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.booleanData);
		Assertions.assertEquals(insertedData.booleanData, retrieve.booleanData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(10)
	@Test
	public void testTimeStamp() throws Exception {

		final TypesTable test = new TypesTable();
		test.timeStampData = Timestamp.from(Instant.now());
		LOGGER.debug("Timestamp = {}", test.timeStampData);
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive Timestamp = {}", retrieve.timeStampData);
		Assertions.assertNotNull(retrieve.timeStampData);
		// Can not compare the exact timestamp due to aproximation and model of storing data :
		// Assertions.assertEquals(insertedData.timeStampData, retrieve.timeStampData);
		Assertions.assertEquals(insertedData.timeStampData.toInstant().toEpochMilli(), retrieve.timeStampData.toInstant().toEpochMilli());

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(11)
	@Test
	public void testDate() throws Exception {

		final TypesTable test = new TypesTable();
		test.dateFullData = Date.from(Instant.now());
		LOGGER.debug("Date = {}", test.dateFullData);
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive Date = {}", retrieve.dateFullData);
		Assertions.assertNotNull(retrieve.dateFullData);
		Assertions.assertEquals(insertedData.dateFullData, retrieve.dateFullData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(12)
	@Test
	public void testLocalDate() throws Exception {

		final TypesTable test = new TypesTable();
		test.dateData = LocalDate.now();
		LOGGER.debug("LocalDate = {}", test.dateData);
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive LocalDate = {}", retrieve.dateData);
		Assertions.assertNotNull(retrieve.dateData);
		Assertions.assertEquals(insertedData.dateData, retrieve.dateData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(13)
	@Test
	public void testLocalTime() throws Exception {

		final TypesTable test = new TypesTable();
		test.timeData = LocalTime.now();
		LOGGER.debug("LocalTime = {}", test.timeData);
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive LocalTime = {}", retrieve.timeData);
		Assertions.assertNotNull(insertedData.timeData);
		Assertions.assertEquals(insertedData.timeData.getHour(), retrieve.timeData.getHour());
		Assertions.assertEquals(insertedData.timeData.getMinute(), retrieve.timeData.getMinute());
		Assertions.assertEquals(insertedData.timeData.getSecond(), retrieve.timeData.getSecond());

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(14)
	@Test
	public void testTextUpdateDirect() throws Exception {

		final TypesTable test = new TypesTable();
		test.textData = "test 1";
		test.booleanData = null;
		test.varcharData = "plop";
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.textData);
		Assertions.assertEquals(insertedData.textData, retrieve.textData);
		Assertions.assertNull(retrieve.booleanData);
		Assertions.assertNotNull(retrieve.varcharData);
		Assertions.assertEquals(insertedData.varcharData, retrieve.varcharData);

		// Update the text value:
		retrieve.textData = "test 2";
		retrieve.booleanData = true;
		retrieve.varcharData = null;
		final int nbUpdate = DataAccess.update(retrieve, insertedData.id);
		Assertions.assertEquals(1, nbUpdate);

		// Get new data
		final TypesTable retrieve2 = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.id);
		Assertions.assertEquals(insertedData.id, retrieve2.id);
		Assertions.assertNotNull(retrieve2.textData);
		Assertions.assertEquals(retrieve.textData, retrieve2.textData);
		Assertions.assertNotNull(retrieve2.booleanData);
		Assertions.assertEquals(retrieve.booleanData, retrieve2.booleanData);
		Assertions.assertNull(retrieve2.varcharData);

		// test filter values:
		retrieve.textData = "test 3";
		retrieve.booleanData = false;
		retrieve.varcharData = "test3";
		final int nbUpdate2 = DataAccess.update(retrieve, insertedData.id, List.of("textData"));
		Assertions.assertEquals(1, nbUpdate2);

		// Get new data
		final TypesTable retrieve3 = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve3);
		Assertions.assertNotNull(retrieve3.id);
		Assertions.assertEquals(insertedData.id, retrieve3.id);
		Assertions.assertNotNull(retrieve3.textData);
		Assertions.assertEquals(retrieve.textData, retrieve3.textData);
		Assertions.assertNotNull(retrieve3.booleanData);
		// note: retreive2
		Assertions.assertEquals(retrieve2.booleanData, retrieve3.booleanData);
		Assertions.assertNull(retrieve3.varcharData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

	@Order(15)
	@Test
	public void testTextUpdateJson() throws Exception {

		final TypesTable test = new TypesTable();
		test.textData = "test 1";
		test.booleanData = null;
		test.varcharData = "plop";
		final TypesTable insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.textData);
		Assertions.assertEquals(insertedData.textData, retrieve.textData);
		Assertions.assertNull(retrieve.booleanData);
		Assertions.assertNotNull(retrieve.varcharData);
		Assertions.assertEquals(insertedData.varcharData, retrieve.varcharData);

		// Update the text value:
		final String jsonData = """
				{
					"textData": "test 2",
					"booleanData": true,
					"varcharData": null
				}
				""";
		final int nbUpdate = DataAccess.updateWithJson(TypesTable.class, insertedData.id, jsonData, null);
		Assertions.assertEquals(1, nbUpdate);

		// Get new data
		final TypesTable retrieve2 = DataAccess.get(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.id);
		Assertions.assertEquals(insertedData.id, retrieve2.id);
		Assertions.assertNotNull(retrieve2.textData);
		Assertions.assertEquals("test 2", retrieve2.textData);
		Assertions.assertNotNull(retrieve2.booleanData);
		Assertions.assertEquals(true, retrieve2.booleanData);
		Assertions.assertNull(retrieve2.varcharData);

		DataAccess.delete(TypesTable.class, insertedData.id);
	}

}