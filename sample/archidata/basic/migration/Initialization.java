package sample.archidata.basic.migration;

import org.atriasoft.archidata.migration.MigrationStep;

public class Initialization extends MigrationStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		// Add migration actions here using addAction(lambda)
	}

}
