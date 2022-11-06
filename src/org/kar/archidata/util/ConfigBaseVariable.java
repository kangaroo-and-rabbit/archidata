package org.kar.archidata.util;

public class ConfigBaseVariable {

    public static String getTmpDataFolder() {
        String out = System.getenv("DATA_TMP_FOLDER");
        if (out == null) {
            return "/application/data/tmp";
        }
        return out;
    }

    public static String getMediaDataFolder() {
        String out = System.getenv("DATA_FOLDER");
        if (out == null) {
            return "/application/data/media";
        }
        return out;
    }
    
    public static String getDBHost() {
        String out = System.getenv("DB_HOST");
        if (out == null) {
            return "localhost";
        }
        return out;
    }

    public static String getDBPort() {
        String out = System.getenv("DB_PORT");
        if (out == null) {
            return "80";
            //return "17036";
        }
        return out;
    }

    public static String getDBLogin() {
        String out = System.getenv("DB_USER");
        if (out == null) {
            return "root";
        }
        return out;
    }

    public static String getDBPassword() {
        String out = System.getenv("DB_PASSWORD");
        if (out == null) {
            return "archidata_password";
        }
        return out;
    }

    public static String getDBName() {
        String out = System.getenv("DB_DATABASE");
        if (out == null) {
            return "unknown";
        }
        return out;
    }

    public static String getlocalAddress() {
        String out = System.getenv("API_ADDRESS");
        if (out == null) {
            return "http://0.0.0.0:80/api/";
        }
        return out;
    }
    
    public static String getSSOAddress() {
        String out = System.getenv("SSO_ADDRESS");
        if (out == null) {
            return "http://sso_host/karauth/api/";
            //return "http://192.168.1.156/karauth/api/";
        }
        return out;
    }
}
