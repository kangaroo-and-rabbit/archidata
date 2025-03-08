package org.kar.archidata.model;

import org.kar.archidata.annotation.ApiGenerationMode;
import org.kar.archidata.annotation.checker.AccessLimitation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@ApiGenerationMode(create = true, update = true)
public class GenericData extends GenericTiming {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique Id of the object", example = "123456")
	@AccessLimitation(creatable = false, updatable = false)
	public Long id = null;
}
