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
		String modeTest = System.getenv("TEST_E2E_MODE");
		if (modeTest == null || modeTest.isEmpty() || "false".equalsIgnoreCase(modeTest)) {
			modeTest = "SQLITE-MEMORY";
		} else if ("true".equalsIgnoreCase(modeTest)) {
			modeTest = "MY-SQL";
		}
		// override the local test:
		//modeTest = "MONGO";
		if ("SQLITE-MEMORY".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.dbHost = "memory";
			// for test we need to connect all time the DB
			ConfigBaseVariable.dbKeepConnected = "true";
		} else if ("SQLITE".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.dbKeepConnected = "true";
		} else if ("MY-SQL".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "mysql";
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
		} else if ("MONGO".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "mongo";
			ConfigBaseVariable.bdDatabase = "test_mongo_db";
		} else {
			// User local modification ...
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
		}
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.getDbconfig());
		entry.connect();
	}

	public static void clear() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();

	}
}
