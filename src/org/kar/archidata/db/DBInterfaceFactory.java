package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBInterfaceFactory implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DBInterfaceFactory.class);
	private final DBConfig config;
	private DbInterface ioDb;
	private Class<?> classes[] = {};
	private static List<DBInterfaceFactory> stored = new ArrayList<>();

	private DBInterfaceFactory(final DBConfig config, final Class<?>... classes) throws IOException {
		this.config = config;
		this.classes = classes;
		connect();
	}

	public static DBInterfaceFactory create(final DBConfig config, final Class<?>... classes) throws IOException {
		if (config.getKeepConnected()) {
			for (final DBInterfaceFactory elem : stored) {
				if (elem == null) {
					continue;
				}
				if (elem.config.getUrl().equals(config.getUrl())) {
					return elem;
				}
			}
			final DBInterfaceFactory tmp = new DBInterfaceFactory(config);
			stored.add(tmp);
			return tmp;
		} else {
			return new DBInterfaceFactory(config, classes);
		}
	}

	public void connect() throws IOException {
		if ("mysql".equals(this.config.getType())) {
			this.ioDb = new DbInterfaceSQL(this.config);
		} else if ("sqlite".equals(this.config.getType())) {
			this.ioDb = new DbInterfaceSQL(this.config);
		} else if ("mongo".equals(this.config.getType())) {
			this.ioDb = new DbInterfaceMorphia(this.config, this.classes);
		} else {
			throw new IOException("DB type: '" + this.config.getType() + "'is not managed");
		}
	}

	@Override
	public void close() throws IOException {
		if (this.config.getKeepConnected()) {
			return;
		}
		closeForce();
	}

	public void closeForce() throws IOException {
		this.ioDb.close();
	}

	public DbInterface getDbInterface() {
		return this.ioDb;
	}

	public static void closeAllForceMode() throws IOException {
		for (final DBInterfaceFactory entry : stored) {
			entry.closeForce();
		}
		stored = new ArrayList<>();
	}
}
