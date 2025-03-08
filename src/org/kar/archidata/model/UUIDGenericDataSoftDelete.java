package org.kar.archidata.model;

import org.kar.archidata.annotation.ApiGenerationMode;
import org.kar.archidata.annotation.DataDeleted;
import org.kar.archidata.annotation.DataNotRead;
import org.kar.archidata.annotation.checker.ApiAccessLimitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.ws.rs.DefaultValue;

@ApiGenerationMode(create = true, update = true)
public class UUIDGenericDataSoftDelete extends UUIDGenericData {
	@DataNotRead
	@Column(nullable = false)
	@DefaultValue("'0'")
	@DataDeleted
	@Schema(description = "Deleted state", hidden = true)
	@Nullable
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Boolean deleted = null;
}
