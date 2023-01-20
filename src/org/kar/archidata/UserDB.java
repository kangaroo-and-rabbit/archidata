package org.kar.archidata;

import org.kar.archidata.db.DBEntry;
import org.kar.archidata.model.User;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserDB {

    public UserDB() {
    }

    public static User getUsers(long userId) throws Exception {
    	return SqlWrapper.get(User.class, userId);
    }

    
    public static User getUserOrCreate(long userId, String userLogin) throws Exception {
        User user = getUsers(userId);
        if (user != null) {
        	return user;
        }
        createUsersInfoFromOAuth(userId, userLogin);
        return getUsers(userId);
    }

    private static void createUsersInfoFromOAuth(long userId, String login) throws IOException {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "INSERT INTO `user` (`id`, `login`, `lastConnection`, `admin`, `blocked`, `removed`) VALUE (?,?,now(3),'0','0','0')";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, userId);
            ps.setString(2, login);
            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
        	entry.close();
        }
    }

}
