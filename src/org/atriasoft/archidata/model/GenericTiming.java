package org.atriasoft.archidata.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.checker.ReadOnlyField;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;

@ApiGenerationMode(create = true, update = true)
public class GenericTiming {
	@DataNotRead
	@CreationTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "Create time of the object", example = "2000-01-23T01:23:45.678+01:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Nullable
	@ReadOnlyField
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "When update the object", example = "2000-01-23T00:23:45.678Z")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Nullable
	@ReadOnlyField
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Date updatedAt = null;
}
