package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericDataSoftDelete;

import jakarta.persistence.Column;

public class SimpleTableSoftDelete extends GenericDataSoftDelete {
	@Column(length = 0)
	public String data;
}