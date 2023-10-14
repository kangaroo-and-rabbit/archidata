package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBEntry implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DBEntry.class);
	public DBConfig config;
	public Connection connection;
	private static List<DBEntry> stored = new ArrayList<>();
	
	private DBEntry(DBConfig config, boolean root) throws IOException {
		this.config = config;
		if (root) {
			connectRoot();
		} else {
			connect();
		}
	}
	
	public static DBEntry createInterface(DBConfig config) throws IOException {
		return createInterface(config, false);
	}
	
	public static DBEntry createInterface(DBConfig config, boolean root) throws IOException {
		if (config.getKeepConnected()) {
			for (DBEntry elem : stored) {
				if (elem == null) {
					continue;
				}
				if (elem.config.getUrl().equals(config.getUrl())) {
					return elem;
				}
			}
			DBEntry tmp = new DBEntry(config, root);
			stored.add(tmp);
			return tmp;
		} else {
			return new DBEntry(config, root);
		}
	}
	
	public void connectRoot() throws IOException {
		try {
			connection = DriverManager.getConnection(config.getUrl(true), config.getLogin(), config.getPassword());
		} catch (SQLException ex) {
			throw new IOException("Connection db fail: " + ex.getMessage());
		}
		
	}
	
	public void connect() throws IOException {
		try {
			connection = DriverManager.getConnection(config.getUrl(), config.getLogin(), config.getPassword());
		} catch (SQLException ex) {
			throw new IOException("Connection db fail: " + ex.getMessage());
		}
		
	}
	
	@Override
	public void close() throws IOException {
		if (config.getKeepConnected()) {
			return;
		}
		try {
			//connection.commit();
			connection.close();
		} catch (SQLException ex) {
			throw new IOException("Dis-connection db fail: " + ex.getMessage());
		}
		
	}
}
