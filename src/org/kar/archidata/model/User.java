package org.kar.archidata.model;

/*
CREATE TABLE `user` (
  `id` bigint NOT NULL COMMENT 'table ID' AUTO_INCREMENT PRIMARY KEY,
  `login` varchar(128) COLLATE 'utf8_general_ci' NOT NULL COMMENT 'login of the user',
  `email` varchar(512) COLLATE 'utf8_general_ci' NOT NULL COMMENT 'email of the user',
  `lastConnection` datetime NOT NULL COMMENT 'last connection time',
  `admin` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE',
  `blocked` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE',
  `removed` enum("TRUE", "FALSE") NOT NULL DEFAULT 'FALSE',
  `avatar` bigint DEFAULT NULL,
) AUTO_INCREMENT=10;

 */

import java.sql.Timestamp;

import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.SQLLimitSize;
import org.kar.archidata.annotation.SQLNotNull;
import org.kar.archidata.annotation.SQLTableName;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@SQLTableName ("user")
@SQLIfNotExists
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class User extends GenericTable {
    @SQLLimitSize(128)
    public String login = null;
    
    public Timestamp lastConnection = null;
    @SQLDefault("'0'")
    @SQLNotNull
    public boolean admin = false;
    @SQLDefault("'0'")
    @SQLNotNull
    public boolean blocked = false;
    @SQLDefault("'0'")
    @SQLNotNull
    public boolean removed = false;
	@Override
	public String toString() {
		return "User [login=" + login + ", last=" + lastConnection + ", admin=" + admin + "]";
	}

}
