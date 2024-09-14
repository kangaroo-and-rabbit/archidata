package test.kar.archidata;

import java.io.IOException;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigureDb {
	final static private Logger LOGGER = LoggerFactory.getLogger(ConfigureDb.class);

	public static void configure() throws IOException {
		if (true) {
			if (!"true".equalsIgnoreCase(System.getenv("TEST_E2E_MODE"))) {
				ConfigBaseVariable.dbType = "sqlite";
				ConfigBaseVariable.dbHost = "memory";
				// for test we need to connect all time the DB
				ConfigBaseVariable.dbKeepConnected = "true";
			}
		} else {
			// Enable this if you want to access to a local MySQL base to test with an adminer
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
			//ConfigBaseVariable.dbPassword = "password";
		}
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		entry.connect();
	}

	public static void clear() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();

	}
}
