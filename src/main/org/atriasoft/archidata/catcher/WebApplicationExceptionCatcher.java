package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link WebApplicationException} instances
 * and converts them into standardized JSON error responses.
 */
public class WebApplicationExceptionCatcher implements ExceptionMapper<WebApplicationException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionCatcher.class);

	/**
	 * Maps a {@link WebApplicationException} to a JSON error response, preserving the original HTTP status code.
	 * @param exception the WebApplicationException that was thrown
	 * @return a {@link Response} containing the error details as JSON
	 */
	@Override
	public Response toResponse(final WebApplicationException exception) {
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		return Response.status(exception.getResponse().getStatusInfo().toEnum()).entity(ret)
				.type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final WebApplicationException exception) {
		LOGGER.error("WebApplicationException caught: {}", exception.getMessage(), exception);
		return new RestErrorResponse(exception.getResponse().getStatusInfo().toEnum(), "Catch system exception",
				exception.getMessage());
	}

}
