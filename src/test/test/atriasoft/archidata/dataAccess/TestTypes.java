package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import org.atriasoft.archidata.dataAccess.options.FilterValue;
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
import test.atriasoft.archidata.dataAccess.model.TypesTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTypes {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTypes.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(2)
	@Test
	public void testInteger() throws Exception {

		final TypesTable test = new TypesTable();
		test.intData = 95;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.intData);
		Assertions.assertEquals(insertedData.intData, retrieve.intData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(3)
	@Test
	public void testLong() throws Exception {

		final TypesTable test = new TypesTable();
		test.longData = 541684354354L;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.longData);
		Assertions.assertEquals(insertedData.longData, retrieve.longData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(4)
	@Test
	public void testfloat() throws Exception {

		final TypesTable test = new TypesTable();
		test.floatData = 153154.0f;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.floatData);
		Assertions.assertEquals(insertedData.floatData, retrieve.floatData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(5)
	@Test
	public void testDouble() throws Exception {

		final TypesTable test = new TypesTable();
		test.doubleData = 153152654654.0;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.doubleData);
		Assertions.assertEquals(insertedData.doubleData, retrieve.doubleData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(6)
	@Test
	public void testText() throws Exception {

		final TypesTable test = new TypesTable();
		test.textData = "lkjlkjlkjmlkqjsdùkljqsùmckljvùwxmckvmwlkdnfqmsjdvnmclkwsjdn;vbcm <wkdjncvm<wk:dnxcm<lwkdnc mqs<wdn:cx,<nm wlx!k:cn<;wmlx:!c;,<wmlx!:c;n<wm ldx:;c,<nwmlx:c,;<wmlx!:c;,< w";
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.textData);
		Assertions.assertEquals(insertedData.textData, retrieve.textData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(7)
	@Test
	public void testVarChar() throws Exception {

		final TypesTable test = new TypesTable();
		test.varcharData = "123456789123456789";
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.varcharData);
		Assertions.assertEquals(insertedData.varcharData, retrieve.varcharData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(8)
	@Test
	public void testBooleanTrue() throws Exception {

		final TypesTable test = new TypesTable();
		test.booleanData = true;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.booleanData);
		Assertions.assertEquals(insertedData.booleanData, retrieve.booleanData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(9)
	@Test
	public void testBooleanFalse() throws Exception {

		final TypesTable test = new TypesTable();
		test.booleanData = false;
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.booleanData);
		Assertions.assertEquals(insertedData.booleanData, retrieve.booleanData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(11)
	@Test
	public void testDate() throws Exception {

		final TypesTable test = new TypesTable();
		test.dateFullData = Date.from(Instant.now());
		LOGGER.debug("Date = {}", test.dateFullData);
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive Date = {}", retrieve.dateFullData);
		Assertions.assertNotNull(retrieve.dateFullData);
		Assertions.assertEquals(insertedData.dateFullData, retrieve.dateFullData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(12)
	@Test
	public void testLocalDate() throws Exception {

		final TypesTable test = new TypesTable();
		test.dateData = LocalDate.now();
		LOGGER.debug("LocalDate = {}", test.dateData);
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive LocalDate = {}", retrieve.dateData);
		Assertions.assertNotNull(retrieve.dateData);
		Assertions.assertEquals(insertedData.dateData, retrieve.dateData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(13)
	@Test
	public void testLocalTime() throws Exception {

		final TypesTable test = new TypesTable();
		test.timeData = LocalTime.now();
		LOGGER.debug("LocalTime = {}", test.timeData);
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		LOGGER.debug("Retreive LocalTime = {}", retrieve.timeData);
		Assertions.assertNotNull(insertedData.timeData);
		Assertions.assertEquals(insertedData.timeData.getHour(), retrieve.timeData.getHour());
		Assertions.assertEquals(insertedData.timeData.getMinute(), retrieve.timeData.getMinute());
		Assertions.assertEquals(insertedData.timeData.getSecond(), retrieve.timeData.getSecond());

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

	@Order(14)
	@Test
	public void testTextUpdateDirect() throws Exception {

		final TypesTable test = new TypesTable();
		test.textData = "test 1";
		test.booleanData = null;
		test.varcharData = "plop";
		final TypesTable insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesTable retrieve = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

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
		final long nbUpdate = ConfigureDb.da.updateById(retrieve, insertedData.id);
		Assertions.assertEquals(1L, nbUpdate);

		// Get new data
		final TypesTable retrieve2 = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

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
		final long nbUpdate2 = ConfigureDb.da.updateById(retrieve, insertedData.id, new FilterValue("textData"));
		Assertions.assertEquals(1L, nbUpdate2);

		// Get new data
		final TypesTable retrieve3 = ConfigureDb.da.getById(TypesTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve3);
		Assertions.assertNotNull(retrieve3.id);
		Assertions.assertEquals(insertedData.id, retrieve3.id);
		Assertions.assertNotNull(retrieve3.textData);
		Assertions.assertEquals(retrieve.textData, retrieve3.textData);
		Assertions.assertNotNull(retrieve3.booleanData);
		// note: retreive2
		Assertions.assertEquals(retrieve2.booleanData, retrieve3.booleanData);
		Assertions.assertNull(retrieve3.varcharData);

		ConfigureDb.da.deleteById(TypesTable.class, insertedData.id);
	}

}
