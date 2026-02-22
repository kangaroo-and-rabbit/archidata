package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationStep;

class InitializationCurrent extends MigrationStep {

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