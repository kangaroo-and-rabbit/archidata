package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

@Table(name = "data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends OIDGenericDataSoftDelete {
	@Column(length = 128, nullable = false)
	@Schema(description = "Sha512 of the data")
	@Size(max = 512)
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public String sha512;
	@Column(length = 128, nullable = false)
	@Schema(description = "Mime -type of the media")
	@Size(max = 512)
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public String mimeType;
	@Column(nullable = false)
	@Schema(description = "Size in Byte of the data")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public Long size;
}
