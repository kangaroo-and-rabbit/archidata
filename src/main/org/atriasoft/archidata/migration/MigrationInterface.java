package org.atriasoft.archidata.migration;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.migration.model.Migration;

/**
 * Interface defining a database migration that can be applied by the {@link MigrationEngine}.
 */
public interface MigrationInterface {
	/** Get Name of the migration
	 * @return Migration name */
	String getName();

	/** Migrate the system to a new version.
	 * @param entry DB interface for the migration.
	 * @param migration Migration post data on each step...
	 * @return true if migration is finished.
	 * @throws Exception if the migration fails */
	boolean applyMigration(DBAccessMongo entry, Migration migration) throws Exception;

	/** Get the number of step in the migration process.
	 * @return count of migration steps.
	 * @throws Exception if step counting fails */
	int getNumberOfStep() throws Exception;
}
