package test.kar.archidata.migration;

import org.kar.archidata.migration.MigrationSqlStep;

import test.kar.archidata.migration.model.TypesMigrationInitialisationFirst;

class InitializationFirst extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	public InitializationFirst() throws Exception {
		addClass(TypesMigrationInitialisationFirst.class);
		addAction("""
				ALTER TABLE `TestTableMigration` AUTO_INCREMENT = 1000;
				""", "mysql");
		display();
	}
}