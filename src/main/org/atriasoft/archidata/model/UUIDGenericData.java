package org.atriasoft.archidata.model;

import java.util.UUID;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.apiGenerator.ApiNotNull;
import org.atriasoft.archidata.annotation.checker.ReadOnlyField;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.ws.rs.DefaultValue;

@ApiGenerationMode(create = true, update = true)
public class UUIDGenericData extends GenericTiming {
	@Id
	@DefaultValue("(UUID_TO_BIN(UUID(), TRUE))")
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique UUID of the object", example = "e6b33c1c-d24d-11ee-b616-02420a030102")
	@ReadOnlyField
	@ApiNotNull
	@ApiAccessLimitation(creatable = false, updatable = false)
	public UUID uuid = null;
}
