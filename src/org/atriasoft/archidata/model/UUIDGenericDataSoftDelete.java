package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.checker.ReadOnlyField;

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
	@ReadOnlyField
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Boolean deleted = null;
}
