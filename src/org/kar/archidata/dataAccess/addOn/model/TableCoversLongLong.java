package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericDataSoftDelete;

import jakarta.persistence.Column;

public class TableCoversLongLong extends GenericDataSoftDelete {
	public TableCoversLongLong() {
		// nothing to do...
	}

	public TableCoversLongLong(final Long id, final List<Long> covers) {
		this.id = id;
		this.covers = covers;
	}

	@DataJson()
	@Column(nullable = false)
	public List<Long> covers;

}
