package org.atriasoft.archidata.exception;

import java.io.IOException;

/**
 * Exception that marks an error of the request body: the JSON sent by the caller cannot be parsed or does
 * not match the expected type. It is raised by
 * {@link org.atriasoft.archidata.catcher.RequestBodyReaderInterceptor} around the reading of the request
 * entity, with the original Jackson exception as cause, and answered with HTTP 400 by
 * {@link org.atriasoft.archidata.catcher.RequestBodyExceptionCatcher}.
 */
public class RequestBodyException extends IOException {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new RequestBodyException that wraps the error raised while reading the request body.
	 * @param cause the parsing or mapping error of the request body
	 */
	public RequestBodyException(final Throwable cause) {
		super(cause.getMessage(), cause);
	}
}
