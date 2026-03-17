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

	public Boolean getDeleted() {
		return this.deleted;
	}

	public void setDeleted(final Boolean deleted) {
		this.deleted = deleted;
	}
}
