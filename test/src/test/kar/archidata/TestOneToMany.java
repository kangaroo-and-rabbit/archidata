package test.kar.archidata;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
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

import test.kar.archidata.model.TypesTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOneToMany {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestOneToMany.class);

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
		final List<String> sqlCommand = DataFactory.createTable(TypesTable.class);
		for (final String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			DataAccess.executeSimpleQuerry(elem, false);
		}
	}
	
	@Order(2)
	@Test
	public void testPlop() throws Exception {
		
	}
}