package test.atriasoft.archidata;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;

public class ConfigureDb {
	final static private Logger LOGGER = LoggerFactory.getLogger(ConfigureDb.class);
	final static private String modeTestForced = null;// "MONGO";
	public static DBAccess da = null;

	public static void configure() throws IOException, InternalServerErrorException, DataAccessException {
		String modeTest = System.getenv("TEST_E2E_MODE");
		if (modeTest == null || modeTest.isEmpty() || "false".equalsIgnoreCase(modeTest)) {
			modeTest = "SQLITE-MEMORY";
		} else if ("true".equalsIgnoreCase(modeTest)) {
			modeTest = "MY-SQL";
		}
		// override the local test:
		if (modeTestForced != null) {
			modeTest = modeTestForced;
		}
		if ("SQLITE-MEMORY".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.bdDatabase = null;
			ConfigBaseVariable.dbHost = "memory";
			// for test we need to connect all time the DB
			ConfigBaseVariable.dbKeepConnected = "true";
		} else if ("SQLITE".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.bdDatabase = null;
			ConfigBaseVariable.dbKeepConnected = "true";
		} else if ("MY-SQL".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "mysql";
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
		} else if ("MONGO".equalsIgnoreCase(modeTest)) {
			ConfigBaseVariable.dbType = "mongo";
			ConfigBaseVariable.bdDatabase = "test_db";
		} else {
			// User local modification ...
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
		}
		removeDB();
		// Connect the dataBase...
		da = DBAccess.createInterface();
	}

	public static void removeDB() {
		String modeTest = System.getenv("TEST_E2E_MODE");
		if (modeTest == null || modeTest.isEmpty() || "false".equalsIgnoreCase(modeTest)) {
			modeTest = "SQLITE-MEMORY";
		} else if ("true".equalsIgnoreCase(modeTest)) {
			modeTest = "MY-SQL";
		}
		// override the local test:
		if (modeTestForced != null) {
			modeTest = modeTestForced;
		}
		DbConfig config = null;
		try {
			config = new DbConfig();
		} catch (final DataAccessException e) {
			e.printStackTrace();
			LOGGER.error("Fail to clean the DB");
			return;
		}
		if (!"MONGO".equalsIgnoreCase(modeTest)) {
			config.setDbName(null);
		}
		LOGGER.info("Remove the DB and create a new one '{}'", config.getDbName());
		try (final DBAccess daRoot = DBAccess.createInterface(config)) {
			if ("SQLITE-MEMORY".equalsIgnoreCase(modeTest)) {
				// nothing to do ...
			} else if ("SQLITE".equalsIgnoreCase(modeTest)) {
				daRoot.deleteDB(ConfigBaseVariable.bdDatabase);
			} else if ("MY-SQL".equalsIgnoreCase(modeTest)) {
				daRoot.deleteDB(ConfigBaseVariable.bdDatabase);
			} else if ("MONGO".equalsIgnoreCase(modeTest)) {
				daRoot.deleteDB(ConfigBaseVariable.bdDatabase);
			}
			daRoot.createDB(ConfigBaseVariable.bdDatabase);
		} catch (final InternalServerErrorException e) {
			e.printStackTrace();
			LOGGER.error("Fail to clean the DB");
			return;
		} catch (final IOException e) {
			e.printStackTrace();
			LOGGER.error("Fail to clean the DB");
			return;
		}
	}

	public static void clear() throws IOException {
		LOGGER.info("Remove the test db");
		removeDB();
		// The connection is by default open ==> close it at the end of test:
		da.close();
		DbIoFactory.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();
		DBAccessMongo.statistic.display();
	}
}
