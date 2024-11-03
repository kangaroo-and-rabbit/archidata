package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;

@Entity
public class SimpleTable extends GenericData {
	public String data;

}