package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link JacksonException} instances
 * and converts them into standardized JSON error responses with HTTP 500.
 * <p>
 * The JSON errors of the request body do not reach it: {@link RequestBodyReaderInterceptor} wraps them in a
 * {@link org.atriasoft.archidata.exception.RequestBodyException}, answered with HTTP 400 by
 * {@link RequestBodyExceptionCatcher}.
 */
public class JacksonExceptionCatcher implements ExceptionMapper<JacksonException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JacksonExceptionCatcher.class);

	/** Default constructor used by the JAX-RS provider framework. */
	public JacksonExceptionCatcher() {}

	/**
	 * Maps a {@link JacksonException} to a JSON error response. A raw Jackson exception comes from a processing
	 * done by the server itself (an upstream response, a stored document, a serialization), so it is a server
	 * error.
	 * @param exception the JacksonException that was thrown during JSON processing
	 * @return a {@link Response} containing the error details as JSON with HTTP 500 status
	 */
	@Override
	public Response toResponse(final JacksonException exception) {
		// The Jackson message names Java classes and lists the properties of the entity: log it only.
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
				"Catch JSON Exception", ExceptionCatcher.GENERIC_MESSAGE);
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(ret.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

}
