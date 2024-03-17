package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class TableCoversUUIDUUID extends GenericData {
	public TableCoversUUIDUUID() {
		// nothing to do...
	}

	public TableCoversUUIDUUID(final UUID id, final List<UUID> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Column(nullable = false)
	@Id
	public UUID id;
	@DataJson()
	@Column(nullable = false)
	public List<UUID> covers;

}
