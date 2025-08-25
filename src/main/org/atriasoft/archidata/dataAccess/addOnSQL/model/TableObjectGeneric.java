package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import jakarta.persistence.Id;

public class TableObjectGeneric {
	public TableObjectGeneric() {
		// nothing to do...
	}
	
	public TableObjectGeneric(final Object primaryKey, final Object fieldToUpdate) {
		this.primaryKey = primaryKey;
		this.fieldToUpdate = fieldToUpdate;
	}
	
	@Id
	public Object primaryKey;
	
	public Object fieldToUpdate;
	
}
