package org.kar.archidata.catcher;

import java.time.Instant;
import java.util.UUID;

import org.kar.archidata.tools.UuidUtils;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

public class RestErrorResponse {
	@NotNull
	public UUID uuid = UuidUtils.nextUUID();
	@NotNull
	public String name; // Mandatory for TS generic error
	@NotNull
	public String message; // Mandatory for TS generic error
	@NotNull
	public String time;
	@NotNull
	final public int status;
	@NotNull
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
