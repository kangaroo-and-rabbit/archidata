package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationSqlStep;

class InitializationFirst extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	public InitializationFirst() {

	}

	@Override
	public void generateStep() throws Exception {
		display();
	}
}