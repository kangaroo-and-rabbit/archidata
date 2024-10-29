package org.kar.archidata.dataAccess.addOnSQL.model;

import java.util.UUID;

import org.kar.archidata.model.UUIDGenericDataSoftDelete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

public class LinkTableUUIDLong extends UUIDGenericDataSoftDelete {
	public LinkTableUUIDLong() {
		// nothing to do...
	}

	public LinkTableUUIDLong(final UUID object1Id, final long object2Id) {
		this.object1Id = object1Id;
		this.object2Id = object2Id;
	}

	@Schema(description = "Object reference 1")
	@Column(nullable = false)
	public UUID object1Id;
	@Schema(description = "Object reference 2")
	@Column(nullable = false)
	public Long object2Id;

}
