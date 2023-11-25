package org.kar.archidata.tools;

public class ConfigBaseVariable {
	static public String tmpDataFolder;
	static public String dataFolder;
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

	// For test only
	public static void clearAllValue() {
		tmpDataFolder = System.getenv("DATA_TMP_FOLDER");
		dataFolder = System.getenv("DATA_FOLDER");
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
	}

	static {
		clearAllValue();
	}

	public static String getTmpDataFolder() {
		if (tmpDataFolder == null) {
			return "/application/data/tmp";
		}
		return tmpDataFolder;
	}

	public static String getMediaDataFolder() {
		if (dataFolder == null) {
			return "/application/data/media";
		}
		return dataFolder;
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

	public static String getDBPort() {
		if (dbPort == null) {
			return "3306";
		}
		return dbPort;
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
		if (bdDatabase == null) {
			return "unknown";
		}
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
}
