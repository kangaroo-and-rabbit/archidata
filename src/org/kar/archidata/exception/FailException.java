package org.kar.archidata.exception;

import jakarta.ws.rs.core.Response;

public class FailException extends Exception {
	private static final long serialVersionUID = 1L;
	public final Response.Status status;

	public FailException(final Response.Status status, final String message) {
		super(message);
		this.status = status;
	}

	public FailException(final String message) {
		super(message);
		this.status = Response.Status.BAD_REQUEST;

	}
}
