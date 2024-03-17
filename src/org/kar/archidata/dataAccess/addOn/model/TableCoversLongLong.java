package org.kar.archidata.dataAccess.addOn.model;

import java.util.List;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

public class TableCoversLongLong extends GenericData {
	public TableCoversLongLong() {
		// nothing to do...
	}

	public TableCoversLongLong(final Long id, final List<Long> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Column(nullable = false)
	@Id
	public Long id;
	@DataJson()
	@Column(nullable = false)
	public List<Long> covers;

}
