package test.kar.archidata.migration;

import org.kar.archidata.migration.MigrationSqlStep;

class MigrationFail extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Fail migration Test";
	}

	public MigrationFail() throws Exception {

		addAction("""
				ALTER TABLE `TestTableMigrationqs`
					RENAME COLUMN `testDataMisqdgration1` TO `testDataMiqsdgration2`
				""");
		display();
	}

}