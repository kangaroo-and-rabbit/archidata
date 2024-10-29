package org.kar.archidata.dataAccess.addOnSQL.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversLongUUID {
	public TableCoversLongUUID() {
		// nothing to do...
	}

	public TableCoversLongUUID(final Long id, final List<UUID> covers) {
		this.id = id;
		this.covers = new ArrayList<>(covers);
	}

	@Id
	public Long id;

	@DataJson()
	public List<UUID> covers;

}
