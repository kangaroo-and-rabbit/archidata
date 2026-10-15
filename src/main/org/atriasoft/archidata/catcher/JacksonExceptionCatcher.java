package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link JacksonException} instances
 * and converts them into standardized JSON error responses.
 */
public class JacksonExceptionCatcher implements ExceptionMapper<JacksonException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JacksonExceptionCatcher.class);

	/** Default constructor used by the JAX-RS provider framework. */
	public JacksonExceptionCatcher() {}

	/**
	 * Maps a {@link JacksonException} to a JSON error response. A body that cannot be parsed
	 * ({@link StreamReadException}) or does not match the expected type ({@link MismatchedInputException},
	 * {@link ValueInstantiationException}) is the caller's mistake and gives HTTP 400; any other
	 * Jackson failure (serialization, bad class definition) is a server error and gives HTTP 500.
	 * @param exception the JacksonException that was thrown during JSON processing
	 * @return a {@link Response} containing the error details as JSON with HTTP 400 or 500 status
	 */
	@Override
	public Response toResponse(final JacksonException exception) {
		LOGGER.warn("Catch exception Input data parsing:");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(ret.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	/**
	 * Builds a RestErrorResponse object from the given exception.
	 *
	 * @param exception the Exception that was thrown
	 * @return a RestErrorResponse object containing the error details
	 */
	private RestErrorResponse build(final JacksonException exception) {
		final Response.Status status = isCallerError(exception) ? Response.Status.BAD_REQUEST
				: Response.Status.INTERNAL_SERVER_ERROR;
		return new RestErrorResponse(status, "Catch JSON Exception", exception.getMessage());
	}

	/**
	 * Tells whether the exception comes from invalid input data sent by the caller.
	 * @param exception the exception to classify
	 * @return true for parsing and input mapping errors
	 */
	private static boolean isCallerError(final JacksonException exception) {
		return exception instanceof StreamReadException || exception instanceof MismatchedInputException
				|| exception instanceof ValueInstantiationException;
	}

}
