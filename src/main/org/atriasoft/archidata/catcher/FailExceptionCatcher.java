package org.atriasoft.archidata.catcher;

import org.atriasoft.archidata.exception.FailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link FailException} instances
 * and converts them into standardized JSON error responses.
 */
public class FailExceptionCatcher implements ExceptionMapper<FailException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FailExceptionCatcher.class);

	/**
	 * Maps a {@link FailException} to a JSON error response using the exception's HTTP status.
	 * @param exception the FailException that was thrown
	 * @return a {@link Response} containing the error details as JSON
	 */
	@Override
	public Response toResponse(final FailException exception) {
		LOGGER.warn("Catch FailException: {}", exception.getLocalizedMessage());
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		if (exception.exception != null) {
			LOGGER.error("FailException cause: {}", exception.exception.getMessage(), exception.exception);
		}
		return Response.status(exception.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final FailException exception) {
		return new RestErrorResponse(exception.status, "Request Fail", exception.getMessage());
	}

}
