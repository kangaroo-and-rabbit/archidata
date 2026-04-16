package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link JacksonException} instances
 * and converts them into standardized JSON error responses.
 */
public class JacksonExceptionCatcher implements ExceptionMapper<JacksonException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JacksonExceptionCatcher.class);

	/**
	 * Maps a {@link JacksonException} to an INTERNAL_SERVER_ERROR JSON error response.
	 * @param exception the JacksonException that was thrown during JSON processing
	 * @return a {@link Response} containing the error details as JSON with HTTP 500 status
	 */
	@Override
	public Response toResponse(final JacksonException exception) {
		LOGGER.warn("Catch exception Input data parsing:");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ret).type(MediaType.APPLICATION_JSON)
				.build();
	}

	/**
	 * Builds a RestErrorResponse object from the given exception.
	 *
	 * @param exception the Exception that was thrown
	 * @return a RestErrorResponse object containing the error details
	 */
	private RestErrorResponse build(final Exception exception) {
		return new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Catch JSON Exception",
				exception.getMessage());
	}

}
