package org.atriasoft.archidata.migration;

/**
 * Exception thrown when an error occurs during database migration,
 * such as a failed schema change or an invalid migration step.
 */
public class MigrationException extends Exception {

	private static final long serialVersionUID = 20230502L;

	/**
	 * Constructs a new MigrationException with the specified detail message.
	 * @param message the detail message describing the migration error
	 */
	public MigrationException(final String message) {
		super(message);
	}
}
