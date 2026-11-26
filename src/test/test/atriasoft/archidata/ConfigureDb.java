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

	/**
	 * Drops the test database. A failure is thrown: the next class would otherwise run on the data of the previous
	 * one and its failures would point at the wrong cause.
	 *
	 * @throws IllegalStateException when the database cannot be dropped
	 */
	public static void removeDB() {
		final DbConfig config;
		try {
			config = new DbConfig();
		} catch (final DataAccessException e) {
			throw new IllegalStateException("Fail to clean the DB: the configuration is not readable", e);
		}
		LOGGER.info("Remove the DB and create a new one '{}'", config.getDbName());
		final int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try (final DBAccessMongo daRoot = DBAccessMongo.createInterface(config)) {
				daRoot.deleteDatabase(ConfigBaseVariable.getDBName());
				return;
			} catch (final RuntimeException e) {
				if (attempt == maxAttempts) {
					throw new IllegalStateException("Fail to clean the DB after " + maxAttempts + " attempts", e);
				}
				LOGGER.warn("DB cleanup attempt {} failed: {} -- retrying", attempt, e.getMessage());
				try {
					Thread.sleep(100L * attempt);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Interrupted while cleaning the DB", ie);
				}
			} catch (final IOException e) {
				throw new IllegalStateException("Fail to clean the DB", e);
			}
		}
	}

	public static void clear() throws IOException {
		LOGGER.info("Remove the test db");
		try {
			removeDB();
		} finally {
			// The connection is by default open ==> close it at the end of test, even if the drop failed:
			da.close();
			DbIoFactory.closeAllForceMode();
			ConfigBaseVariable.clearAllValue();
			DBAccessMongo.statistic.display();
		}
	}
}
