package org.kar.archidata.exception;

import jakarta.ws.rs.core.Response;

public class FailException extends Exception {
	private static final long serialVersionUID = 1L;
	public final Response.Status status;
	
	public FailException(Response.Status status, String message) {
		super(message);
		this.status = status;
	}
	
	public FailException(String message) {
		super(message);
		this.status = Response.Status.BAD_REQUEST;
		
	}
}
