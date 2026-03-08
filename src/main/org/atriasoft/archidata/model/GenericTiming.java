package org.atriasoft.archidata.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

//@ApiGenerationMode(create = true, update = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericTiming {

	@DataNotRead
	@CreationTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "Create time of the object", example = "2000-01-23T01:23:45.678+01:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "When update the object", example = "2000-01-23T00:23:45.678Z")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date updatedAt = null;

	public Date getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(final Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return this.updatedAt;
	}

	public void setUpdatedAt(final Date updatedAt) {
		this.updatedAt = updatedAt;
	}
}
