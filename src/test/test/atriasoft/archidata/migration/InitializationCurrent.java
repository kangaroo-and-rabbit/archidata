package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationSqlStep;

class InitializationCurrent extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	public InitializationCurrent() {

	}

	@Override
	public void generateStep() throws Exception {
		display();
	}
}