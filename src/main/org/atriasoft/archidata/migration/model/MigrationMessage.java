package org.atriasoft.archidata.migration.model;

/**
 * A structured log message associated with a migration step.
 */
public class MigrationMessage {
	/** The step identifier this message relates to. */
	public Integer id;
	/** The log message content. */
	public String message;

	/**
	 * Default constructor for deserialization.
	 */
	public MigrationMessage() {}

	/**
	 * Constructs a migration message with the given step ID and message.
	 *
	 * @param id the step identifier
	 * @param message the log message content
	 */
	public MigrationMessage(final Integer id, final String message) {
		this.id = id;
		this.message = message;
	}

}
