package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbInterfaceSQL extends DbInterface implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DbInterfaceSQL.class);
	
	private final Connection connection;
	
	public DbInterfaceSQL(final DBConfig config, final String dbName, final Class<?>... classes) throws IOException {
		this(config.getUrl(), config.getLogin(), config.getPassword());
	}
	
	public DbInterfaceSQL(final String dbUrl, final String login, final String password) throws IOException {
		try {
			this.connection = DriverManager.getConnection(dbUrl, login, password);
		} catch (final SQLException ex) {
			LOGGER.error("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
			throw new IOException("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
		}
	}
	
	public Connection getConnection() {
		return this.connection;
	}

	@Override
	public void close() throws IOException {
		try {
			this.connection.close();
		} catch (final SQLException ex) {
			throw new IOException("Dis-connection db fail: " + ex.getMessage());
		}
	}
}
