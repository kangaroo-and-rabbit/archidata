package org.atriasoft.archidata.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

/**
 * General-purpose exception for REST API failures. Carries an HTTP response
 * status and an optional wrapped exception. Subclasses such as
 * {@link UnAuthorizedException} specialize this for specific failure modes.
 */
public class FailException extends Exception {
	private static final Logger LOGGER = LoggerFactory.getLogger(FailException.class);
	private static final long serialVersionUID = 1L;
	/** The HTTP response status associated with this failure. */
	public final Response.Status status;
	/** An optional underlying exception that caused this failure, may be {@code null}. */
	public final Exception exception;

	/**
	 * Constructs a new FailException with the specified HTTP status and message.
	 * @param status the HTTP response status to return
	 * @param message the detail message describing the failure
	 */
	public FailException(final Response.Status status, final String message) {
		super(message);
		this.status = status;
		this.exception = null;
	}

	/**
	 * Constructs a new FailException with the specified HTTP status, message, and cause.
	 * The cause is logged at error level.
	 * @param status the HTTP response status to return
	 * @param message the detail message describing the failure
	 * @param ex the underlying exception that caused this failure
	 */
	public FailException(final Response.Status status, final String message, final Exception ex) {
		super(message);
		this.status = status;
		this.exception = ex;
		LOGGER.error("Generate Fail exception with exceptionData: {}", ex.getMessage(), ex);
	}

	/**
	 * Constructs a new FailException with the default HTTP 400 Bad Request status.
	 * @param message the detail message describing the failure
	 */
	public FailException(final String message) {
		super(message);
		this.status = Response.Status.BAD_REQUEST;
		this.exception = null;
	}
}
