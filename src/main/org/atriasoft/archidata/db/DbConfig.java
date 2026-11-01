package org.atriasoft.archidata.db;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class DbConfig implements Cloneable {
	static final Logger LOGGER = LoggerFactory.getLogger(DbConfig.class);
	/** Port used when none is given (MongoDB default). */
	private static final int DEFAULT_PORT = 27017;
	/** Highest valid TCP port. */
	private static final int MAX_PORT = 65535;
	private final String hostname;
	private final Integer port;
	private final String login;
	private final String password;
	private String dbName;
	private final boolean keepConnected;
	private final List<Class<?>> classes;
	/** TLS on the connection ({@code DB_TLS}). */
	private volatile boolean tls;
	/** Authentication database ({@code DB_AUTH_SOURCE}), {@code null} for the default. */
	private String authSource;
	/** The WARN about the {@code root} account is written once per process. */
	private static final AtomicBoolean ROOT_WARNED = new AtomicBoolean(false);

	/**
	 * Constructs a DbConfig using values from {@link ConfigBaseVariable}.
	 *
	 * @throws DataAccessException if the configuration cannot be loaded, including a {@code DB_PORT} that is
	 *         not an integer between 1 and 65535 (the {@code IllegalArgumentException} of
	 *         {@link ConfigBaseVariable#getDBPort()} is the cause)
	 */
	public DbConfig() throws DataAccessException {
		this(ConfigBaseVariable.getDBHost(), configuredPort(), ConfigBaseVariable.getDBLogin(),
				ConfigBaseVariable.getDBPassword(), ConfigBaseVariable.getDBName(),
				ConfigBaseVariable.getDBKeepConnected(), List.of(ConfigBaseVariable.getBbInterfacesClasses()));
	}

	/**
	 * Constructs a DbConfig with explicit connection parameters.
	 *
	 * @param hostname the database server hostname (defaults to "localhost" if null)
	 * @param port the database server port, between 1 and 65535 (defaults to 27017 if null)
	 * @param login the database login username
	 * @param password the database login password
	 * @param dbName the name of the database
	 * @param keepConnected whether to keep the connection open for reuse
	 * @param classes the list of data model classes registered with this configuration, copied
	 * @throws DataAccessException if the configuration is invalid, including a port not between 1 and 65535
	 */
	public DbConfig(final String hostname, final Integer port, final String login, final String password,
			final String dbName, final boolean keepConnected, final List<Class<?>> classes) throws DataAccessException {

		if (hostname == null) {
			this.hostname = "localhost";
		} else {
			this.hostname = hostname;
		}
		if (port == null) {
			this.port = DEFAULT_PORT;
		} else if (port < 1 || port > MAX_PORT) {
			throw new DataAccessException("Invalid DB port " + port + ": must be between 1 and " + MAX_PORT);
		} else {
			this.port = port;
		}
		this.login = login;
		this.password = password;
		this.dbName = dbName;
		this.keepConnected = keepConnected;
		this.classes = classes == null ? null : List.copyOf(classes);
		this.tls = ConfigBaseVariable.getDBTls();
		this.authSource = ConfigBaseVariable.getDBAuthSource();
	}

	/**
	 * Constructs a DbConfig with a {@code Short} port (former signature, kept for source and binary
	 * compatibility). A port above 32767 cannot be given this way: a negative value is rejected.
	 *
	 * @param hostname the database server hostname (defaults to "localhost" if null)
	 * @param port the database server port (defaults to 27017 if null)
	 * @param login the database login username
	 * @param password the database login password
	 * @param dbName the name of the database
	 * @param keepConnected whether to keep the connection open for reuse
	 * @param classes the list of data model classes registered with this configuration
	 * @throws DataAccessException if the configuration is invalid, including a port not between 1 and 65535
	 * @deprecated use {@link #DbConfig(String, Integer, String, String, String, boolean, List)}: the port is an
	 *             {@code Integer} (up to 65535). A literal {@code null} port is ambiguous between the two
	 *             constructors: write {@code (Integer) null}.
	 */
	@Deprecated
	public DbConfig(final String hostname, final Short port, final String login, final String password,
			final String dbName, final boolean keepConnected, final List<Class<?>> classes) throws DataAccessException {
		this(hostname, port == null ? null : Integer.valueOf(port), login, password, dbName, keepConnected, classes);
	}

	/**
	 * Reads the port of {@link ConfigBaseVariable}: its {@code IllegalArgumentException} (the configuration
	 * accessors throw unchecked exceptions) becomes the {@link DataAccessException} of the constructors.
	 *
	 * @return the configured port
	 * @throws DataAccessException if {@code DB_PORT} is not an integer between 1 and 65535
	 */
	private static Integer configuredPort() throws DataAccessException {
		try {
			return ConfigBaseVariable.getDBPort();
		} catch (final IllegalArgumentException ex) {
			throw new DataAccessException(ex.getMessage(), ex);
		}
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "DBConfig{hostname='" + this.hostname + '\'' + ", port=" + this.port + ", login='" + this.login + '\''
				+ ", password='****'" + ", dbName='" + this.dbName + "', tls=" + this.tls + ", authSource='"
				+ this.authSource + "' }";
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
	 * Returns whether the connection uses TLS ({@code tls=true} in the URL).
	 *
	 * @return {@code true} if TLS is enabled
	 */
	public boolean isTls() {
		return this.tls;
	}

	/**
	 * Enables or disables TLS on the connection (default: {@code DB_TLS}).
	 *
	 * @param tls {@code true} to enable TLS
	 */
	public void setTls(final boolean tls) {
		this.tls = tls;
	}

	/**
	 * Returns the authentication database, or {@code null} for the default ({@code admin} for {@code root}).
	 *
	 * @return the authentication database, or {@code null}
	 */
	public String getAuthSource() {
		return this.authSource;
	}

	/**
	 * Sets the authentication database (default: {@code DB_AUTH_SOURCE}).
	 *
	 * @param authSource the authentication database, or {@code null} for the default
	 */
	public void setAuthSource(final String authSource) {
		this.authSource = authSource;
	}

	/**
	 * Returns the list of data model classes registered with this configuration.
	 *
	 * @return the immutable list of classes
	 */
	public List<Class<?>> getClasses() {
		return this.classes;
	}

	/**
	 * Percent-encodes a URL component (RFC 3986): {@code URLEncoder} writes a space as {@code +}, which the
	 * MongoDB URI parser reads as a literal plus.
	 */
	private static String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * Builds and returns the MongoDB connection URL from the configuration parameters.
	 *
	 * <p>The login, the password, the database name and the authentication database are percent-encoded, so
	 * that a generated password containing {@code : / ? # @ %} neither breaks the URL nor adds options to it.
	 * {@code authSource} is the configured one ({@code DB_AUTH_SOURCE}), otherwise {@code admin} for the
	 * {@code root} user (a WARN is written once: the application should use an account limited to its
	 * database). {@code tls=true} is added when TLS is enabled ({@code DB_TLS}).
	 *
	 * @return the MongoDB connection URL string
	 */
	public String getUrl() {
		final StringBuilder tmp = new StringBuilder("mongodb://");
		tmp.append(encode(getLogin()));
		tmp.append(":");
		tmp.append(encode(getPassword()));
		tmp.append("@");
		tmp.append(this.hostname);
		tmp.append(":");
		tmp.append(this.port);
		tmp.append("/");
		if (this.dbName != null) {
			tmp.append(encode(this.dbName));
		}
		final List<String> params = new ArrayList<>();
		final boolean root = "root".equals(getLogin());
		if (root && ROOT_WARNED.compareAndSet(false, true)) {
			LOGGER.warn("The database is accessed with the 'root' account (cluster administrator): use an account "
					+ "with the readWrite role on '{}' only (DB_USER, DB_AUTH_SOURCE)", this.dbName);
		}
		if (this.authSource != null) {
			params.add("authSource=" + encode(this.authSource));
		} else if (root) {
			// For root user, authenticate against admin database
			params.add("authSource=admin");
		}
		if (this.tls) {
			params.add("tls=true");
		}
		if (!params.isEmpty()) {
			tmp.append("?").append(String.join("&", params));
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
					&& this.tls == dbConfig.tls //
					&& Objects.equals(this.authSource, dbConfig.authSource) //
					&& Objects.equals(this.classes, dbConfig.classes);
		}
		return false;
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return Objects.hash(this.hostname, this.port, this.login, this.password, this.dbName, this.keepConnected,
				this.classes, this.tls, this.authSource);
	}

	/**
	 * Creates a copy of this configuration.
	 *
	 * @return a new DbConfig with the same parameters, never {@code null}
	 */
	@Override
	public DbConfig clone() {
		try {
			return (DbConfig) super.clone();
		} catch (final CloneNotSupportedException e) {
			throw new IllegalStateException("DbConfig is Cloneable", e);
		}
	}
}
