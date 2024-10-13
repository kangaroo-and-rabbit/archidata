package test.kar.archidata.migration;

import java.io.IOException;

import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.migration.MigrationSqlStep;

class MigrationFail extends MigrationSqlStep {

	@Override
	public String getName() {
		return "Fail migration Test";
	}

	public MigrationFail() {

	}

	@Override
	public void generateStep() throws Exception {

		addAction((final DataAccess da) -> {
			throw new IOException("FAIL migration");
		});
		display();
	}

}