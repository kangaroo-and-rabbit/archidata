package org.atriasoft.archidata.catcher;

import org.atriasoft.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * This class catches InputException and maps it to a HTTP response.
 */
public class InputExceptionCatcher implements ExceptionMapper<InputException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(InputExceptionCatcher.class);

	/**
	 * This method is called when an InputException is thrown.
	 * It logs the exception and builds a response with the error details.
	 *
	 * @param exception the InputException that was thrown
	 * @return a Response object containing the error details
	 */
	@Override
	public Response toResponse(final InputException exception) {
		LOGGER.warn("Catch InputException:");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={} ==> '{}'=>'{}'", ret.oid, exception.missingVariable,
				exception.getLocalizedMessage());
		// exception.printStackTrace();
		return Response.status(exception.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	/**
	 * This method builds a RestErrorResponse object from the InputException.
	 *
	 * @param exception the InputException that was thrown
	 * @return a RestErrorResponse object containing the error details
	 */
	private RestErrorResponse build(final InputException exception) {
		return new RestErrorResponse(exception.status, "Error on input='" + exception.missingVariable + "'",
				exception.getMessage());
	}

}
