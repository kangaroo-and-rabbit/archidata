package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.ws.rs.DefaultValue;

public class GenericDataSoftDelete extends GenericData {
	@DataNotRead
	@Column(nullable = false)
	@DefaultValue("'0'")
	@DataDeleted
	@Schema(description = "Deleted state", hidden = true)
	@NotNull(groups = { GroupRead.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public Boolean deleted = null;
}
