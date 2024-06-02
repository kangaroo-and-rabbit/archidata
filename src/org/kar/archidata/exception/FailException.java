package org.kar.archidata.exception;

import org.kar.archidata.api.DataResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

public class FailException extends Exception {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataResource.class);
	private static final long serialVersionUID = 1L;
	public final Response.Status status;
	public final Exception exception;

	public FailException(final Response.Status status, final String message) {
		super(message);
		this.status = status;
		this.exception = null;
	}

	public FailException(final Response.Status status, final String message, final Exception ex) {
		super(message);
		this.status = status;
		this.exception = ex;
		ex.printStackTrace();
		LOGGER.error("Generate Fail exception with exceptionData: {}", ex.getMessage());
	}

	public FailException(final String message) {
		super(message);
		this.status = Response.Status.BAD_REQUEST;
		this.exception = null;
	}
}
