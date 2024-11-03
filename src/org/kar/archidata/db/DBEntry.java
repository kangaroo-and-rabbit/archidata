package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBEntry implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DBEntry.class);
	private final DBConfig config;
	private DbInterface ioDb;
	private Class<?> classes[] = {};
	private static List<DBEntry> stored = new ArrayList<>();

	private DBEntry(final DBConfig config, final boolean root, final Class<?>... classes) throws IOException {
		this.config = config;
		this.classes = classes;
		if (root) {
			connectRoot();
		} else {
			connect();
		}
	}

	public static DBEntry createInterface(final DBConfig config, final Class<?>... classes) throws IOException {
		return createInterface(config, false, classes);
	}

	public static DBEntry createInterface(final DBConfig config, final boolean root, final Class<?>... classes)
			throws IOException {
		if (config.getKeepConnected()) {
			for (final DBEntry elem : stored) {
				if (elem == null) {
					continue;
				}
				if (elem.config.getUrl().equals(config.getUrl())) {
					return elem;
				}
			}
			final DBEntry tmp = new DBEntry(config, root);
			stored.add(tmp);
			return tmp;
		} else {
			return new DBEntry(config, root, classes);
		}
	}

	public void connectRoot() throws IOException {
		// TODO: maybe better check for root connection ...
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
		for (final DBEntry entry : stored) {
			entry.closeForce();
		}
		stored = new ArrayList<>();
	}
}
