package test.kar.archidata.migration;

import org.kar.archidata.migration.MigrationSqlStep;

import test.kar.archidata.migration.model.TypesMigrationInitialisationCurrent;

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