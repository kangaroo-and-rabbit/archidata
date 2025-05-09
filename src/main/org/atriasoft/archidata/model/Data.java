package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataIfNotExists;
import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;

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
	@ApiAccessLimitation(creatable = false, updatable = false)
	public String sha512;
	@Column(length = 128, nullable = false)
	@Schema(description = "Mime -type of the media")
	@Size(max = 512)
	@ApiAccessLimitation(creatable = false, updatable = false)
	public String mimeType;
	@Column(nullable = false)
	@Schema(description = "Size in Byte of the data")
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Long size;
}
