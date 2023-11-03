package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.DataIfNotExists;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "applicationToken")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends GenericDataSoftDelete {
	@Column(nullable = false)
	public Long parentId;
	@Column(nullable = false)
	public String name;
	@Column(nullable = false)
	public Timestamp endValidityTime = null;
	@Column(nullable = false)
	public String token;
}
