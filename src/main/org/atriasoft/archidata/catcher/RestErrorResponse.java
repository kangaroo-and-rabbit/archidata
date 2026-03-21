package org.atriasoft.archidata.catcher;

import java.time.Instant;
import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

/**
 * Represents a standardized REST error response returned to clients when an error occurs.
 * Contains error details such as status code, error name, message, timestamp, and optional input validation errors.
 */
@ApiGenerationMode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestErrorResponse {
	/** Unique identifier for this error occurrence, used for log correlation. */
	public ObjectId oid = new ObjectId();
	/** The error name or type identifier (mandatory for TypeScript generic error handling). */
	@NotNull
	@Column(length = 0)
	public String name; // Mandatory for TS generic error
	/** The human-readable error message (mandatory for TypeScript generic error handling). */
	@NotNull
	@Column(length = 0)
	public String message; // Mandatory for TS generic error
	/** The ISO-8601 timestamp indicating when the error occurred. */
	@NotNull
	@Column(length = 0)
	public String time;
	/** The HTTP status code associated with this error. */
	@NotNull
	final public int status;
	/** The human-readable HTTP status reason phrase. */
	@NotNull
	@Column(length = 0)
	final public String statusMessage;

	/** Optional list of input validation errors providing details about specific field failures. */
	@Nullable
	final public List<RestInputError> inputError;

	/**
	 * Constructs a REST error response with input validation errors.
	 * @param status the HTTP response status
	 * @param time the ISO-8601 timestamp of the error
	 * @param error the error name or type identifier
	 * @param message the human-readable error message
	 * @param inputError the list of input validation errors
	 */
	public RestErrorResponse(final Response.Status status, final String time, final String error, final String message,
			final List<RestInputError> inputError) {
		this.time = time;
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = inputError;
	}

	/**
	 * Constructs a REST error response without input validation errors.
	 * @param status the HTTP response status
	 * @param time the ISO-8601 timestamp of the error
	 * @param error the error name or type identifier
	 * @param message the human-readable error message
	 */
	public RestErrorResponse(final Response.Status status, final String time, final String error,
			final String message) {
		this.time = time;
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

	/**
	 * Constructs a REST error response with the current timestamp and no input validation errors.
	 * @param status the HTTP response status
	 * @param error the error name or type identifier
	 * @param message the human-readable error message
	 */
	public RestErrorResponse(final Response.Status status, final String error, final String message) {
		this.time = Instant.now().toString();
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

	/**
	 * Constructs a generic REST error response with default error name and empty message.
	 * @param status the HTTP response status
	 */
	public RestErrorResponse(final Response.Status status) {
		this.name = "generic";
		this.message = "";
		this.time = Instant.now().toString();
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

}
