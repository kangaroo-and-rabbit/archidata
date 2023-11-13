package org.kar.archidata.model;

import org.kar.archidata.annotation.DataComment;
import org.kar.archidata.annotation.DataIfNotExists;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "data")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends GenericDataSoftDelete {

	@Column(length = 128, nullable = false)
	@DataComment("Sha512 of the data")
	public String sha512;
	@Column(length = 128, nullable = false)
	@DataComment("Mime -type of the media")
	public String mimeType;
	@Column(nullable = false)
	@DataComment("Size in Byte of the data")
	public Long size;
}
