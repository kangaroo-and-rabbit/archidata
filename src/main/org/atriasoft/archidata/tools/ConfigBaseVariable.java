package org.atriasoft.archidata.tools;

public class ConfigBaseVariable {
	static public String tmpDataFolder;
	static public String dataFolder;
	static public String dbAbleToCreate;
	static public String dbType;
	static public String dbHost;
	static public String dbPort;
	static public String dbUser;
	static public String dbKeepConnected;
	static public String dbPassword;
	static public String bdDatabase;
	static public String apiAdress;
	static public String ssoAdress;
	static public String ssoToken;
	static public String testMode;
	static public String eMailFrom;
	static public String eMailLogin;
	static public String eMailPassword;
	static public String thumbnailFormat;
	static public String thumbnailWidth;
	static public Class<?>[] dbInterfacesClasses;

	// For test only
	public static void clearAllValue() {
		tmpDataFolder = System.getenv("DATA_TMP_FOLDER");
		dataFolder = System.getenv("DATA_FOLDER");
		dbAbleToCreate = System.getenv("DB_ABLE_TO_CREATE");
		dbType = System.getenv("DB_TYPE");
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

	public static String getTmpDataFolder() {
		if (tmpDataFolder == null) {
			return "./data/tmp";
		}
		return tmpDataFolder;
	}

	public static String getMediaDataFolder() {
		if (dataFolder == null) {
			return "./data/media";
		}
		return dataFolder;
	}

	public static boolean getDBAbleToCreate() {
		if (dbAbleToCreate == null) {
			return true;
		}
		return Boolean.getBoolean(dbAbleToCreate);
	}

	public static String getDBType() {
		if (dbType == null) {
			return "mysql";
		}
		return dbType;
	}

	public static String getDBHost() {
		if (dbHost == null) {
			return "localhost";
		}
		return dbHost;
	}

	public static Short getDBPort() {
		if (dbPort == null) {
			if (getDBType().equals("mongo")) {
				return 27017;
			}
			return 3306;
		}
		if (dbPort == null) {
			return null;
		}
		return Short.parseShort(dbPort);
	}

	public static String getDBLogin() {
		if (dbUser == null) {
			return "root";
		}
		return dbUser;
	}

	public static String getDBPassword() {
		if (dbPassword == null) {
			return "base_db_password";
		}
		return dbPassword;
	}

	public static String getDBName() {
		return bdDatabase;
	}

	public static boolean getDBKeepConnected() {
		if (dbKeepConnected == null) {
			return false;
		}
		return Boolean.parseBoolean(dbKeepConnected);
	}

	public static String getlocalAddress() {
		if (apiAdress == null) {
			return "http://0.0.0.0:80/api/";
		}
		return apiAdress;
	}

	public static String getSSOAddress() {
		return ssoAdress;
	}

	public static String ssoToken() {
		return ssoToken;
	}

	public static boolean getTestMode() {
		if (testMode == null) {
			return false;
		}
		return Boolean.parseBoolean(testMode);
	}

	public record EMailConfig(
			String from,
			String login,
			String password) {};

	public static EMailConfig getEMailConfig() {
		if (eMailFrom == null || eMailLogin == null || eMailPassword == null) {
			return null;
		}
		return new EMailConfig(eMailFrom, eMailLogin, eMailPassword);
	}

	public static Class<?>[] getBbInterfacesClasses() {
		return dbInterfacesClasses;
	}

	public static void setBbInterfacesClasses(final Class<?>[] data) {
		dbInterfacesClasses = data;
	}

	public static String getThumbnailFormat() {
		if (thumbnailFormat == null || thumbnailFormat.isEmpty()) {
			return "png";
		}
		return thumbnailFormat;
	}

	public static int getThumbnailWidth() {
		if (thumbnailWidth == null || thumbnailWidth.isEmpty()) {
			return 256;
		}
		return Integer.parseInt(thumbnailWidth);
	}
}
