package org.atriasoft.archidata.dataAccess.addOnSQL.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableCoversGeneric {
	public TableCoversGeneric() {
		// nothing to do...
	}

	public TableCoversGeneric(final Object idOfTheObject, final List<Object> filedNameOfTheObject) {
		this.idOfTheObject = idOfTheObject;
		this.filedNameOfTheObject = filedNameOfTheObject;
	}

	@Id
	public Object idOfTheObject;

	@DataJson()
	public List<Object> filedNameOfTheObject;

}
