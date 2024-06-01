package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericDataSoftDelete;

import jakarta.persistence.Column;

public class TableCoversLongUUID extends GenericDataSoftDelete {
	public TableCoversLongUUID() {
		// nothing to do...
	}

	public TableCoversLongUUID(final Long id, final List<UUID> covers) {
		this.id = id;
		this.covers = covers;
	}

	@DataJson()
	@Column(nullable = false)
	public List<UUID> covers;

}
