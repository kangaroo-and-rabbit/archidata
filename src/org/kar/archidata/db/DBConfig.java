package org.kar.archidata.db;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConfig {
	static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);
	private final String type;
	private final String hostname;
	private final int port;
	private final String login;
	private final String password;
	private final String dbName;
	private final boolean keepConnected;

	public DBConfig(final String type, final String hostname, final Integer port, final String login,
			final String password, final String dbName, final boolean keepConnected) throws DataAccessException {
		if (type == null) {
			this.type = "mysql";
		} else {
			if (!"mysql".equals(type) && !"sqlite".equals(type) && !"mongo".equals(type)) {
				throw new DataAccessException("unexpected DB type: '" + type + "'");
			}
			this.type = type;
		}
		if (hostname == null) {
			this.hostname = "localhost";
		} else {
			this.hostname = hostname;
		}
		if (port == null) {
			if ("mysql".equals(this.type)) {
				this.port = 3306;
			} else {
				this.port = 27017;
			}
		} else {
			this.port = port;
		}
		this.login = login;
		this.password = password;
		this.dbName = dbName;
		this.keepConnected = keepConnected;
		
	}

	@Override
	public String toString() {
		return "DBConfig{type='" + this.type + '\'' + ", hostname='" + this.hostname + '\'' + ", port=" + this.port
				+ ", login='" + this.login + '\'' + ", password='" + this.password + '\'' + ", dbName='" + this.dbName
				+ "' }";
	}

	public String getHostname() {
		return this.hostname;
	}

	public int getPort() {
		return this.port;
	}

	public String getLogin() {
		return this.login;
	}

	public String getPassword() {
		return this.password;
	}

	public String getDbName() {
		return this.dbName;
	}

	public boolean getKeepConnected() {
		return this.keepConnected;
	}

	public String getUrl() {
		return getUrl(false);
	}

	public String getUrl(final boolean isRoot) {
		if (this.type.equals("sqlite")) {
			if (isRoot) {
				LOGGER.error("Can not manage root connection on SQLite...");
			}
			if (this.hostname.equals("memory")) {
				return "jdbc:sqlite::memory:";
			}
			return "jdbc:sqlite:" + this.hostname + ".db";
		}
		if ("mongo".equals(this.type)) {
			return "mongodb:" + getLogin() + ":" + getPassword() + "//" + this.hostname + ":" + this.port;
		}
		if ("mysql".equals(this.type)) {
			if (isRoot) {
				return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port
						+ "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
			}
			return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port + "/" + this.dbName
					+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
		}
		return "dead_code";
	}
}
