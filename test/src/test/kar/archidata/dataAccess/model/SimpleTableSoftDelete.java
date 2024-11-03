package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericDataSoftDelete;

import dev.morphia.annotations.Entity;

@Entity
public class SimpleTableSoftDelete extends GenericDataSoftDelete {
	public String data;
}