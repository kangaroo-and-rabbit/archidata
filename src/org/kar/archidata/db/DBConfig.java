package org.kar.archidata.db;

public class DBConfig {
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
    	if (type.equals("sqlite")) {
    		if (this.hostname.equals("memory")) {
    			return "jdbc:sqlite::memory:";
    		}
    		return "jdbc:sqlite:" + this.hostname + ".db";
    	}
		return "jdbc:" + this.type + "://" + this.hostname + ":" + this.port + "/" + this.dbName + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    }
}
