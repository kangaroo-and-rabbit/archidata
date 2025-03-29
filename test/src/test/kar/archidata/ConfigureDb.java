package test.kar.archidata;

import java.io.IOException;
import java.util.List;

import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.db.DbConfig;
import org.kar.archidata.db.DbIoFactory;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import test.kar.archidata.dataAccess.model.SerializeAsJson;
import test.kar.archidata.dataAccess.model.SerializeListAsJson;
import test.kar.archidata.dataAccess.model.SimpleTable;
import test.kar.archidata.dataAccess.model.SimpleTableSoftDelete;
import test.kar.archidata.dataAccess.model.TypeManyToManyLongRemote;
import test.kar.archidata.dataAccess.model.TypeManyToManyLongRoot;
import test.kar.archidata.dataAccess.model.TypeManyToManyLongRootExpand;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRootExpand;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRootExpand;
import test.kar.archidata.dataAccess.model.TypeOneToManyLongRemote;
import test.kar.archidata.dataAccess.model.TypeOneToManyLongRoot;
import test.kar.archidata.dataAccess.model.TypeOneToManyLongRootExpand;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRemote;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRoot;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRootExpand;
import test.kar.archidata.dataAccess.model.TypesEnum1;
import test.kar.archidata.dataAccess.model.TypesEnum2;
import test.kar.archidata.dataAccess.model.TypesTable;

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
		final List<Class<?>> listObject = List.of( //
				SerializeAsJson.class, //
				SerializeListAsJson.class, //
				SimpleTable.class, //
				SimpleTableSoftDelete.class, //
				TypeManyToManyLongRemote.class, //
				TypeManyToManyLongRoot.class, //
				TypeManyToManyLongRootExpand.class, //
				TypeManyToOneLongRemote.class, //
				TypeManyToOneLongRoot.class, //
				TypeManyToOneLongRootExpand.class, //
				TypeManyToOneUUIDRemote.class, //
				TypeManyToOneUUIDRoot.class, //
				TypeManyToOneUUIDRootExpand.class, //
				TypeOneToManyLongRemote.class, //
				TypeOneToManyLongRoot.class, //
				TypeOneToManyLongRootExpand.class, //
				TypeOneToManyUUIDRemote.class, //
				TypeOneToManyUUIDRoot.class, //
				TypeOneToManyUUIDRootExpand.class, //
				TypesEnum1.class, //
				TypesEnum2.class, //
				TypesTable.class);
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
	}
}
