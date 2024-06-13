package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;

import org.kar.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversLongLong {
	public TableCoversLongLong() {
		// nothing to do...
	}

	public TableCoversLongLong(final Long id, final List<Long> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Id
	public Long id;

	@DataJson()
	public List<Long> covers;

}
