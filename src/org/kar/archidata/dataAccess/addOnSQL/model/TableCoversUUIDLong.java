package org.kar.archidata.dataAccess.addOnSQL.model;

import java.util.ArrayList;
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
		this.covers = new ArrayList<>(covers);
	}

	@Id
	public UUID uuid;
	@DataJson()
	public List<Long> covers;

}
