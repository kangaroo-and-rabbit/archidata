package org.atriasoft.archidata.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when input validation fails, such as missing or invalid
 * request parameters. Carries the name of the offending variable and an
 * HTTP status code for the REST error response.
 */
public class InputException extends Exception {
	private static final long serialVersionUID = 1L;
	/** The name of the variable that caused the validation failure. */
	public final String missingVariable;
	/** The HTTP response status associated with this input error. */
	public final Response.Status status;

	/**
	 * Constructs a new InputException with a specific HTTP status, variable name, and message.
	 * @param status the HTTP response status to return
	 * @param variable the name of the variable that failed validation
	 * @param message the detail message describing the validation error
	 */
	public InputException(final Response.Status status, final String variable, final String message) {
		super(message);
		this.missingVariable = variable;
		this.status = status;
	}

	/**
	 * Constructs a new InputException with the default HTTP 400 Bad Request status.
	 * @param variable the name of the variable that failed validation
	 * @param message the detail message describing the validation error
	 */
	public InputException(final String variable, final String message) {
		super(message);
		this.missingVariable = variable;
		this.status = Response.Status.BAD_REQUEST;
	}
}
