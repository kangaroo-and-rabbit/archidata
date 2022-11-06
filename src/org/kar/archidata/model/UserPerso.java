package org.kar.archidata.model;

/*
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT 'table ID' AUTO_INCREMENT PRIMARY KEY,
  `login` varchar(128) COLLATE 'utf8_general_ci' NOT NULL COMMENT 'login of the user',
  `email` varchar(512) COLLATE 'utf8_general_ci' NOT NULL COMMENT 'email of the user',
  `lastConnection` datetime NOT NULL COMMENT 'last connection time',
  `admin` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE',
  `blocked` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE',
  `removed` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE'
) AUTO_INCREMENT=10;

 */


public class UserPerso {
    public Long id;
    public String login;
    public boolean admin;
    public boolean blocked;
    public boolean removed;

    public UserPerso(User other) {
        this.id = other.id;
        this.login = other.login;
        this.admin = other.admin;
        this.blocked = other.blocked;
        this.removed = other.removed;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", admin=" + admin +
                ", blocked=" + blocked +
                ", removed=" + removed +
                '}';
    }
}
