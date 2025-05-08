package org.atriasoft.archidata.model;

import java.util.UUID;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.ws.rs.DefaultValue;

public class UUIDGenericData extends GenericTiming {
	@Id
	@DefaultValue("(UUID_TO_BIN(UUID(), TRUE))")
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique UUID of the object", example = "e6b33c1c-d24d-11ee-b616-02420a030102")
	@NotNull(groups = { GroupRead.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public UUID uuid = null;
}
