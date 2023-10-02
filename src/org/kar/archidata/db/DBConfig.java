package org.kar.archidata.db;

import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBConfig {
	static final Logger LOGGER = LoggerFactory.getLogger(SqlWrapper.class);
    private final String type;
    private final String hostname;
    private final int port;
    private final String login;
    private final String password;
    private final String dbName;
    private final boolean keepConnected;

    public DBConfig(String type, String hostname, Integer port, String login, String password, String dbName, boolean keepConnected) {
    	if (type == null) {
    		this.type = "mysql";
    	} else {
    		this.type = type;
    	}
        if (hostname == null) {
            this.hostname = "localhost";
        } else {
            this.hostname = hostname;
        }
        if (port == null) {
            this.port = 3306;
        } else {
            this.port = port;
        }
        this.login = login;
        this.password = password;
        this.dbName = dbName;
        this.keepConnected = keepConnected;
    }

    @Override
    public String toString() {
        return "DBConfig{" +
                "type='" + type + '\'' +
                ", hostname='" + hostname + '\'' +
                ", port=" + port +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", dbName='" + dbName + '\'' +
                '}';
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getDbName() {
        return dbName;
    }
    public boolean getKeepConnected() {
        return keepConnected;
    }

    public String getUrl() {
    	return getUrl(false);
    }
    public String getUrl(boolean isRoot) {
    	if (type.equals("sqlite")) {
    		if (isRoot == true) {
    			LOGGER.error("Can not manage root connection on SQLite...");
    		}
    		if (this.hostname.equals("memory")) {
    			return "jdbc:sqlite::memory:";
    		}
    		return "jdbc:sqlite:" + this.hostname + ".db";
    	}
    	if (isRoot) {
    		return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port + "/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    	}
		return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port + "/" + this.dbName + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    }
}
