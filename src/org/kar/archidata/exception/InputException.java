package org.kar.archidata.exception;

import jakarta.ws.rs.core.Response;

public class InputException extends Exception {
	private static final long serialVersionUID = 1L;
	public final String missingVariable;
	public final Response.Status status;
	
	public InputException(Response.Status status, String variable, String message) {
		super(message);
		this.missingVariable = variable;
		this.status = status;
	}
	
	public InputException(String variable, String message) {
		super(message);
		this.missingVariable = variable;
		this.status = Response.Status.NOT_ACCEPTABLE;
	}
}
