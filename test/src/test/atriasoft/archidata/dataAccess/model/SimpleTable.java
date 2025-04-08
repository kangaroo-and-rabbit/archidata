package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;

@Entity
public class SimpleTable extends GenericData {
	@Column(length = 0)
	public String data;

}