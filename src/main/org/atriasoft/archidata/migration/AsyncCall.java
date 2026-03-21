package org.atriasoft.archidata.migration;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;

/**
 * Functional interface representing an asynchronous database operation
 * used as an individual step within a {@link MigrationStep}.
 */
public interface AsyncCall {
	/**
	 * Executes a database request as part of a migration step.
	 *
	 * @param da the database access interface
	 * @throws Exception if the request fails
	 */
	void doRequest(DBAccessMongo da) throws Exception;
}
