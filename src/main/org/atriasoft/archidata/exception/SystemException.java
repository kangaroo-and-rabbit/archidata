package org.atriasoft.archidata.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when an internal system error occurs that is not caused
 * by user input. Carries an HTTP status code for the REST error response,
 * defaulting to 500 Internal Server Error.
 */
public class SystemException extends Exception {
	private static final long serialVersionUID = 1L;
	/** The HTTP response status associated with this system error. */
	public final Response.Status status;

	/**
	 * Constructs a new SystemException with the specified HTTP status and message.
	 * @param status the HTTP response status to return
	 * @param message the detail message describing the system error
	 */
	public SystemException(final Response.Status status, final String message) {
		super(message);
		this.status = status;
	}

	/**
	 * Constructs a new SystemException with the default HTTP 500 Internal Server Error status.
	 * @param message the detail message describing the system error
	 */
	public SystemException(final String message) {
		super(message);
		this.status = Response.Status.INTERNAL_SERVER_ERROR;
	}
}
