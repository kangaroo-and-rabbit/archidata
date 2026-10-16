package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches any unhandled {@link Exception} as a fallback
 * and converts it into a standardized JSON error response with HTTP 500 status.
 * <p>
 * The answer carries {@link #GENERIC_MESSAGE} and the {@code oid} of the error, never the message of the
 * exception, which is logged with its stack trace under the same {@code oid}.
 * </p>
 */
public class ExceptionCatcher implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatcher.class);
	/** Message sent back for an unexpected exception: the details stay in the log (search it by {@code oid}). */
	public static final String GENERIC_MESSAGE = "Internal server error";

	/** Default constructor used by the JAX-RS provider framework. */
	public ExceptionCatcher() {}

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
		// The message of an unexpected exception can carry internal details (host names, driver messages, values
		// of a duplicate key, class names): it only goes to the server log, under the oid of the answer.
		return new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Internal Server Error", GENERIC_MESSAGE);
	}

}
