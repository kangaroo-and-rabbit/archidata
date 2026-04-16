package org.atriasoft.archidata.exception;

import java.util.List;

import org.atriasoft.archidata.catcher.RestInputError;
import org.bson.types.ObjectId;

/**
 * Exception representing a structured REST API error response. Contains all
 * the fields needed to serialize a standardized error payload back to the client,
 * including a unique identifier, timestamp, HTTP status, and optional input errors.
 */
public class RESTErrorResponseException extends Exception {
	private static final long serialVersionUID = 1L;
	/** Unique identifier for this error occurrence. */
	public ObjectId oid;
	/** Timestamp of when the error occurred. */
	public String time;
	/** Short name or code identifying the error type. */
	public String name;
	/** Human-readable description of the error. */
	public String message;
	/** HTTP status code associated with this error. */
	public int status;
	/** Human-readable message corresponding to the HTTP status code. */
	public String statusMessage;
	/** List of input validation errors, may be {@code null} if not applicable. */
	public List<RestInputError> inputError;

	/**
	 * Constructs a default RESTErrorResponseException with a new unique identifier
	 * and all other fields set to their default values.
	 */
	public RESTErrorResponseException() {
		this.oid = new ObjectId();
		this.time = null;
		this.name = null;
		this.message = null;
		this.status = 0;
		this.statusMessage = null;
		this.inputError = null;
	}

	/**
	 * Constructs a fully specified RESTErrorResponseException.
	 * @param oid unique identifier for this error occurrence
	 * @param time timestamp of when the error occurred
	 * @param name short name or code identifying the error type
	 * @param message human-readable description of the error
	 * @param status HTTP status code associated with this error
	 * @param statusMessage human-readable message for the HTTP status code
	 * @param inputError list of input validation errors, or {@code null}
	 */
	public RESTErrorResponseException(final ObjectId oid, final String time, final String name, final String message,
			final int status, final String statusMessage, final List<RestInputError> inputError) {
		super(message);
		this.oid = oid;
		this.time = time;
		this.name = name;
		this.message = message;
		this.status = status;
		this.statusMessage = statusMessage;
		this.inputError = inputError;
	}

	/**
	 * Returns the error message stored in this exception.
	 * @return the human-readable error message
	 */
	@Override
	public String getMessage() {
		return this.message;
	}

	/**
	 * Returns a string representation of this error response, including
	 * the identifier, timestamp, name, message, status, and status message.
	 * @return a string representation of this exception
	 */
	@Override
	public String toString() {
		return "RESTErrorResponseExeption [oid=" + this.oid + ", time=" + this.time + ", name=" + this.name
				+ ", message=" + this.message + ", status=" + this.status + ", statusMessage=" + this.statusMessage
				+ "]";
	}

}
