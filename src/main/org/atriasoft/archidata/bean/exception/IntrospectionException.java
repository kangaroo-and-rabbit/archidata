package org.atriasoft.archidata.bean.exception;

/**
 * Exception thrown when an error occurs during bean introspection,
 * such as type conflicts, inaccessible members, or a failed instantiation. A failure to generate an accessor
 * lambda is not thrown: the factory logs it at debug level and falls back to a {@code MethodHandle}.
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
