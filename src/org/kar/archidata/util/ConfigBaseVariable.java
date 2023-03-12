package org.kar.archidata.util;

public class ConfigBaseVariable {
    static public String tmpDataFolder = System.getenv("DATA_TMP_FOLDER");
    static public String dataFolder = System.getenv("DATA_FOLDER");
    static public String dbType = System.getenv("DB_TYPE");
    static public String dbHost = System.getenv("DB_HOST");
    static public String dbPort = System.getenv("DB_PORT");
    static public String dbUser = System.getenv("DB_USER");
    static public String dbPassword = System.getenv("DB_PASSWORD");
    static public String bdDatabase = System.getenv("DB_DATABASE");
    static public String apiAdress = System.getenv("API_ADDRESS");
    static public String ssoAdress = System.getenv("SSO_ADDRESS");
    static public String ssoToken = System.getenv("SSO_TOKEN");
    
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
}
