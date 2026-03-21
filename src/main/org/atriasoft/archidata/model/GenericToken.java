package org.atriasoft.archidata.model;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an application token stored in the database. Contains the token string, its validity period, and a reference to the parent entity.
 */
@Table(name = "applicationToken")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends OIDGenericDataSoftDelete {
	@NotNull
	private ObjectId parentId;
	@NotNull
	private String name;
	@NotNull
	private Date endValidity = null;
	@NotNull
	private String token;

	/**
	 * Gets the parent entity identifier.
	 * @return the parent ObjectId
	 */
	public ObjectId getParentId() {
		return this.parentId;
	}

	/**
	 * Sets the parent entity identifier.
	 * @param parentId the parent ObjectId to set
	 */
	public void setParentId(final ObjectId parentId) {
		this.parentId = parentId;
	}

	/**
	 * Gets the name of this token.
	 * @return the token name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name of this token.
	 * @param name the token name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the end validity date of this token.
	 * @return the expiration date
	 */
	public Date getEndValidity() {
		return this.endValidity;
	}

	/**
	 * Sets the end validity date of this token.
	 * @param endValidity the expiration date to set
	 */
	public void setEndValidity(final Date endValidity) {
		this.endValidity = endValidity;
	}

	/**
	 * Gets the token string value.
	 * @return the token string
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * Sets the token string value.
	 * @param token the token string to set
	 */
	public void setToken(final String token) {
		this.token = token;
	}
}
