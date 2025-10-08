package org.atriasoft.archidata.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.DataIfNotExists;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Table(name = "applicationToken")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends OIDGenericDataSoftDelete {
	@NotNull
	public ObjectId parentId;
	@NotNull
	public String name;
	@NotNull
	public Date endValidity = null;
	@NotNull
	public String token;
}
