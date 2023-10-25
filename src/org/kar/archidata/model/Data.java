package org.kar.archidata.model;

import org.kar.archidata.annotation.SQLComment;
import org.kar.archidata.annotation.SQLIfNotExists;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "data")
@SQLIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends GenericDataSoftDelete {
	
	@Column(length = 128, nullable = false)
	@SQLComment("Sha512 of the data")
	public String sha512;
	@Column(length = 128, nullable = false)
	@SQLComment("Mime -type of the media")
	public String mimeType;
	@Column(nullable = false)
	@SQLComment("Size in Byte of the data")
	public Long size;
}
