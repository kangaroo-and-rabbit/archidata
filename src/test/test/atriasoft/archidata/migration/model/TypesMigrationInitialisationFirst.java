package test.atriasoft.archidata.migration.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Id;

@Collection("TestTableMigration")
public class TypesMigrationInitialisationFirst {
	@Id
	public Long id = null;
	public Double testData;
}