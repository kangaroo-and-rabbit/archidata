package org.atriasoft.archidata.bean.exception;

/**
 * Exception thrown when an error occurs during bean introspection,
 * such as type conflicts, inaccessible members, or lambda generation failures.
 */
public class IntrospectionException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new IntrospectionException with the specified detail message.
	 * @param message the detail message describing the introspection error
	 */
	public IntrospectionException(final String message) {
		super(message);
	}

	/**
	 * Constructs a new IntrospectionException with the specified detail message and cause.
	 * @param message the detail message describing the introspection error
	 * @param cause the underlying cause of this exception
	 */
	public IntrospectionException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
