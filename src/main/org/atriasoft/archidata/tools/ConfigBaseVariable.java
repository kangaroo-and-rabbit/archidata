package org.atriasoft.archidata.tools;

/**
 * Central configuration holder initialized from environment variables.
 *
 * <p>All fields are populated from the corresponding environment variables at class loading
 * and can be reset via {@link #clearAllValue()}. Getter methods provide default values
 * when the environment variable is not set.</p>
 */
public class ConfigBaseVariable {
	private ConfigBaseVariable() {
		// Utility class
	}

	/** Temporary data folder path (from {@code DATA_TMP_FOLDER} env var). */
	public static String tmpDataFolder;
	/** Media data folder path (from {@code DATA_FOLDER} env var). */
	public static String dataFolder;
	/** Whether the database can be created automatically (from {@code DB_ABLE_TO_CREATE} env var). */
	public static String dbAbleToCreate;
	/** Database host address (from {@code DB_HOST} env var). */
	public static String dbHost;
	/** Database port number as string (from {@code DB_PORT} env var). */
	public static String dbPort;
	/** Database user name (from {@code DB_USER} env var). */
	public static String dbUser;
	/** Whether to keep the database connection alive (from {@code DB_KEEP_CONNECTED} env var). */
	public static String dbKeepConnected;
	/** Database password (from {@code DB_PASSWORD} env var). */
	public static String dbPassword;
	/** Database name (from {@code DB_DATABASE} env var). */
	public static String bdDatabase;
	/** Local API address (from {@code API_ADDRESS} env var). */
	public static String apiAdress;
	/** SSO server address (from {@code SSO_ADDRESS} env var). */
	public static String ssoAdress;
	/** SSO authentication token (from {@code SSO_TOKEN} env var). */
	public static String ssoToken;
	/** Test mode flag (from {@code TEST_MODE} env var). */
	public static String testMode;
	/** E-mail sender address (from {@code EMAIL_FROM} env var). */
	public static String eMailFrom;
	/** E-mail login user name (from {@code EMAIL_LOGIN} env var). */
	public static String eMailLogin;
	/** E-mail login password (from {@code EMAIL_PASSWORD} env var). */
	public static String eMailPassword;
	/** Thumbnail image format (from {@code THUMBNAIL_FORMAT} env var). */
	public static String thumbnailFormat;
	/** Thumbnail width in pixels as string (from {@code THUMBNAIL_WIDTH} env var). */
	public static String thumbnailWidth;
	/** Database interface classes for data access registration. */
	public static Class<?>[] dbInterfacesClasses;

	/**
	 * Reloads all configuration values from environment variables. Primarily used for testing.
	 */
	public static void clearAllValue() {
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
		dbInterfacesClasses = new Class<?>[0];
	}

	static {
		clearAllValue();
	}

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
	 * Returns the database password, defaulting to {@code "base_db_password"}.
	 * @return The database password.
	 */
	public static String getDBPassword() {
		if (dbPassword == null) {
			return "base_db_password";
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
	 * Returns the local API address, defaulting to {@code "http://0.0.0.0:80/api/"}.
	 * @return The local API address.
	 */
	public static String getlocalAddress() {
		if (apiAdress == null) {
			return "http://0.0.0.0:80/api/";
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
			String password) {};

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
}
