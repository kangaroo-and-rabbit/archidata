package org.kar.archidata;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.options.DBInterfaceOption;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.model.User;

public class UserDB {
	
	public UserDB() {}
	
	public static User getUsers(final long userId) throws Exception {
		return DataAccess.get(User.class, userId);
	}
	
	public static User getUserOrCreate(final long userId, final String userLogin) throws Exception {
		final User user = getUsers(userId);
		if (user != null) {
			return user;
		}
		createUsersInfoFromOAuth(userId, userLogin);
		return getUsers(userId);
	}
	
	private static void createUsersInfoFromOAuth(final long userId, final String login) throws IOException {
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final String query = "INSERT INTO `user` (`id`, `login`, `lastConnection`, `admin`, `blocked`, `removed`) VALUE (?,?,now(3),'0','0','0')";
		try {
			final PreparedStatement ps = entry.connection.prepareStatement(query);
			ps.setLong(1, userId);
			ps.setString(2, login);
			ps.executeUpdate();
		} catch (final SQLException throwables) {
			throwables.printStackTrace();
		} finally {
			entry.close();
		}
	}
}
