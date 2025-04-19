package test.atriasoft.archidata.migration.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "TestTableMigration")
public class TypesMigrationInitialisationCurrent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;
	public Double testDataMigration2;
}
