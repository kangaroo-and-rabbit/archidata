package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataAsyncHardDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDGenericDataSoftAsyncHardDelete extends OIDGenericDataSoftDelete {

	@DataNotRead
	@DataAsyncHardDeleted
	@Schema(description = "Deleted state", hidden = true)
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Boolean hardDeleted = null;

	public Boolean getHardDeleted() {
		return this.hardDeleted;
	}

	public void setHardDeleted(final Boolean hardDeleted) {
		this.hardDeleted = hardDeleted;
		if (Boolean.TRUE.equals(hardDeleted)) {
			setDeleted(true);
		}
	}
}
