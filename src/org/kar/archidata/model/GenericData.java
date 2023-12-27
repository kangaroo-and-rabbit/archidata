package org.kar.archidata.model;

import java.util.Date;

import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataNotRead;
import org.kar.archidata.annotation.UpdateTimestamp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

public class GenericData {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique Id of the object", required = false, readOnly = true)
	public Long id = null;
	@DataNotRead
	@CreationTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull
	@Schema(description = "Create time of the object", required = false, example = "2000-01-23T01:23:45.678+01:00", readOnly = true)
	public Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@NotNull
	@Schema(description = "When update the object", required = false, example = "2000-01-23T00:23:45.678Z", readOnly = true)
	public Date updatedAt = null;
}
