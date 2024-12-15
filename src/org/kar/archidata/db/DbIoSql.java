package org.kar.archidata.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbIoSql extends DbIo {
	final static Logger LOGGER = LoggerFactory.getLogger(DbIoSql.class);

	private Connection con = null;

	public DbIoSql(final DbConfig config) throws IOException {
		super(config);
		// If we want to stay connected, we instantiate a basic connection (only force close can remove it).
		if (this.config.getKeepConnected()) {
			open();
		}
	}

	public Connection getConnection() {
		if (this.con == null) {
			LOGGER.error("[{}] >>>>>>>>>> Retrieve a closed connection !!!", this.id);
		}
		LOGGER.error("[{}] ++++++++++ Retrieve connection {}", this.id, this.con);
		return this.con;
	}

	@Override
	synchronized public void openImplement() throws IOException {
		final String dbUrl = this.config.getUrl();
		final String login = this.config.getLogin();
		final String password = this.config.getPassword();
		LOGGER.error("[{}] >>>>>>>>>> openImplement ", this.id);
		try {
			this.con = DriverManager.getConnection(dbUrl, login, password);
			LOGGER.error("[{}] >>>>>>>>>> openImplement ==> {}", this.id, this.con);
		} catch (final SQLException ex) {
			LOGGER.error("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
			throw new IOException("Connection db fail: " + ex.getMessage() + " On URL: " + dbUrl);
		}
		if (this.con == null) {
			LOGGER.error("Request open of un-open connection !!!");
			return;
		}
	}

	@Override
	synchronized public void closeImplement() throws IOException {
		LOGGER.error("[{}] >>>>>>>>>> closeImplement ", this.id);
		if (this.con == null) {
			LOGGER.error("Request close of un-open connection !!!");
			return;
		}
		try {
			this.con.close();
			this.con = null;
		} catch (final SQLException ex) {
			throw new IOException("Dis-connection db fail: " + ex.getMessage());
		}
	}
}
