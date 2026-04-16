package org.atriasoft.archidata.exception;

/**
 * Exception thrown when an error occurs during data access operations,
 * such as database reads, writes, or query execution failures.
 */
public class DataAccessException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new DataAccessException with the specified detail message.
	 * @param message the detail message describing the data access error
	 */
	public DataAccessException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new DataAccessException with the specified detail message and cause.
	 * @param message the detail message describing the data access error
	 * @param cause the underlying cause of this exception
	 */
	public DataAccessException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
