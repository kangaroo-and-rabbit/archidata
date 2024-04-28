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
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.model.SerializeAsJson;
import test.kar.archidata.model.SimpleTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestJson.class);

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
	public void testTableInsertAndRetrieve() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(SerializeAsJson.class);
		for (final String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			DataAccess.executeSimpleQuery(elem);
		}
	}

	@Order(2)
	@Test
	public void testIO() throws Exception {
		final SerializeAsJson test = new SerializeAsJson();
		test.data = new SimpleTable();
		test.data.data = "plopppopql";

		final SerializeAsJson insertedData = DataAccess.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertNotNull(insertedData.data.data);
		Assertions.assertEquals(test.data.data, insertedData.data.data);

		// Try to retrieve all the data:
		final SerializeAsJson retrieve = DataAccess.get(SerializeAsJson.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertTrue(retrieve.id >= 0);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertNotNull(retrieve.data.data);
		Assertions.assertEquals(test.data.data, retrieve.data.data);
	}

}
