package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class TableCoversLongUUID extends GenericData {
	public TableCoversLongUUID() {
		// nothing to do...
	}

	public TableCoversLongUUID(final Long id, final List<UUID> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Column(nullable = false)
	@Id
	public Long id;
	@DataJson()
	@Column(nullable = false)
	public List<UUID> covers;

}
