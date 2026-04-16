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

/**
 * Represents a user entity with login credentials, connection tracking, blocking capabilities, and cover images.
 */
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

	/**
	 * Gets the login name of this user.
	 * @return the login name
	 */
	public String getLogin() {
		return this.login;
	}

	/**
	 * Sets the login name of this user.
	 * @param login the login name to set
	 */
	public void setLogin(final String login) {
		this.login = login;
	}

	/**
	 * Gets the timestamp of the user's last connection.
	 * @return the last connection date
	 */
	public Date getLastConnection() {
		return this.lastConnection;
	}

	/**
	 * Sets the timestamp of the user's last connection.
	 * @param lastConnection the last connection date to set
	 */
	public void setLastConnection(final Date lastConnection) {
		this.lastConnection = lastConnection;
	}

	/**
	 * Gets the blocked state of this user.
	 * @return {@code true} if the user is blocked, {@code false} otherwise
	 */
	public Boolean getBlocked() {
		return this.blocked;
	}

	/**
	 * Sets the blocked state of this user.
	 * @param blocked {@code true} to block the user, {@code false} to unblock
	 */
	public void setBlocked(final Boolean blocked) {
		this.blocked = blocked;
	}

	/**
	 * Gets the reason why this user is blocked.
	 * @return the blocked reason, or {@code null} if not blocked
	 */
	public String getBlockedReason() {
		return this.blockedReason;
	}

	/**
	 * Sets the reason why this user is blocked.
	 * @param blockedReason the blocked reason to set
	 */
	public void setBlockedReason(final String blockedReason) {
		this.blockedReason = blockedReason;
	}

	/**
	 * Gets the list of cover image identifiers for this user.
	 * @return the list of cover ObjectIds
	 */
	public List<ObjectId> getCovers() {
		return this.covers;
	}

	/**
	 * Sets the list of cover image identifiers for this user.
	 * @param covers the list of cover ObjectIds to set
	 */
	public void setCovers(final List<ObjectId> covers) {
		this.covers = covers;
	}

	@Override
	public String toString() {
		return "User [login=" + this.login + ", last=" + this.lastConnection + ", blocked=" + this.blocked
				+ ", blockedReason=" + this.blockedReason + "]";
	}

}
