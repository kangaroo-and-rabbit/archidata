package org.atriasoft.archidata.exception;

import jakarta.ws.rs.core.Response;

public class UnAuthorizedException extends FailException {
	private static final long serialVersionUID = 1L;

	public UnAuthorizedException(final String message) {
		super(Response.Status.UNAUTHORIZED, message);
	}

	public UnAuthorizedException() {
		super(Response.Status.UNAUTHORIZED, "Not enought Right");
	}
}
