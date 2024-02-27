package org.kar.archidata.model;

import org.kar.archidata.annotation.DataDeleted;
import org.kar.archidata.annotation.DataNotRead;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;

public class GenericDataSoftDelete extends GenericData {
	@DataNotRead
	@Column(nullable = false)
	@DefaultValue("'0'")
	@DataDeleted
	@NotNull
	@Schema(description = "Deleted state", hidden = true, required = false, readOnly = true)
	public Boolean deleted = null;
}
