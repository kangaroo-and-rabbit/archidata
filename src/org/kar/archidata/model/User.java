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
import java.util.UUID;

import org.kar.archidata.annotation.DataIfNotExists;
import org.kar.archidata.annotation.DataJson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;

@Table(name = "user")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User extends GenericDataSoftDelete {
	@NotNull
	@Column(length = 128)
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[a-zA-Z0-9-_ \\.]+$")
	public String login = null;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public Timestamp lastConnection = null;

	@DefaultValue("'0'")
	@Column(nullable = false)
	@Nullable
	public Boolean blocked = false;
	@Column(length = 512)
	@Size(max = 512)
	public String blockedReason;

	@Schema(description = "List of Id of the specific covers")
	@DataJson(targetEntity = Data.class)
	@Nullable
	public List<UUID> covers;

	@Override
	public String toString() {
		return "User [login=" + this.login + ", last=" + this.lastConnection + ", blocked=" + this.blocked
				+ ", blockedReason=" + this.blockedReason + "]";
	}

}
