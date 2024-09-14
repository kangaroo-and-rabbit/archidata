package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBEntry implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DBEntry.class);
	public DBConfig config;
	public Connection connection;
	private static List<DBEntry> stored = new ArrayList<>();

	private DBEntry(final DBConfig config, final boolean root) throws IOException {
		this.config = config;
		if (root) {
			connectRoot();
		} else {
			connect();
		}
	}

	public static DBEntry createInterface(final DBConfig config) throws IOException {
		return createInterface(config, false);
	}

	public static DBEntry createInterface(final DBConfig config, final boolean root) throws IOException {
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
			return new DBEntry(config, root);
		}
	}

	public void connectRoot() throws IOException {
		try {
			this.connection = DriverManager.getConnection(this.config.getUrl(true), this.config.getLogin(),
					this.config.getPassword());
		} catch (final SQLException ex) {
			throw new IOException("Connection db fail: " + ex.getMessage() + " On URL: " + this.config.getUrl(true));
		}

	}

	public void connect() throws IOException {
		try {
			this.connection = DriverManager.getConnection(this.config.getUrl(), this.config.getLogin(),
					this.config.getPassword());
		} catch (final SQLException ex) {
			LOGGER.error("Connection db fail: " + ex.getMessage() + " On URL: " + this.config.getUrl(true));
			throw new IOException("Connection db fail: " + ex.getMessage() + " On URL: " + this.config.getUrl(true));
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
		try {
			// connection.commit();
			this.connection.close();
		} catch (final SQLException ex) {
			throw new IOException("Dis-connection db fail: " + ex.getMessage());
		}
	}

	public static void closeAllForceMode() throws IOException {
		for (final DBEntry entry : stored) {
			entry.closeForce();
		}
		stored = new ArrayList<>();
	}
}
