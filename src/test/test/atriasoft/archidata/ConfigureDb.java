package test.atriasoft.archidata;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
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
	public static DBAccessMongo da = null;

	public static void configure() throws IOException, InternalServerErrorException, DataAccessException {
		ConfigBaseVariable.bdDatabase = "test_db";
		removeDB();
		// Connect the dataBase...
		da = DBAccessMongo.createInterface();
	}

	public static void removeDB() {
		DbConfig config = null;
		try {
			config = new DbConfig();
		} catch (final DataAccessException e) {
			e.printStackTrace();
			LOGGER.error("Fail to clean the DB");
			return;
		}
		LOGGER.info("Remove the DB and create a new one '{}'", config.getDbName());
		try (final DBAccessMongo daRoot = DBAccessMongo.createInterface(config)) {
			daRoot.deleteDB(ConfigBaseVariable.bdDatabase);
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
