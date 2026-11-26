package org.atriasoft.archidata.migration;

/**
 * Deprecated migration step class originally named for SQL.
 *
 * @deprecated Use {@link MigrationStep} instead: it is the same class. This one was named for SQL
 *             but the project uses MongoDB, and it is not used by archidata.
 */
@Deprecated(since = "0.41.0", forRemoval = true)
public class MigrationSqlStep extends MigrationStep {

	/** Default constructor for the deprecated SQL migration step. */
	public MigrationSqlStep() {
		// default constructor
	}

}
