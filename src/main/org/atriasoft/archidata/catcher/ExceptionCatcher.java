package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches any unhandled {@link Exception} as a fallback
 * and converts it into a standardized JSON error response with HTTP 500 status.
 */
public class ExceptionCatcher implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatcher.class);

	/**
	 * Maps an unhandled {@link Exception} to an INTERNAL_SERVER_ERROR JSON error response.
	 * @param exception the Exception that was thrown
	 * @return a {@link Response} containing the error details as JSON with HTTP 500 status
	 */
	@Override
	public Response toResponse(final Exception exception) {
		LOGGER.warn("Catch exception (not managed...):");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ret).type(MediaType.APPLICATION_JSON)
				.build();
	}

	private RestErrorResponse build(final Exception exception) {
		return new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error",
				exception.getMessage());
	}

}
