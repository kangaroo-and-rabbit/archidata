package org.kar.archidata.exception;

import jakarta.ws.rs.core.Response;

public class InputException extends Exception {
	private static final long serialVersionUID = 1L;
	public final String missingVariable;
	public final Response.Status status;

	public InputException(final Response.Status status, final String variable, final String message) {
		super(message);
		this.missingVariable = variable;
		this.status = status;
	}

	public InputException(final String variable, final String message) {
		super(message);
		this.missingVariable = variable;
		this.status = Response.Status.BAD_REQUEST;
	}
}
