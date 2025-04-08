package org.atriasoft.archidata.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Token {
	public Long id;
	public Long userId;
	public String token;
	public String createTime;
	public String endValidityTime;

	public Token() {}

	public Token(final long id, final long userId, final String token, final String createTime,
			final String endValidityTime) {
		this.id = id;
		this.userId = userId;
		this.token = token;
		this.createTime = createTime;
		this.endValidityTime = endValidityTime;
	}

	public Token(final ResultSet rs) {
		int iii = 1;
		try {
			this.id = rs.getLong(iii++);
			this.userId = rs.getLong(iii++);
			this.token = rs.getString(iii++);
			this.createTime = rs.getString(iii++);
			this.endValidityTime = rs.getString(iii++);
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Token{" + "id=" + this.id + ", userId=" + this.userId + ", token='" + this.token + '\''
				+ ", createTime=" + this.createTime + ", endValidityTime=" + this.endValidityTime + '}';
	}
}
