package test.atriasoft.archidata.migration;

import org.atriasoft.archidata.migration.MigrationStep;

class Migration1 extends MigrationStep {

	@Override
	public String getName() {
		return "first migratiion";
	}

	public Migration1() {

	}

	@Override
	public void generateStep() throws Exception {
		addAction((final org.atriasoft.archidata.dataAccess.DBAccessMongo da) -> {
			// Migration step placeholder (was SQL: ALTER TABLE RENAME COLUMN)
		});
		display();
	}

}