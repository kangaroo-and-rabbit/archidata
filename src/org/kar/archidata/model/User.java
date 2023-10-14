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
import java.util.List;

import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.sqlWrapper.Foreign;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "user")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends GenericTable {
	@Column(length = 128)
	public String login = null;
	
	public Timestamp lastConnection = null;
	@SQLDefault("'0'")
	@Column(nullable = false)
	public boolean admin = false;
	@SQLDefault("'0'")
	@Column(nullable = false)
	public boolean blocked = false;
	@SQLDefault("'0'")
	@Column(nullable = false)
	public boolean removed = false;

	@ManyToOne(fetch = FetchType.LAZY)
	public List<Foreign<Data>> covers;

	@Override
	public String toString() {
		return "User [login=" + this.login + ", last=" + this.lastConnection + ", admin=" + this.admin + "]";
	}
	
}
