package org.kar.archidata.model;

import java.util.Date;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;

public class GenericTiming {
	@DataNotRead
	@CreationTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "Create time of the object", required = false, example = "2000-01-23T01:23:45.678+01:00", readOnly = true)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@Nullable
	public Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Column(nullable = false, insertable = false, updatable = false)
	@Schema(description = "When update the object", required = false, example = "2000-01-23T00:23:45.678Z", readOnly = true)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	// public Instant updatedAt = null;
	@Nullable
	public Date updatedAt = null;
}
