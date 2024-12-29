package org.kar.archidata.dataAccess.addOnSQL.model;

import java.util.UUID;

import org.kar.archidata.model.UUIDGenericDataSoftDelete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

public class LinkTableUUIDUUID extends UUIDGenericDataSoftDelete {
	public LinkTableUUIDUUID() {
		// nothing to do...
	}

	public LinkTableUUIDUUID(final UUID object1Id, final UUID object2Id) {
		this.object1Id = object1Id;
		this.object2Id = object2Id;
	}

	@Schema(description = "Object reference 1")
	@Column(nullable = false)
	public UUID object1Id;
	@Schema(description = "Object reference 2")
	@Column(nullable = false)
	public UUID object2Id;

}
