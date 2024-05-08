package org.kar.archidata.model;

import java.sql.Timestamp;

import org.kar.archidata.annotation.DataIfNotExists;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "applicationToken")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericToken extends GenericDataSoftDelete {
	@Column(nullable = false)
	public Long parentId;
	@Column(nullable = false, length = 0)
	public String name;
	@Column(nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public Timestamp endValidityTime = null;
	@Column(nullable = false, length = 0)
	public String token;
}
