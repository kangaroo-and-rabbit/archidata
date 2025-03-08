package org.kar.archidata.catcher;

import java.time.Instant;

import org.bson.types.ObjectId;
import org.kar.archidata.annotation.ApiGenerationMode;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

@ApiGenerationMode
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

	public RestErrorResponse(final Response.Status status, final String time, final String error,
			final String message) {
		this.time = time;
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}

	public RestErrorResponse(final Response.Status status, final String error, final String message) {
		this.time = Instant.now().toString();
		this.name = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}

	public RestErrorResponse(final Response.Status status) {
		this.name = "generic";
		this.message = "";
		this.time = Instant.now().toString();
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}

}
