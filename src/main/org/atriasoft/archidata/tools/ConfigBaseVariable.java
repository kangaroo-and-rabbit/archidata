package org.atriasoft.archidata.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration holder initialized from environment variables.
 *
 * <p>All fields are populated from the corresponding environment variables at class loading
 * and can be set via setters before the configuration is locked. Once {@link #lock()} is called,
 * no further modifications are allowed unless {@link #allowReconfiguration(boolean)} was called
 * with {@code true} beforehand (typically only in test environments).</p>
 *
 * <p>Attempting to unlock a locked configuration without reconfiguration permission will log the
 * full stack trace, flush logs, and terminate the JVM immediately — this is treated as a security
 * violation.</p>
 */
public class ConfigBaseVariable {
	private ConfigBaseVariable() {
		// Utility class
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigBaseVariable.class);

	private static boolean locked = false;
	private static boolean reconfigurationAllowed = false;

	private static String tmpDataFolder;
	private static String dataFolder;
	private static String dbAbleToCreate;
	private static String dbHost;
	private static String dbPort;
	private static String dbUser;
	private static String dbKeepConnected;
	private static String dbPassword;
	private static String bdDatabase;
	private static String apiAdress;
	private static String ssoAdress;
	private static String ssoToken;
	private static String testMode;
	private static String eMailFrom;
	private static String eMailLogin;
	private static String eMailPassword;
	private static String thumbnailFormat;
	private static String thumbnailWidth;
	private static String dataDecryptEnable;
	private static String queryStatisticsFile;
	private static String queryStatisticsPeriod;
	private static byte[] encryptDefaultKey;
	private static String encryptKeysDir;
	private static Class<?>[] dbInterfacesClasses;

	/**
	 * Checks that the configuration is not locked before allowing a set operation.
	 * @param fieldName The name of the field being set (for error messages).
	 * @throws IllegalStateException If the configuration is locked.
	 */
	private static void checkNotLocked(final String fieldName) {
		if (locked) {
			throw new IllegalStateException(
					"Configuration is locked. Cannot modify '" + fieldName + "'. Call unlock() first.");
		}
	}

	/**
	 * Enables or disables the ability to unlock a locked configuration.
	 * This must be called BEFORE {@link #lock()} and is intended for test environments only.
	 *
	 * <p>In production, this should never be called — leaving reconfiguration disabled means
	 * {@link #lock()} is permanent and any attempt to {@link #unlock()} will crash the JVM.</p>
	 *
	 * @param allowed {@code true} to allow future unlock() calls, {@code false} to forbid them.
	 */
	public static void allowReconfiguration(final boolean allowed) {
		reconfigurationAllowed = allowed;
	}

	/**
	 * Locks the configuration, preventing any further modifications via setters.
	 * Typically called after server startup.
	 *
	 * <p>If {@link #allowReconfiguration(boolean)} was not called with {@code true},
	 * this lock is permanent — any call to {@link #unlock()} will terminate the JVM.</p>
	 */
	public static void lock() {
		locked = true;
		LOGGER.info("Configuration locked (reconfiguration {})", reconfigurationAllowed ? "allowed" : "FORBIDDEN");
	}

	/**
	 * Unlocks the configuration, allowing modifications via setters.
	 * Only works if {@link #allowReconfiguration(boolean)} was called with {@code true}.
	 *
	 * <p>If reconfiguration is not allowed, this method logs the full stack trace of the caller,
	 * flushes logs, and terminates the JVM with exit code 1. This is treated as a security violation.</p>
	 */
	public static void unlock() {
		if (!reconfigurationAllowed) {
			final RuntimeException violation = new RuntimeException(
					"SECURITY VIOLATION: unauthorized attempt to unlock configuration");
			LOGGER.error("==========================================================");
			LOGGER.error("SECURITY VIOLATION: unauthorized attempt to unlock configuration.");
			LOGGER.error("Caller stack trace:", violation);
			LOGGER.error("The JVM will now terminate.");
			LOGGER.error("==========================================================");
			// Also print to stderr to ensure visibility even if logging is misconfigured
			System.err.println("SECURITY VIOLATION: unauthorized attempt to unlock configuration.");
			violation.printStackTrace(System.err);
			System.err.flush();
			// Terminate immediately — Runtime.halt bypasses shutdown hooks
			Runtime.getRuntime().halt(1);
		}
		locked = false;
		LOGGER.info("Configuration unlocked");
	}

	/**
	 * Returns whether the configuration is currently locked.
	 * @return {@code true} if locked.
	 */
	public static boolean isLocked() {
		return locked;
	}

	// ========== Setters ==========

	/**
	 * Sets the temporary data folder path.
	 * @param value the new temporary data folder path
	 */
	public static void setTmpDataFolder(final String value) {
		checkNotLocked("tmpDataFolder");
		tmpDataFolder = value;
	}

	/**
	 * Sets the data folder path.
	 * @param value the new data folder path
	 */
	public static void setDataFolder(final String value) {
		checkNotLocked("dataFolder");
		dataFolder = value;
	}

	/**
	 * Sets whether the database may be created automatically.
	 * @param value the new "able to create" flag value
	 */
	public static void setDbAbleToCreate(final String value) {
		checkNotLocked("dbAbleToCreate");
		dbAbleToCreate = value;
	}

	/**
	 * Sets the database host.
	 * @param value the new database host
	 */
	public static void setDbHost(final String value) {
		checkNotLocked("dbHost");
		dbHost = value;
	}

	/**
	 * Sets the database port.
	 * @param value the new database port
	 */
	public static void setDbPort(final String value) {
		checkNotLocked("dbPort");
		dbPort = value;
	}

	/**
	 * Sets the database user.
	 * @param value the new database user
	 */
	public static void setDbUser(final String value) {
		checkNotLocked("dbUser");
		dbUser = value;
	}

	/**
	 * Sets whether the database connection is kept alive.
	 * @param value the new "keep connected" flag value
	 */
	public static void setDbKeepConnected(final String value) {
		checkNotLocked("dbKeepConnected");
		dbKeepConnected = value;
	}

	/**
	 * Sets the database password.
	 * @param value the new database password
	 */
	public static void setDbPassword(final String value) {
		checkNotLocked("dbPassword");
		dbPassword = value;
	}

	/**
	 * Sets the database name.
	 * @param value the new database name
	 */
	public static void setBdDatabase(final String value) {
		checkNotLocked("bdDatabase");
		bdDatabase = value;
	}

	/**
	 * Sets the API address.
	 * @param value the new API address
	 */
	public static void setApiAddress(final String value) {
		checkNotLocked("apiAdress");
		apiAdress = value;
	}

	/**
	 * Sets the SSO address.
	 * @param value the new SSO address
	 */
	public static void setSsoAddress(final String value) {
		checkNotLocked("ssoAdress");
		ssoAdress = value;
	}

	/**
	 * Sets the SSO token.
	 * @param value the new SSO token
	 */
	public static void setSsoToken(final String value) {
		checkNotLocked("ssoToken");
		ssoToken = value;
	}

	/**
	 * Sets the test mode flag.
	 * @param value the new test mode value
	 */
	public static void setTestMode(final String value) {
		checkNotLocked("testMode");
		testMode = value;
	}

	/**
	 * Sets the "from" address used for outgoing e-mails.
	 * @param value the new e-mail "from" address
	 */
	public static void setEMailFrom(final String value) {
		checkNotLocked("eMailFrom");
		eMailFrom = value;
	}

	/**
	 * Sets the login used for the e-mail account.
	 * @param value the new e-mail login
	 */
	public static void setEMailLogin(final String value) {
		checkNotLocked("eMailLogin");
		eMailLogin = value;
	}

	/**
	 * Sets the password used for the e-mail account.
	 * @param value the new e-mail password
	 */
	public static void setEMailPassword(final String value) {
		checkNotLocked("eMailPassword");
		eMailPassword = value;
	}

	/**
	 * Sets the thumbnail output format.
	 * @param value the new thumbnail format
	 */
	public static void setThumbnailFormat(final String value) {
		checkNotLocked("thumbnailFormat");
		thumbnailFormat = value;
	}

	/**
	 * Sets the thumbnail width.
	 * @param value the new thumbnail width
	 */
	public static void setThumbnailWidth(final String value) {
		checkNotLocked("thumbnailWidth");
		thumbnailWidth = value;
	}

	/**
	 * Sets the path of the query-statistics JSON file. Setting a non-empty value enables the
	 * query-statistics collection mode.
	 * @param value the new query-statistics file path, or {@code null} to disable the mode
	 */
	public static void setQueryStatisticsFile(final String value) {
		checkNotLocked("queryStatisticsFile");
		queryStatisticsFile = value;
	}

	/**
	 * Sets the cron expression driving the query-statistics flush period.
	 * @param value the new cron expression
	 */
	public static void setQueryStatisticsPeriod(final String value) {
		checkNotLocked("queryStatisticsPeriod");
		queryStatisticsPeriod = value;
	}

	/**
	 * Enables or disables the decryption of {@code @DataEncrypt} fields on read.
	 * @param value {@code "true"} to allow decryption, anything else (or {@code null}) to forbid it.
	 */
	public static void setDataDecryptEnable(final String value) {
		checkNotLocked("dataDecryptEnable");
		dataDecryptEnable = value;
	}

	/**
	 * Sets the raw bytes of the base encryption key (logical name {@code "default"}).
	 * @param value the key material (raw or Base64 symmetric key, or PEM asymmetric key), may be {@code null}.
	 */
	public static void setEncryptDefaultKey(final byte[] value) {
		checkNotLocked("encryptDefaultKey");
		encryptDefaultKey = value == null ? null : value.clone();
	}

	/**
	 * Sets the directory scanned for additional named encryption keys.
	 * @param value the directory path, may be {@code null}.
	 */
	public static void setEncryptKeysDir(final String value) {
		checkNotLocked("encryptKeysDir");
		encryptKeysDir = value;
	}

	/**
	 * Reloads all configuration values from environment variables and unlocks the configuration.
	 * Only works if reconfiguration is allowed. Primarily used for testing.
	 *
	 * <p>If reconfiguration is not allowed and the configuration is locked,
	 * this will trigger the same security violation as {@link #unlock()}.</p>
	 */
	public static void clearAllValue() {
		if (locked) {
			unlock();
		}
		tmpDataFolder = System.getenv("DATA_TMP_FOLDER");
		dataFolder = System.getenv("DATA_FOLDER");
		dbAbleToCreate = System.getenv("DB_ABLE_TO_CREATE");
		dbHost = System.getenv("DB_HOST");
		dbPort = System.getenv("DB_PORT");
		dbUser = System.getenv("DB_USER");
		dbKeepConnected = System.getenv("DB_KEEP_CONNECTED");
		dbPassword = System.getenv("DB_PASSWORD");
		bdDatabase = System.getenv("DB_DATABASE");
		apiAdress = System.getenv("API_ADDRESS");
		ssoAdress = System.getenv("SSO_ADDRESS");
		ssoToken = System.getenv("SSO_TOKEN");
		testMode = System.getenv("TEST_MODE");
		eMailFrom = System.getenv("EMAIL_FROM");
		eMailLogin = System.getenv("EMAIL_LOGIN");
		eMailPassword = System.getenv("EMAIL_PASSWORD");
		thumbnailFormat = System.getenv("THUMBNAIL_FORMAT");
		thumbnailWidth = System.getenv("THUMBNAIL_WIDTH");
		dataDecryptEnable = System.getenv("DATA_DECRYPT_ENABLE");
		queryStatisticsFile = System.getenv("QUERY_STATISTICS_FILE");
		queryStatisticsPeriod = System.getenv("QUERY_STATISTICS_PERIOD");
		encryptDefaultKey = readSecretBytes("DATA_ENCRYPT_KEY");
		encryptKeysDir = System.getenv("DATA_ENCRYPT_KEYS_DIR");
		dbInterfacesClasses = new Class<?>[0];
	}

	/**
	 * Reads a secret value either from the file pointed to by {@code <name>_FILE} (the Docker secrets
	 * convention) or, as a fallback, from the inline environment variable {@code <name>}.
	 *
	 * @param name the base environment variable name.
	 * @return the secret bytes, or {@code null} if neither variable is defined.
	 */
	private static byte[] readSecretBytes(final String name) {
		final String filePath = System.getenv(name + "_FILE");
		if (filePath != null && !filePath.isEmpty()) {
			try {
				return Files.readAllBytes(Path.of(filePath));
			} catch (final IOException e) {
				LOGGER.error("Failed to read secret file for {}_FILE: {}", name, filePath, e);
				return null;
			}
		}
		final String inline = System.getenv(name);
		if (inline != null && !inline.isEmpty()) {
			return inline.getBytes(StandardCharsets.UTF_8);
		}
		return null;
	}

	static {
		clearAllValue();
	}

	// ========== Getters ==========

	/**
	 * Returns the temporary data folder path, defaulting to {@code "./data/tmp"}.
	 * @return The temporary data folder path.
	 */
	public static String getTmpDataFolder() {
		if (tmpDataFolder == null) {
			return "./data/tmp";
		}
		return tmpDataFolder;
	}

	/**
	 * Returns the media data folder path, defaulting to {@code "./data/media"}.
	 * @return The media data folder path.
	 */
	public static String getMediaDataFolder() {
		if (dataFolder == null) {
			return "./data/media";
		}
		return dataFolder;
	}

	/**
	 * Returns whether the database can be created automatically, defaulting to {@code true}.
	 * @return {@code true} if database creation is allowed.
	 */
	public static boolean getDBAbleToCreate() {
		if (dbAbleToCreate == null) {
			return true;
		}
		return Boolean.getBoolean(dbAbleToCreate);
	}

	/**
	 * Returns the database host, defaulting to {@code "localhost"}.
	 * @return The database host address.
	 */
	public static String getDBHost() {
		if (dbHost == null) {
			return "localhost";
		}
		return dbHost;
	}

	/**
	 * Returns the database port, defaulting to {@code 27017}.
	 * @return The database port number.
	 */
	public static Short getDBPort() {
		if (dbPort == null) {
			return 27017;
		}
		return Short.parseShort(dbPort);
	}

	/**
	 * Returns the database login user name, defaulting to {@code "root"}.
	 * @return The database login user name.
	 */
	public static String getDBLogin() {
		if (dbUser == null) {
			return "root";
		}
		return dbUser;
	}

	/**
	 * Returns the database password.
	 * @return The database password.
	 * @throws IllegalStateException If the database password is not configured.
	 */
	public static String getDBPassword() {
		if (dbPassword == null) {
			throw new IllegalStateException(
					"DB_PASSWORD is not configured. Set the DB_PASSWORD environment variable or call setDbPassword().");
		}
		return dbPassword;
	}

	/**
	 * Returns the database name, or {@code null} if not configured.
	 * @return The database name.
	 */
	public static String getDBName() {
		return bdDatabase;
	}

	/**
	 * Returns whether the database connection should be kept alive, defaulting to {@code false}.
	 * @return {@code true} if the connection should be kept alive.
	 */
	public static boolean getDBKeepConnected() {
		if (dbKeepConnected == null) {
			return false;
		}
		return Boolean.parseBoolean(dbKeepConnected);
	}

	/**
	 * Returns the local API address, defaulting to {@code "http://localhost:80/api/"}.
	 * @return The local API address.
	 */
	public static String getLocalAddress() {
		if (apiAdress == null) {
			return "http://localhost:80/api/";
		}
		return apiAdress;
	}

	/**
	 * Returns the SSO server address, or {@code null} if not configured.
	 * @return The SSO server address.
	 */
	public static String getSSOAddress() {
		return ssoAdress;
	}

	/**
	 * Returns the SSO authentication token, or {@code null} if not configured.
	 * @return The SSO token.
	 */
	public static String ssoToken() {
		return ssoToken;
	}

	/**
	 * Returns whether test mode is enabled, defaulting to {@code false}.
	 * @return {@code true} if test mode is enabled.
	 */
	public static boolean getTestMode() {
		if (testMode == null) {
			return false;
		}
		return Boolean.parseBoolean(testMode);
	}

	/**
	 * Record holding e-mail configuration (sender, login, password).
	 * @param from The sender e-mail address.
	 * @param login The login user name for the e-mail server.
	 * @param password The login password for the e-mail server.
	 */
	public record EMailConfig(
			String from,
			String login,
			String password) {}

	/**
	 * Returns the e-mail configuration, or {@code null} if any required field is missing.
	 * @return The e-mail configuration record, or {@code null}.
	 */
	public static EMailConfig getEMailConfig() {
		if (eMailFrom == null || eMailLogin == null || eMailPassword == null) {
			return null;
		}
		return new EMailConfig(eMailFrom, eMailLogin, eMailPassword);
	}

	/**
	 * Returns the registered database interface classes.
	 * @return An array of database interface classes.
	 */
	public static Class<?>[] getBbInterfacesClasses() {
		return dbInterfacesClasses;
	}

	/**
	 * Sets the database interface classes for data access registration.
	 * @param data The array of database interface classes to register.
	 */
	public static void setBbInterfacesClasses(final Class<?>[] data) {
		checkNotLocked("dbInterfacesClasses");
		dbInterfacesClasses = data;
	}

	/**
	 * Returns the thumbnail image format, defaulting to {@code "png"}.
	 * @return The thumbnail format string (e.g. "png", "jpg").
	 */
	public static String getThumbnailFormat() {
		if (thumbnailFormat == null || thumbnailFormat.isEmpty()) {
			return "png";
		}
		return thumbnailFormat;
	}

	/**
	 * Returns the thumbnail width in pixels, defaulting to {@code 256}.
	 * @return The thumbnail width.
	 */
	public static int getThumbnailWidth() {
		if (thumbnailWidth == null || thumbnailWidth.isEmpty()) {
			return 256;
		}
		return Integer.parseInt(thumbnailWidth);
	}

	/**
	 * Returns the path of the JSON file where the query statistics are accumulated.
	 *
	 * <p>
	 * This is the switch of the whole query-statistics mode: when it is {@code null} or empty the
	 * collection is fully disabled and costs nothing at runtime. Set the environment variable
	 * {@code QUERY_STATISTICS_FILE} to a writable path to enable it.
	 * </p>
	 *
	 * @return The query-statistics file path, or {@code null} when the mode is disabled.
	 */
	public static String getQueryStatisticsFile() {
		if (queryStatisticsFile == null || queryStatisticsFile.isEmpty()) {
			return null;
		}
		return queryStatisticsFile;
	}

	/**
	 * Returns the cron expression driving the periodic flush of the query statistics to disk,
	 * defaulting to {@code "*&#47;10 * * * *"} (every 10 minutes).
	 *
	 * @return The flush period as a cron expression.
	 */
	public static String getQueryStatisticsPeriod() {
		if (queryStatisticsPeriod == null || queryStatisticsPeriod.isEmpty()) {
			return "*/10 * * * *";
		}
		return queryStatisticsPeriod;
	}

	/**
	 * Returns whether archidata is allowed to decrypt {@code @DataEncrypt} fields on read.
	 * Defaults to {@code false}: without explicit activation, encrypted fields are never decrypted.
	 * @return {@code true} if decryption is enabled.
	 */
	public static boolean getDataDecryptEnabled() {
		if (dataDecryptEnable == null) {
			return false;
		}
		return Boolean.parseBoolean(dataDecryptEnable);
	}

	/**
	 * Returns the raw bytes of the base encryption key, or {@code null} if not configured.
	 * @return The base key material.
	 */
	public static byte[] getEncryptDefaultKey() {
		return encryptDefaultKey == null ? null : encryptDefaultKey.clone();
	}

	/**
	 * Returns the directory scanned for additional named encryption keys, or {@code null}.
	 * @return The encryption keys directory path.
	 */
	public static String getEncryptKeysDir() {
		return encryptKeysDir;
	}
}
