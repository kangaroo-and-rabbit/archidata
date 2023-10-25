package test.kar.archidata;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBase {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestBase.class);
	
	@BeforeAll
	public static void configureWebServer() throws Exception {
		/*LOGGER.info("Create DB");
		final String dbName = "sdfsdfsdfsfsdfsfsfsfsdfsdfsd";
		boolean data = SqlWrapper.isDBExist(dbName);
		LOGGER.error("exist: {}", data);
		data = SqlWrapper.createDB(dbName);
		LOGGER.error("create: {}", data);
		data = SqlWrapper.isDBExist(dbName);
		LOGGER.error("exist: {}", data);
		*/
		ConfigBaseVariable.dbType = "sqlite";
		ConfigBaseVariable.dbHost = "memory";
		// for test we need to connect all time the DB
		ConfigBaseVariable.dbKeepConnected = "true";
		
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();
	}
	
	@Order(1)
	@Test
	public void checkSimpleTestError() throws Exception {
		Assertions.assertEquals("lkjlkjlkjlk", "alive and kicking");
	}
}
