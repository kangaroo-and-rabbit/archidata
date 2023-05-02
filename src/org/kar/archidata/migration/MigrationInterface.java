package org.kar.archidata.migration;

import org.kar.archidata.db.DBEntry;

public interface MigrationInterface {
	/**
	 * Get Name of the migration
	 * @return Migration name
	 */
	String getName();
	/**
	 * Migrate the system to a new version.
	 * @param entry DB interface for the migration.
	 * @param log Stored data in the BDD for the migration progression.
	 * @return true if migration is finished.
	 */
	boolean applyMigration(DBEntry entry, StringBuilder log);
	/**
	 * Remove a migration the system to the previous version.
	 * @param entry DB interface for the migration.
	 * @param log Stored data in the BDD for the migration progression.
	 * @return true if migration is finished.
	 */
	boolean revertMigration(DBEntry entry, StringBuilder log);
}
