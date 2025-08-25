package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import jakarta.persistence.Id;

public class TableFieldUpdate {
	public TableFieldUpdate() {
		// nothing to do...
	}
	
	public TableFieldUpdate(final Object primaryKey, final Object fieldToUpdate) {
		this.primaryKey = primaryKey;
		this.fieldToUpdate = fieldToUpdate;
	}
	
	@Id
	public Object primaryKey;
	
	public Object fieldToUpdate;
	
}
