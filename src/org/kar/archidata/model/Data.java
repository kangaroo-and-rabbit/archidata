package org.kar.archidata.model;

import org.kar.archidata.annotation.DataIfNotExists;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Table(name = "data")
@DataIfNotExists
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends OIDGenericDataSoftDelete {
	@Column(length = 128, nullable = false)
	@Schema(description = "Sha512 of the data")
	@Size(max = 512)
	public String sha512;
	@Column(length = 128, nullable = false)
	@Schema(description = "Mime -type of the media")
	@Size(max = 512)
	public String mimeType;
	@Column(nullable = false)
	@Schema(description = "Size in Byte of the data")
	public Long size;
}
