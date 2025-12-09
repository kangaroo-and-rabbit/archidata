package test.atriasoft.archidata.migration;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.migration.MigrationSqlStep;

class MigrationFail extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Fail migration Test";
	}

	public MigrationFail() {

	}

	@Override
	public void generateStep() throws Exception {

		addAction((final DBAccessMongo da) -> {
			throw new IOException("FAIL migration");
		});
		display();
	}

}