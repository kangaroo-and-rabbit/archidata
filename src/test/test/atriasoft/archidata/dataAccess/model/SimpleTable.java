package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;

public class SimpleTable extends GenericData {
	@Column(length = 0)
	public String data;

}