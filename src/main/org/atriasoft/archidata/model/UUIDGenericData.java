package org.atriasoft.archidata.model;

import java.util.UUID;

import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Base data model with a {@link UUID} primary key. Extends {@link GenericTiming} to include creation and update timestamps.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UUIDGenericData extends GenericTiming {

	/** Creates an empty {@link UUIDGenericData} instance. */
	public UUIDGenericData() {}

	@Id
	@Schema(description = "Unique UUID of the object", example = "e6b33c1c-d24d-11ee-b616-02420a030102")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private UUID uuid = null;

	/**
	 * Gets the UUID of this object.
	 * @return the UUID
	 */
	public UUID getUuid() {
		return this.uuid;
	}

	/**
	 * Sets the UUID of this object.
	 * @param uuid the UUID to set
	 */
	public void setUuid(final UUID uuid) {
		this.uuid = uuid;
	}
}
