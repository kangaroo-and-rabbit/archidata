package org.atriasoft.archidata.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Factory for creating and managing {@link DbIo} database connection instances.
 *
 * <p>
 * Maintains a pool of existing connections and reuses compatible ones
 * when the configuration matches.
 * </p>
 */
public class DbIoFactory {
	private static List<DbIo> dbIoStored = new ArrayList<>();

	private DbIoFactory() {}

	/**
	 * Creates a new {@link DbIo} instance using the default global configuration.
	 *
	 * @return a new or reused database connection
	 * @throws IOException if the connection cannot be opened
	 * @throws DataAccessException if the default configuration is invalid
	 */
	public static DbIo create() throws IOException, DataAccessException {
		// Find the global configuration of the system.
		return create(new DbConfig());
	}

	/**
	 * Creates a new {@link DbIo} instance for the given configuration, reusing an existing
	 * compatible connection if available. A kept connection ({@code keepConnected}) carries one reference of the
	 * factory, released by {@link #close()}; the caller opens and closes its own, the reuse adds none.
	 *
	 * @param config the database configuration
	 * @return a new or reused database connection
	 * @throws IOException if the connection cannot be opened
	 */
	public static DbIo create(final DbConfig config) throws IOException {
		for (final DbIo dbIo : dbIoStored) {
			if (dbIo.compatible(config)) {
				// the factory already holds its own reference on a kept connection: the caller takes and releases its own
				return dbIo;
			}
		}
		final DbIo dbIo = createInstance(config);
		if (config.getKeepConnected()) {
			dbIo.open();
			dbIoStored.add(dbIo);
		}
		return dbIo;
	}

	private static DbIo createInstance(final DbConfig config) throws IOException {
		return new DbIoMongo(config);

	}

	/**
	 * Closes all stored database connections.
	 *
	 * @throws IOException if closing any connection fails
	 */
	public static void close() throws IOException {
		for (final DbIo dbIo : dbIoStored) {
			dbIo.close();
		}
	}

	/**
	 * Forcefully closes all stored database connections and clears the connection pool.
	 *
	 * @throws IOException if closing any connection fails
	 */
	public static void closeAllForceMode() throws IOException {
		for (final DbIo entry : dbIoStored) {
			entry.closeForce();
		}
		dbIoStored = new ArrayList<>();
	}
}
