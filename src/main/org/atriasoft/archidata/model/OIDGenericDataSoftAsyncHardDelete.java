package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataAsyncHardDeleted;
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

/**
 * Extends {@link OIDGenericDataSoftDelete} with asynchronous hard-delete support.
 * Records are first soft-deleted, then asynchronously hard-deleted in a background process.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDGenericDataSoftAsyncHardDelete extends OIDGenericDataSoftDelete {

	@DataNotRead
	@Column(nullable = false)
	@DataAsyncHardDeleted
	@Schema(description = "Deleted state", hidden = true)
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Boolean hardDeleted = null;

	/**
	 * Gets the hard-delete state of this object.
	 * @return {@code true} if the object is marked for hard deletion, {@code null} if not set
	 */
	public Boolean getHardDeleted() {
		return this.hardDeleted;
	}

	/**
	 * Sets the hard-delete state of this object. Setting to {@code true} also sets the soft-delete flag.
	 * @param hardDeleted {@code true} to mark for hard deletion, {@code false} otherwise
	 */
	public void setHardDeleted(final Boolean hardDeleted) {
		this.hardDeleted = hardDeleted;
		if (Boolean.TRUE.equals(hardDeleted)) {
			setDeleted(true);
		}
	}
}
