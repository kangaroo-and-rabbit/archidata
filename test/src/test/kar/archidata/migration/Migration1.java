package test.kar.archidata.migration;

import org.kar.archidata.migration.MigrationSqlStep;

class Migration1 extends MigrationSqlStep {

	@Override
	public String getName() {
		return "first migratiion";
	}

	public Migration1() {

	}

	@Override
	public void generateStep() throws Exception {

		addAction("""
				ALTER TABLE `TestTableMigration`
					RENAME COLUMN `testData` TO `testDataMigration1`
				""");
		display();
	}

}