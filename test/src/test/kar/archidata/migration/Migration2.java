package test.kar.archidata.migration;

import org.kar.archidata.migration.MigrationSqlStep;

class Migration2 extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Second migration";
	}

	public Migration2() throws Exception {

		addAction("""
				ALTER TABLE `TestTableMigration`
					RENAME COLUMN `testDataMigration1` TO `testDataMigration2`
				""");
		display();
	}

}