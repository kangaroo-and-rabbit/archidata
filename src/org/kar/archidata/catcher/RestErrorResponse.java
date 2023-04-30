package org.kar.archidata.catcher;

import java.time.Instant;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

public class RestErrorResponse {
	public UUID uuid = UUID.randomUUID();
	public String time; 
	public String error;
	public String message;
	final public int status;
	final public String statusMessage;
	
	public RestErrorResponse(Response.Status status, String time, String error, String message) {
		this.time = time;
		this.error = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}
	public RestErrorResponse(Response.Status status, String error, String message) {
		this.time = Instant.now().toString();
		this.error = error;
		this.message = message;
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}
	public RestErrorResponse(Response.Status status) {
		this.time = Instant.now().toString();
		this.status = status.getStatusCode();
		this.statusMessage = status.getReasonPhrase();
	}
	
}
