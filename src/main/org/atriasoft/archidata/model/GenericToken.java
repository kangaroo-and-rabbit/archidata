package org.atriasoft.archidata.model;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

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

	public ObjectId getParentId() {
		return this.parentId;
	}

	public void setParentId(final ObjectId parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Date getEndValidity() {
		return this.endValidity;
	}

	public void setEndValidity(final Date endValidity) {
		this.endValidity = endValidity;
	}

	public String getToken() {
		return this.token;
	}

	public void setToken(final String token) {
		this.token = token;
	}
}
