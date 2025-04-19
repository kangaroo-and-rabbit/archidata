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

@ApiGenerationMode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestErrorResponse {
	public ObjectId oid = new ObjectId();
	@NotNull
	@Column(length = 0)
	public String name; // Mandatory for TS generic error
	@NotNull
	@Column(length = 0)
	public String message; // Mandatory for TS generic error
	@NotNull
	@Column(length = 0)
	public String time;
	@NotNull
	final public int status;
	@NotNull
	@Column(length = 0)
	final public String statusMessage;

	@Nullable
	final public List<RestInputError> inputError;

	public RestErrorResponse(final Response.Status status, final String time, final String error, final String message,
			final List<RestInputError> inputError) {
		this.time = time;
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = inputError;
	}

	public RestErrorResponse(final Response.Status status, final String time, final String error,
			final String message) {
		this.time = time;
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

	public RestErrorResponse(final Response.Status status, final String error, final String message) {
		this.time = Instant.now().toString();
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

	public RestErrorResponse(final Response.Status status) {
		this.name = "generic";
		this.message = "";
		this.time = Instant.now().toString();
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
		this.inputError = null;
	}

}
