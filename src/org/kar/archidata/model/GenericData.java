package org.kar.archidata.model;

import org.bson.types.ObjectId;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class GenericData extends GenericTiming {
	@dev.morphia.annotations.Id
	private ObjectId _id;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	@Schema(description = "Unique Id of the object", required = false, readOnly = true, example = "123456")
	public Long id = null;
}
