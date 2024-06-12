package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericDataSoftDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class TableCoversUUIDUUID extends GenericDataSoftDelete {
	public TableCoversUUIDUUID() {
		// nothing to do...
	}

	public TableCoversUUIDUUID(final UUID uuid, final List<UUID> covers) {
		this.uuid = uuid;
		this.covers = covers;
	}

	@Column(nullable = false)
	@Id
	public UUID uuid;
	@DataJson()
	@Column(nullable = false)
	public List<UUID> covers;

}
