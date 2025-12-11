package org.atriasoft.archidata.migration;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.migration.model.Migration;

public interface MigrationInterface {
	/** Get Name of the migration
	 * @return Migration name */
	String getName();

	/** Migrate the system to a new version.
	 * @param entry DB interface for the migration.
	 * @param migration Migration post data on each step...
	 * @return true if migration is finished. */
	boolean applyMigration(DBAccessMongo entry, Migration migration) throws Exception;

	/** Get the number of step in the migration process.
	 * @return count of SQL access. */
	int getNumberOfStep() throws Exception;
}
