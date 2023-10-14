package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.SQLIfNotExists;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "applicationToken")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends GenericTable {
	@Column(nullable = false)
	public Long parentId;
	@Column(nullable = false)
	public String name;
	@Column(nullable = false)
	public Timestamp endValidityTime = null;
	@Column(nullable = false)
	public String token;
}
