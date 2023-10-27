package test.kar.archidata;

import java.io.IOException;
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
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.model.Enum2ForTest;
import test.kar.archidata.model.TypesEnum2;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTypeEnum2 {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypeEnum2.class);
	
	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigBaseVariable.dbType = "sqlite";
		ConfigBaseVariable.dbHost = "memory";
		// for test we need to connect all time the DB
		ConfigBaseVariable.dbKeepConnected = "true";
		
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
		final List<String> sqlCommand = DataFactory.createTable(TypesEnum2.class);
		for (final String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			DataAccess.executeSimpleQuerry(elem, false);
		}
	}

	@Order(2)
	@Test
	public void testEnum() throws Exception {

		final TypesEnum2 test = new TypesEnum2();
		test.data = Enum2ForTest.ENUM_VALUE_4;
		final TypesEnum2 insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		
		// Try to retrieve all the data:
		TypesEnum2 retrieve = DataAccess.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(insertedData.data, retrieve.data);

		// Update data to null
		retrieve.data = null;
		int ret = DataAccess.update(retrieve, retrieve.id);
		Assertions.assertEquals(1, ret);
		
		// get new data
		retrieve = DataAccess.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNull(retrieve.data);
		
		// Remove the data
		ret = DataAccess.delete(TypesEnum2.class, insertedData.id);
		Assertions.assertEquals(1, ret);
		
		// Get the removed data:
		retrieve = DataAccess.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNull(retrieve);
	}

	@Order(3)
	@Test
	public void testNull() throws Exception {

		final TypesEnum2 test = new TypesEnum2();
		test.data = null;
		final TypesEnum2 insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		
		// Try to retrieve all the data:
		final TypesEnum2 retrieve = DataAccess.get(TypesEnum2.class, insertedData.id);
		
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNull(retrieve.data);

		DataAccess.delete(TypesEnum2.class, insertedData.id);
	}
}