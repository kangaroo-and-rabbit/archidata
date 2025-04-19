package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.annotation.apiGenerator.ApiNotNull;
import org.atriasoft.archidata.annotation.checker.ReadOnlyField;

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
	@ReadOnlyField
	@ApiNotNull
	@ApiAccessLimitation(creatable = false, updatable = false)
	public Long id = null;
}
