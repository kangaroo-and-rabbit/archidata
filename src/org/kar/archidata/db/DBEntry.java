package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;

public class DBEntry implements Closeable {
    public DBConfig config;
    public Connection connection;

    public DBEntry(DBConfig config) throws IOException {
        this.config = config;
        connect();
    }

    public void connect() throws IOException {
        try {
            connection = DriverManager.getConnection(config.getUrl(), config.getLogin(), config.getPassword());
        } catch (SQLException ex) {
            throw new IOException("Connection db fail: " + ex.getMessage());
        }

    }
/*
    public void test() throws SQLException {
        String query = "SELECT * FROM user";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        System.out.println("List of user:");
        if (rs.next()) {
            User user = new User(rs);
            System.out.println("    - " + user);
        }
    }
   */

	@Override
	public void close() throws IOException {
        try {
            //connection.commit();
            connection.close();
        } catch (SQLException ex) {
            throw new IOException("Dis-connection db fail: " + ex.getMessage());
        }
		
	}
}
