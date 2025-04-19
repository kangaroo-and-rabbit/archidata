package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationSqlStep;

import test.atriasoft.archidata.migration.model.TypesMigrationInitialisationFirst;

class InitializationFirst extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	public InitializationFirst() {

	}

	@Override
	public void generateStep() throws Exception {
		addClass(TypesMigrationInitialisationFirst.class);
		addAction("""
				ALTER TABLE `TestTableMigration` AUTO_INCREMENT = 1000;
				""", "mysql");
		display();
	}
}