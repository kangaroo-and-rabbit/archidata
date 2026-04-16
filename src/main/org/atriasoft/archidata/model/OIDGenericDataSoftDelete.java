package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.ws.rs.DefaultValue;

/**
 * Extends {@link OIDGenericData} with soft-delete support. Records are marked as deleted rather than being physically removed from the database.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDGenericDataSoftDelete extends OIDGenericData {

	@DataNotRead
	@Column(nullable = false)
	@DefaultValue("'0'")
	@DataDeleted
	@Schema(description = "Deleted state", hidden = true)
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Boolean deleted = null;

	/**
	 * Gets the soft-delete state of this object.
	 * @return {@code true} if the object is marked as deleted, {@code null} if not set
	 */
	public Boolean getDeleted() {
		return this.deleted;
	}

	/**
	 * Sets the soft-delete state of this object.
	 * @param deleted {@code true} to mark as deleted, {@code false} otherwise
	 */
	public void setDeleted(final Boolean deleted) {
		this.deleted = deleted;
	}
}
