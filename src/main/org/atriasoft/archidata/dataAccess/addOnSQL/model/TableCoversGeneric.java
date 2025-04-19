package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversGeneric {
	public TableCoversGeneric() {
		// nothing to do...
	}

	public TableCoversGeneric(final Object id, final List<Object> covers) {
		this.id = id;
		this.covers = covers;
	}

	@Id
	public Object id;

	@DataJson()
	public List<Object> covers;

}
