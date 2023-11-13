package org.kar.archidata.dataAccess.addOn.model;

import org.kar.archidata.annotation.DataComment;
import org.kar.archidata.model.GenericDataSoftDelete;

import jakarta.persistence.Column;

public class LinkTable extends GenericDataSoftDelete {
	public LinkTable() {
		// nothing to do...
	}

	public LinkTable(final long object1Id, final long object2Id) {
		this.object1Id = object1Id;
		this.object2Id = object2Id;
	}

	@DataComment("Object reference 1")
	@Column(nullable = false)
	public Long object1Id;
	@DataComment("Object reference 2")
	@Column(nullable = false)
	public Long object2Id;

}
