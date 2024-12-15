package org.kar.archidata.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbIoFactory {
	final static Logger LOGGER = LoggerFactory.getLogger(DbIoFactory.class);
	private static List<DbIo> stored = new ArrayList<>();

	private DbIoFactory() throws IOException {}

	public static DbIo create() throws IOException, DataAccessException {
		// Find the global configuration of the system.
		return create(new DbConfig());
	}

	public static DbIo create(final DbConfig config) throws IOException {
		for (final DbIo elem : stored) {
			if (elem == null) {
				continue;
			}
			if (elem.compatible(config)) {
				elem.open();
				return elem;
			}
		}
		final DbIo tmp = createInstance(config);
		if (config.getKeepConnected()) {
			stored.add(tmp);
		}
		tmp.open();
		return tmp;
	}

	private static DbIo createInstance(final DbConfig config) throws IOException {
		switch (config.getType()) {
			case "mysql":
				return new DbIoSql(config);
			case "sqlite":
				return new DbIoSql(config);
			case "mongo":
				return new DbIoMorphia(config);
		}
		throw new IOException("DB type: '" + config.getType() + "'is not managed");

	}

	public static void close() throws IOException {
		for (final DbIo entry : stored) {
			entry.close();
		}
	}

	public static void closeAllForceMode() throws IOException {
		for (final DbIo entry : stored) {
			entry.closeForce();
		}
		stored = new ArrayList<>();
	}
}
