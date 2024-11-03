package test.kar.archidata;

import java.io.IOException;
import java.util.List;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.dataAccess.model.SerializeAsJson;
import test.kar.archidata.dataAccess.model.SerializeListAsJson;
import test.kar.archidata.dataAccess.model.SimpleTable;
import test.kar.archidata.dataAccess.model.SimpleTableSoftDelete;
import test.kar.archidata.dataAccess.model.TypeManyToManyRemote;
import test.kar.archidata.dataAccess.model.TypeManyToManyRoot;
import test.kar.archidata.dataAccess.model.TypeManyToManyRootExpand;
import test.kar.archidata.dataAccess.model.TypeManyToOneRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneRootExpand;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRootExpand;
import test.kar.archidata.dataAccess.model.TypeOneToManyRemote;
import test.kar.archidata.dataAccess.model.TypeOneToManyRoot;
import test.kar.archidata.dataAccess.model.TypeOneToManyRootExpand;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRemote;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRoot;
import test.kar.archidata.dataAccess.model.TypeOneToManyUUIDRootExpand;
import test.kar.archidata.dataAccess.model.TypesEnum1;
import test.kar.archidata.dataAccess.model.TypesEnum2;
import test.kar.archidata.dataAccess.model.TypesTable;

public class ConfigureDb {
	final static private Logger LOGGER = LoggerFactory.getLogger(ConfigureDb.class);
	final static private String modeTestForced = "MONGO";

	public static void configure() throws IOException {
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
				TypeManyToManyRemote.class, //
				TypeManyToManyRoot.class, //
				TypeManyToManyRootExpand.class, //
				TypeManyToOneRemote.class, //
				TypeManyToOneRoot.class, //
				TypeManyToOneRootExpand.class, //
				TypeManyToOneUUIDRemote.class, //
				TypeManyToOneUUIDRoot.class, //
				TypeManyToOneUUIDRootExpand.class, //
				TypeOneToManyRemote.class, //
				TypeOneToManyRoot.class, //
				TypeOneToManyRootExpand.class, //
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
			ConfigBaseVariable.bdDatabase = "test_mongo_db";
		} else {
			// User local modification ...
			ConfigBaseVariable.bdDatabase = "test_db";
			ConfigBaseVariable.dbPort = "3906";
			ConfigBaseVariable.dbUser = "root";
		}
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.getDbconfig(),
				listObject.toArray(new Class<?>[0]));
		entry.connect();

		removeDB();
	}

	public static void removeDB() {

		final DataAccess da = DataAccess.createInterface();

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
			// nothing to do ...
		} else if ("SQLITE".equalsIgnoreCase(modeTest)) {
			da.deleteDB(ConfigBaseVariable.bdDatabase);
		} else if ("MY-SQL".equalsIgnoreCase(modeTest)) {
			da.deleteDB(ConfigBaseVariable.bdDatabase);
		} else if ("MONGO".equalsIgnoreCase(modeTest)) {
			da.deleteDB(ConfigBaseVariable.bdDatabase);
		} else {}
	}

	public static void clear() throws IOException {
		LOGGER.info("Remove the test db");
		removeDB();
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();

	}
}
