package org.kar.archidata.model;

import org.kar.archidata.annotation.DataDefault;
import org.kar.archidata.annotation.DataDeleted;
import org.kar.archidata.annotation.DataNotRead;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

public class GenericDataSoftDelete extends GenericData {
	@DataNotRead
	@Column(nullable = false)
	@DataDefault("'0'")
	@DataDeleted
	@NotNull
	@Schema(description = "Deleted state", hidden = true, required = false, readOnly = true)
	public Boolean deleted = null;
}
