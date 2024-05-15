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

import org.kar.archidata.annotation.DataIfNotExists;
import org.kar.archidata.annotation.DataJson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.ws.rs.DefaultValue;

@Table(name = "user")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends GenericDataSoftDelete {
	@Column(length = 128)
	public String login = null;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public Timestamp lastConnection = null;
	@DefaultValue("'0'")
	@Column(nullable = false)
	public boolean admin = false;
	@DefaultValue("'0'")
	@Column(nullable = false)
	public boolean blocked = false;
	@DefaultValue("'0'")
	@Column(nullable = false)
	public boolean removed = false;

	@Schema(description = "List of Id of the specific covers")
	@DataJson(targetEntity = Data.class)
	public List<Long> covers;

	@Override
	public String toString() {
		return "User [login=" + this.login + ", last=" + this.lastConnection + ", admin=" + this.admin + "]";
	}

}
