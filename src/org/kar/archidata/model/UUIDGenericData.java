package org.kar.archidata.model;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;

public class UUIDGenericData extends GenericTiming {
	@Id
	@DefaultValue("(UUID_TO_BIN(UUID(), TRUE))")
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique UUID of the object", required = false, readOnly = true, example = "e6b33c1c-d24d-11ee-b616-02420a030102")
	@NotNull
	public UUID uuid = null;
}
