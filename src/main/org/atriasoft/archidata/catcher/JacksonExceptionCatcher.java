package org.atriasoft.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class JacksonExceptionCatcher implements ExceptionMapper<JacksonException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JacksonExceptionCatcher.class);

	/**
	 * This method is called when a JacksonException is thrown.
	 * It logs the exception, builds a response with the error details, and returns it.
	 *
	 * @param exception the JacksonException that was thrown
	 * @return a Response object containing the error details
	 */
	@Override
	public Response toResponse(final JacksonException exception) {
		LOGGER.warn("Catch exception Input data parsing:");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		exception.printStackTrace();
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
