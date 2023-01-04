package org.kar.archidata.exception;

import javax.ws.rs.core.Response;

public class NotFoundException extends FailException {
	private static final long serialVersionUID = 1L;
	public NotFoundException(String message) {
		super(Response.Status.NOT_FOUND, message);
	}
}
