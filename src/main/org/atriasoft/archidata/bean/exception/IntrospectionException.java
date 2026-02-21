package org.atriasoft.archidata.bean.exception;

/**
 * Exception thrown when an error occurs during bean introspection,
 * such as type conflicts, inaccessible members, or lambda generation failures.
 */
public class IntrospectionException extends Exception {

	private static final long serialVersionUID = 1L;

	public IntrospectionException(final String message) {
		super(message);
	}

	public IntrospectionException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
