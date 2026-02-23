package org.atriasoft.archidata.model;

import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.GroupWithContext;
import org.atriasoft.archidata.annotation.checker.UniqueInBaseId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.DefaultValue;

@Table(name = "user")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiGenerationMode(create = true, update = true)
public class User extends OIDGenericDataSoftDelete {
	@NotNull
	@Column(length = 128)
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[a-zA-Z0-9-_ \\.]+$")
	@UniqueInBaseId(target = User.class, nameOfField = "login", groups = GroupWithContext.class)
	private String login = null;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date lastConnection = null;

	@DefaultValue("'0'")
	@Column(nullable = false)
	private Boolean blocked = false;
	@Column(length = 512)
	@Size(max = 512)
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	private String blockedReason;

	@Schema(description = "List of Id of the specific covers")
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private List<ObjectId> covers;

	public String getLogin() {
		return this.login;
	}

	public void setLogin(final String login) {
		this.login = login;
	}

	public Date getLastConnection() {
		return this.lastConnection;
	}

	public void setLastConnection(final Date lastConnection) {
		this.lastConnection = lastConnection;
	}

	public Boolean getBlocked() {
		return this.blocked;
	}

	public void setBlocked(final Boolean blocked) {
		this.blocked = blocked;
	}

	public String getBlockedReason() {
		return this.blockedReason;
	}

	public void setBlockedReason(final String blockedReason) {
		this.blockedReason = blockedReason;
	}

	public List<ObjectId> getCovers() {
		return this.covers;
	}

	public void setCovers(final List<ObjectId> covers) {
		this.covers = covers;
	}

	@Override
	public String toString() {
		return "User [login=" + this.login + ", last=" + this.lastConnection + ", blocked=" + this.blocked
				+ ", blockedReason=" + this.blockedReason + "]";
	}

}
