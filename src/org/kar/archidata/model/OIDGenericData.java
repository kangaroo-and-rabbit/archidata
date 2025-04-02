package org.kar.archidata.model;

import org.bson.types.ObjectId;
import org.kar.archidata.annotation.apiGenerator.ApiAccessLimitation;
import org.kar.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.kar.archidata.annotation.apiGenerator.ApiNotNull;
import org.kar.archidata.annotation.checker.ReadOnlyField;

import dev.morphia.annotations.Id;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

@ApiGenerationMode(create = true, update = true)
public class OIDGenericData extends GenericTiming {
	@Id
	@jakarta.persistence.Id
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the object", example = "65161616841351")
	@ReadOnlyField
	@ApiNotNull
	@ApiAccessLimitation(creatable = false, updatable = false)
	public ObjectId oid = null;
}
