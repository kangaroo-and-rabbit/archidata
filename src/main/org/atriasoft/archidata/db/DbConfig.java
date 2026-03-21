package org.atriasoft.archidata.db;

import java.util.List;
import java.util.Objects;

import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database configuration holder for MongoDB connections.
 *
 * <p>
 * Stores connection parameters such as hostname, port, credentials, database name,
 * and registered data classes. Provides a connection URL builder for MongoDB.
 * </p>
 */
public class DbConfig {
	static final Logger LOGGER = LoggerFactory.getLogger(DbConfig.class);
	private final String hostname;
	private final Short port;
	private final String login;
	private final String password;
	private String dbName;
	private final boolean keepConnected;
	private final List<Class<?>> classes;

	/**
	 * Constructs a DbConfig using values from {@link ConfigBaseVariable}.
	 *
	 * @throws DataAccessException if the configuration cannot be loaded
	 */
	public DbConfig() throws DataAccessException {
		this(ConfigBaseVariable.getDBHost(), ConfigBaseVariable.getDBPort(), ConfigBaseVariable.getDBLogin(),
				ConfigBaseVariable.getDBPassword(), ConfigBaseVariable.getDBName(),
				ConfigBaseVariable.getDBKeepConnected(), List.of(ConfigBaseVariable.getBbInterfacesClasses()));
	}

	/**
	 * Constructs a DbConfig with explicit connection parameters.
	 *
	 * @param hostname the database server hostname (defaults to "localhost" if null)
	 * @param port the database server port (defaults to 27017 if null)
	 * @param login the database login username
	 * @param password the database login password
	 * @param dbName the name of the database
	 * @param keepConnected whether to keep the connection open for reuse
	 * @param classes the list of data model classes registered with this configuration
	 * @throws DataAccessException if the configuration is invalid
	 */
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

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "DBConfig{hostname='" + this.hostname + '\'' + ", port=" + this.port + ", login='" + this.login + '\''
				+ ", password='" + this.password + '\'' + ", dbName='" + this.dbName + "' }";
	}

	/**
	 * Returns the database server hostname.
	 *
	 * @return the hostname
	 */
	public String getHostname() {
		return this.hostname;
	}

	/**
	 * Returns the database server port.
	 *
	 * @return the port number
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the database login username.
	 *
	 * @return the login
	 */
	public String getLogin() {
		return this.login;
	}

	/**
	 * Returns the database login password.
	 *
	 * @return the password
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Returns the database name.
	 *
	 * @return the database name
	 */
	public String getDbName() {
		return this.dbName;
	}

	/**
	 * Sets the database name.
	 *
	 * @param dbName the new database name
	 */
	public void setDbName(final String dbName) {
		this.dbName = dbName;
	}

	/**
	 * Returns whether connections should be kept open for reuse.
	 *
	 * @return {@code true} if connections should be kept connected
	 */
	public boolean getKeepConnected() {
		return this.keepConnected;
	}

	/**
	 * Returns the list of data model classes registered with this configuration.
	 *
	 * @return the list of classes
	 */
	public List<Class<?>> getClasses() {
		return this.classes;
	}

	/**
	 * Builds and returns the MongoDB connection URL from the configuration parameters.
	 *
	 * @return the MongoDB connection URL string
	 */
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

	/**
	 * Checks equality based on all configuration fields.
	 *
	 * @param other the object to compare with
	 * @return {@code true} if the configurations are equal
	 */
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

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hash(this.hostname, this.port, this.login, this.password, this.dbName, this.keepConnected,
				this.classes);
	}

	/**
	 * Creates a copy of this configuration.
	 *
	 * @return a new DbConfig with the same parameters, or {@code null} if cloning fails
	 */
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
