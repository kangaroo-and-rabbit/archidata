package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;

@Entity
public class SimpleTable extends GenericData {
	@Column(length = 0)
	public String data;

}