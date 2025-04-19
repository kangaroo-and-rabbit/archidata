package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationSqlStep;

import test.atriasoft.archidata.migration.model.TypesMigrationInitialisationCurrent;

class InitializationCurrent extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	public InitializationCurrent() {

	}

	@Override
	public void generateStep() throws Exception {
		addClass(TypesMigrationInitialisationCurrent.class);
		addAction("""
				ALTER TABLE `TestTableMigration` AUTO_INCREMENT = 1000;
				""", "mysql");
		display();
	}
}