package org.kar.archidata.dataAccess.addOnSQL.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversUUIDUUID {
	public TableCoversUUIDUUID() {
		// nothing to do...
	}

	public TableCoversUUIDUUID(final UUID uuid, final List<UUID> covers) {
		this.uuid = uuid;
		this.covers = covers;
	}

	@Id
	public UUID uuid;
	@DataJson()
	public List<UUID> covers;

}
