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
 * <p>
 * The message of a client error (4xx) is sent back; the message of a server error (5xx) is replaced by
 * {@link ExceptionCatcher#GENERIC_MESSAGE} and only logged.
 * </p>
 */
public class WebApplicationExceptionCatcher implements ExceptionMapper<WebApplicationException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionCatcher.class);

	/** Default constructor used by the JAX-RS provider framework. */
	public WebApplicationExceptionCatcher() {}

	/**
	 * Maps a {@link WebApplicationException} to a JSON error response, preserving the original HTTP status code.
	 * @param exception the WebApplicationException that was thrown
	 * @return a {@link Response} containing the error details as JSON
	 */
	@Override
	public Response toResponse(final WebApplicationException exception) {
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		return Response.status(exception.getResponse().getStatusInfo()).entity(ret).type(MediaType.APPLICATION_JSON)
				.build();
	}

	private RestErrorResponse build(final WebApplicationException exception) {
		LOGGER.error("WebApplicationException caught: {}", exception.getMessage(), exception);
		// Not toEnum(): it is null for a code that Response.Status does not list (499, 599...).
		final Response.StatusType status = exception.getResponse().getStatusInfo();
		// A 4xx message describes the request (NotFoundException("...")); a 5xx one may describe the server
		// (an InternalServerErrorException wrapping an IOException): it only goes to the log.
		final String message = status.getFamily() == Response.Status.Family.SERVER_ERROR
				? ExceptionCatcher.GENERIC_MESSAGE
				: exception.getMessage();
		return new RestErrorResponse(status, "Catch system exception", message);
	}

}
