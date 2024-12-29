package org.kar.archidata.db;

import java.util.List;
import java.util.Objects;

import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConfig {
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccess.class);
	private final String type;
	private final String hostname;
	private final Short port;
	private final String login;
	private final String password;
	private String dbName;
	private final boolean keepConnected;
	private final List<Class<?>> classes;

	public DbConfig() throws DataAccessException {
		this(ConfigBaseVariable.getDBType(), ConfigBaseVariable.getDBHost(), ConfigBaseVariable.getDBPort(),
				ConfigBaseVariable.getDBLogin(), ConfigBaseVariable.getDBPassword(), ConfigBaseVariable.getDBName(),
				ConfigBaseVariable.getDBKeepConnected(), List.of(ConfigBaseVariable.getBbInterfacesClasses()));
	}

	public DbConfig(final String type, final String hostname, final Short port, final String login,
			final String password, final String dbName, final boolean keepConnected, final List<Class<?>> classes)
			throws DataAccessException {
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
		this.classes = classes;

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

	public String getType() {
		return this.type;
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

	public void setDbName(final String dbName) {
		this.dbName = dbName;
	}

	public boolean getKeepConnected() {
		return this.keepConnected;
	}

	public List<Class<?>> getClasses() {
		return this.classes;
	}

	public String getUrl() {
		if (this.type.equals("sqlite")) {
			if (this.hostname.equals("memory")) {
				return "jdbc:sqlite::memory:";
			}
			return "jdbc:sqlite:" + this.hostname + ".db";
		}
		if ("mongo".equals(this.type)) {
			return "mongodb://" + getLogin() + ":" + getPassword() + "@" + this.hostname + ":" + this.port;
		}
		if ("mysql".equals(this.type)) {
			if (this.dbName == null || this.dbName.isEmpty()) {
				LOGGER.warn("Request log on SQL: root");
				return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port
						+ "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
			}
			return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port + "/" + this.dbName
					+ "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
		}
		return "dead_code";
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		if (other instanceof final DbConfig dbConfig) {
			return Objects.equals(this.port, dbConfig.port) //
					&& this.keepConnected == dbConfig.keepConnected //
					&& Objects.equals(this.type, dbConfig.type) //
					&& Objects.equals(this.hostname, dbConfig.hostname) //
					&& Objects.equals(this.login, dbConfig.login) //
					&& Objects.equals(this.password, dbConfig.password) //
					&& Objects.equals(this.dbName, dbConfig.dbName) //
					&& Objects.equals(this.classes, dbConfig.classes);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.hostname, this.port, this.login, this.password, this.dbName,
				this.keepConnected, this.classes);
	}

	@Override
	public DbConfig clone() {
		try {
			return new DbConfig(this.type, this.hostname, this.port, this.login, this.password, this.dbName,
					this.keepConnected, this.classes);
		} catch (final DataAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
