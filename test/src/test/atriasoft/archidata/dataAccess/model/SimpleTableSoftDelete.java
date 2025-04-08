package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericDataSoftDelete;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;

@Entity
public class SimpleTableSoftDelete extends GenericDataSoftDelete {
	@Column(length = 0)
	public String data;
}