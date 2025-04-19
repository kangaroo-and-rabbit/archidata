package org.atriasoft.archidata.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbIoSql extends DbIo {
	final static Logger LOGGER = LoggerFactory.getLogger(DbIoSql.class);

	private Connection connection = null;

	public DbIoSql(final DbConfig config) throws IOException {
		super(config);
	}

	public Connection getConnection() {
		if (this.connection == null) {
			LOGGER.error("[{}] Retrieve a closed connection !!!", this.id);
		}
		return this.connection;
	}

	@Override
	synchronized public void openImplement() throws IOException {
		final String dbUrl = this.config.getUrl();
		final String login = this.config.getLogin();
		final String password = this.config.getPassword();
		try {
			this.connection = DriverManager.getConnection(dbUrl, login, password);
		} catch (final SQLException ex) {
			LOGGER.error("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
			throw new IOException("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
		}
		if (this.connection == null) {
			throw new IOException("Connection db fail: NULL On URL: " + dbUrl);
		}
	}

	@Override
	synchronized public void closeImplement() throws IOException {
		if (this.connection == null) {
			LOGGER.error("Request close of un-open connection !!!");
			return;
		}
		try {
			this.connection.close();
			this.connection = null;
		} catch (final SQLException ex) {
			throw new IOException("Dis-connection db fail: " + ex.getMessage());
		}
	}
}
