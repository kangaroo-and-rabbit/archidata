package org.atriasoft.archidata.db;

import java.util.List;
import java.util.Objects;

import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConfig {
	static final Logger LOGGER = LoggerFactory.getLogger(DbConfig.class);
	private final String hostname;
	private final Short port;
	private final String login;
	private final String password;
	private String dbName;
	private final boolean keepConnected;
	private final List<Class<?>> classes;

	public DbConfig() throws DataAccessException {
		this(ConfigBaseVariable.getDBHost(), ConfigBaseVariable.getDBPort(), ConfigBaseVariable.getDBLogin(),
				ConfigBaseVariable.getDBPassword(), ConfigBaseVariable.getDBName(),
				ConfigBaseVariable.getDBKeepConnected(), List.of(ConfigBaseVariable.getBbInterfacesClasses()));
	}

	public DbConfig(final String hostname, final Short port, final String login, final String password,
			final String dbName, final boolean keepConnected, final List<Class<?>> classes) throws DataAccessException {

		if (hostname == null) {
			this.hostname = "localhost";
		} else {
			this.hostname = hostname;
		}
		if (port == null) {
			this.port = 27017;
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
		return "DBConfig{hostname='" + this.hostname + '\'' + ", port=" + this.port + ", login='" + this.login + '\''
				+ ", password='" + this.password + '\'' + ", dbName='" + this.dbName + "' }";
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
		final String tmpPassword = getPassword().replace("@", "%40");
		final StringBuilder tmp = new StringBuilder("mongodb://");
		tmp.append(getLogin());
		tmp.append(":");
		tmp.append(tmpPassword);
		tmp.append("@");
		tmp.append(this.hostname);
		tmp.append(":");
		tmp.append(this.port);
		tmp.append("/");
		tmp.append(this.dbName);
		if ("root".equals(getLogin())) {
			// For root user, authenticate against admin database
			tmp.append("?authSource=admin");
		}
		return tmp.toString();
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
		return Objects.hash(this.hostname, this.port, this.login, this.password, this.dbName, this.keepConnected,
				this.classes);
	}

	@Override
	public DbConfig clone() {
		try {
			return new DbConfig(this.hostname, this.port, this.login, this.password, this.dbName, this.keepConnected,
					this.classes);
		} catch (final DataAccessException e) {
			LOGGER.error("Failed to clone DbConfig: {}", e.getMessage(), e);
		}
		return null;
	}
}
