package org.atriasoft.archidata.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbIoFactory {
	static final Logger LOGGER = LoggerFactory.getLogger(DbIoFactory.class);
	private static List<DbIo> dbIoStored = new ArrayList<>();

	private DbIoFactory() throws IOException {}

	public static DbIo create() throws IOException, DataAccessException {
		// Find the global configuration of the system.
		return create(new DbConfig());
	}

	public static DbIo create(final DbConfig config) throws IOException {
		for (final DbIo dbIo : dbIoStored) {
			if (dbIo == null) {
				continue;
			}
			if (dbIo.compatible(config)) {
				dbIo.open();
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

	public static void close() throws IOException {
		for (final DbIo dbIo : dbIoStored) {
			dbIo.close();
		}
	}

	public static void closeAllForceMode() throws IOException {
		for (final DbIo entry : dbIoStored) {
			entry.closeForce();
		}
		dbIoStored = new ArrayList<>();
	}
}
