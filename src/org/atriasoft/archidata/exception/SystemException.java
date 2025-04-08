package org.atriasoft.archidata.exception;

import jakarta.ws.rs.core.Response;

public class SystemException extends Exception {
	private static final long serialVersionUID = 1L;
	public final Response.Status status;

	public SystemException(final Response.Status status, final String message) {
		super(message);
		this.status = status;
	}

	public SystemException(final String message) {
		super(message);
		this.status = Response.Status.INTERNAL_SERVER_ERROR;
	}
}
