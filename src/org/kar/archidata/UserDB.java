package org.kar.archidata;

import org.kar.archidata.db.DBEntry;
import org.kar.archidata.model.User;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDB {

    public UserDB() {
    }

    public static User getUsers(long userId) throws Exception {
    	return SqlWrapper.get(User.class, userId);
    	/*
        DBEntry entry = new DBEntry(WebLauncher.dbConfig);
        String query = "SELECT * FROM user WHERE id = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User out = new User(rs);
                entry.disconnect();
                return out;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
        return null;
        */
    }

    
    public static User getUserOrCreate(long userId, String userLogin) throws Exception {
        User user = getUsers(userId);
        if (user != null) {
        	/*
            boolean blocked = false;
            boolean removed = false;
            if (user.email != userOAuth.email || user.login != userOAuth.login || user.blocked != blocked || user.removed != removed) {
                updateUsersInfoFromOAuth(userOAuth.id, userOAuth.email, userOAuth.login, blocked, removed);
            } else {
                updateUsersConnectionTime(userOAuth.id);
            }
            return getUsers(userOAuth.id);
            */
        	return user;
        }
        createUsersInfoFromOAuth(userId, userLogin);
        return getUsers(userId);
    }
/*
    private static void updateUsersConnectionTime(long userId) {
        DBEntry entry = new DBEntry(WebLauncher.dbConfig);
        String query = "UPDATE `user` SET `lastConnection`=now(3) WHERE `id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
    }

    private static void updateUsersInfoFromOAuth(long userId, String email, String login, boolean blocked, boolean removed) {
        DBEntry entry = new DBEntry(WebLauncher.dbConfig);
        String query = "UPDATE `user` SET `login`=?, `email`=?, `lastConnection`=now(3), `blocked`=?, `removed`=? WHERE id = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setString(2, email);
            ps.setString(3, blocked ? "TRUE" : "FALSE");
            ps.setString(4, removed ? "TRUE" : "FALSE");
            ps.setLong(5, userId);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
    }
    */

    private static void createUsersInfoFromOAuth(long userId, String login) {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "INSERT INTO `user` (`id`, `login`, `lastConnection`, `admin`, `blocked`, `removed`) VALUE (?,?,now(3),'0','0','0')";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, userId);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
    }

}
































