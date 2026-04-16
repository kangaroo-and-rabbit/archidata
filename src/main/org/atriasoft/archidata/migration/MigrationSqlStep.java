package org.atriasoft.archidata.migration;

/**
 * Deprecated migration step class originally named for SQL.
 *
 * @deprecated Use {@link MigrationStep} instead. This class was named for SQL
 *             but the project uses MongoDB.
 */
@Deprecated
public class MigrationSqlStep extends MigrationStep {

	/** Default constructor for the deprecated SQL migration step. */
	public MigrationSqlStep() {
		// default constructor
	}

}
