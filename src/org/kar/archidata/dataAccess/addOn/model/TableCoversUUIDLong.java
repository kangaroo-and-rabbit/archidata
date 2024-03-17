package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class TableCoversUUIDLong extends GenericData {
	public TableCoversUUIDLong() {
		// nothing to do...
	}

	public TableCoversUUIDLong(final UUID id, final List<Long> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Column(nullable = false)
	@Id
	public UUID id;
	@DataJson()
	@Column(nullable = false)
	public List<Long> covers;

}
