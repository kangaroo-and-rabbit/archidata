package org.kar.archidata.model;

import org.kar.archidata.annotation.DataDeleted;
import org.kar.archidata.annotation.DataNotRead;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.ws.rs.DefaultValue;

public class UUIDGenericDataSoftDelete extends UUIDGenericData {
	@DataNotRead
	@Column(nullable = false)
	@DefaultValue("'0'")
	@DataDeleted
	@Schema(description = "Deleted state", hidden = true, required = false, readOnly = true)
	@Nullable
	public Boolean deleted = null;
}
