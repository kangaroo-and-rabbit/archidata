package org.kar.archidata.dataAccess.addOn.model;

import org.kar.archidata.model.GenericData;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;

public class LinkTable extends GenericData {
	public LinkTable() {
		// nothing to do...
	}

	public LinkTable(final long object1Id, final long object2Id) {
		this.object1Id = object1Id;
		this.object2Id = object2Id;
	}

	@Schema(description = "Object reference 1")
	@Column(nullable = false)
	public Long object1Id;
	@Schema(description = "Object reference 2")
	@Column(nullable = false)
	public Long object2Id;

}
