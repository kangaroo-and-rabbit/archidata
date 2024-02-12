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

import test.kar.archidata.model.TypesTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestRawQuery {
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
	public void testGet() throws Exception {

		final TypesTable test = new TypesTable();
		test.intData = 95;
		test.floatData = 1.0F;
		DataAccess.insert(test);
		test.intData = 96;
		test.floatData = 2.0F;
		DataAccess.insert(test);
		test.intData = 97;
		test.floatData = 3.0F;
		DataAccess.insert(test);
		test.intData = 98;
		test.floatData = 4.0F;
		DataAccess.insert(test);
		test.intData = 99;
		test.floatData = 5.0F;
		DataAccess.insert(test);
		test.intData = 99;
		test.floatData = 6.0F;
		DataAccess.insert(test);
		test.intData = 99;
		test.floatData = 7.0F;
		DataAccess.insert(test);
		{
			String querry = """
					SELECT *
					FROM TypesTable
					WHERE `intData` = ?
					ORDER BY id DESC
					""";
			List<Object> parameters = List.of(Integer.valueOf(99));
			// Try to retrieve all the data:
			final List<TypesTable> retrieve = DataAccess.query(TypesTable.class, querry, parameters);

			Assertions.assertNotNull(retrieve);
			Assertions.assertEquals(3, retrieve.size());
			Assertions.assertEquals(99, retrieve.get(0).intData);
			Assertions.assertEquals(7.0F, retrieve.get(0).floatData);
			Assertions.assertEquals(6.0F, retrieve.get(1).floatData);
			Assertions.assertEquals(5.0F, retrieve.get(2).floatData);
		}
		{

			String querry = """
					SELECT DISTINCT intData
					FROM TypesTable
					WHERE `intData` = ?
					ORDER BY id DESC
					""";
			List<Object> parameters = List.of(Integer.valueOf(99));
			// Try to retrieve all the data:
			final List<TypesTable> retrieve = DataAccess.query(TypesTable.class, querry, parameters);

			Assertions.assertNotNull(retrieve);
			Assertions.assertEquals(1, retrieve.size());
			Assertions.assertEquals(99, retrieve.get(0).intData);
		}
	}

}