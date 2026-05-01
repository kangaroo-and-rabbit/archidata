package test.atriasoft.archidata;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;

public class ConfigureDb {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDb.class);
	public static DBAccessMongo da = null;

	public static void configure() throws IOException, InternalServerErrorException, DataAccessException {
		ConfigBaseVariable.setBdDatabase("test_db");
		ConfigBaseVariable.setDbPassword("base_db_password");
		removeDB();
		da = DBAccessMongo.createInterface();
	}

	public static void removeDB() {
		DbConfig config = null;
		try {
			config = new DbConfig();
		} catch (final DataAccessException e) {
			LOGGER.error("Fail to clean the DB", e);
			return;
		}
		LOGGER.info("Remove the DB and create a new one '{}'", config.getDbName());
		final int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try (final DBAccessMongo daRoot = DBAccessMongo.createInterface(config)) {
				daRoot.deleteDatabase(ConfigBaseVariable.getDBName());
				return;
			} catch (final RuntimeException e) {
				if (attempt == maxAttempts) {
					LOGGER.error("Fail to clean the DB after {} attempts", maxAttempts, e);
					return;
				}
				LOGGER.warn("DB cleanup attempt {} failed: {} -- retrying", attempt, e.getMessage());
				try {
					Thread.sleep(100L * attempt);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
			} catch (final IOException e) {
				LOGGER.error("Fail to clean the DB", e);
				return;
			}
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
