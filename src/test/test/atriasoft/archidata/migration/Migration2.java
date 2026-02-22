package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationStep;

class Migration2 extends MigrationStep {

	@Override
	public String getName() {
		return "Second migration";
	}

	public Migration2() {

	}

	@Override
	public void generateStep() throws Exception {
		addAction((final org.atriasoft.archidata.dataAccess.DBAccessMongo da) -> {
			// Migration step placeholder (was SQL: ALTER TABLE RENAME COLUMN)
		});
		display();
	}

}