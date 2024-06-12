package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversUUIDLong {
	public TableCoversUUIDLong() {
		// nothing to do...
	}

	public TableCoversUUIDLong(final UUID uuid, final List<Long> covers) {
		this.uuid = uuid;
		this.covers = covers;
	}

	@Id
	public UUID uuid;
	@DataJson()
	public List<Long> covers;

}
