package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationStep;

class InitializationFirst extends MigrationStep {

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