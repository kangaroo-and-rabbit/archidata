package org.atriasoft.archidata.exception;

import jakarta.ws.rs.core.Response;

/**
 * Exception thrown when a request lacks valid authentication credentials
 * or the authenticated user does not have sufficient permissions.
 * Always maps to HTTP 401 Unauthorized status.
 */
public class UnAuthorizedException extends FailException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new UnAuthorizedException with the specified detail message.
	 * @param message the detail message describing the authorization failure
	 */
	public UnAuthorizedException(final String message) {
		super(Response.Status.UNAUTHORIZED, message);
	}

	/**
	 * Constructs a new UnAuthorizedException with a default message indicating insufficient rights.
	 */
	public UnAuthorizedException() {
		super(Response.Status.UNAUTHORIZED, "Not enought Right");
	}
}
