package sample.archidata.basic.migration;

import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.migration.MigrationStep;

import sample.archidata.basic.model.Task;

/**
 * First migration, run once on an empty database.
 *
 * <p>When the database is empty, {@code MigrationEngine} runs ONLY this step, then records the last
 * registered migration as already applied. The initialization must therefore build the state of
 * the CURRENT version of the model, not the state of version 1.
 */
public class Initialization extends MigrationStep {

	// The name is what the engine stores in the KAR_migration collection: never rename it once
	// deployed (the default is the canonical class name).
	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		// One addAction() per atomic operation: the engine logs the progress after each one.
		// Collections need no creation, MongoDB creates them on the first insert.
		addAction((final DBAccessMongo da) -> {
			da.insertMultiple(List.of(//
					new Task("Read the archidata README", "doc/index.md lists every topic"), //
					new Task("Write a second migration", "See doc/migration.md")));
		});
	}
}
