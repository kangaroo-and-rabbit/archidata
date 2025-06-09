package org.atriasoft.archidata.annotation.checker.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;

import jakarta.persistence.Id;

public class TableObjectGeneric {
	public TableObjectGeneric() {
		// nothing to do...
	}

	public TableObjectGeneric(final Object idOfTheObject, final List<Object> filedNameOfTheObject) {
		this.idOfTheObject = idOfTheObject;
		this.filedNameOfTheObject = filedNameOfTheObject;
	}

	@Id
	public Object idOfTheObject;

	@DataJson()
	public List<Object> filedNameOfTheObject;

}
