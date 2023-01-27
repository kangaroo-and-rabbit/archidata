package org.kar.archidata.model;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Token {
    public Long id;
    public Long userId;
    public String token;
    public String createTime;
    public String endValidityTime;

    public Token() {
    }

    public Token(long id, long userId, String token, String createTime, String endValidityTime) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.createTime = createTime;
        this.endValidityTime = endValidityTime;
    }

    public Token(ResultSet rs) {
        int iii = 1;
        try {
            this.id = rs.getLong(iii++);
            this.userId = rs.getLong(iii++);
            this.token = rs.getString(iii++);
            this.createTime = rs.getString(iii++);
            this.endValidityTime = rs.getString(iii++);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Token{" +
                "id=" + id +
                ", userId=" + userId +
                ", token='" + token + '\'' +
                ", createTime=" + createTime +
                ", endValidityTime=" + endValidityTime +
                '}';
    }
}
