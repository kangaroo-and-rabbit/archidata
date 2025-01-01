package org.kar.archidata.model;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Id;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

public class OIDGenericData extends GenericTiming {
	@Id
	//@jakarta.persistence.Id
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique ObjectID of the object", required = false, readOnly = true, example = "65161616841351")
	@NotNull
	public ObjectId _id = null;
}
