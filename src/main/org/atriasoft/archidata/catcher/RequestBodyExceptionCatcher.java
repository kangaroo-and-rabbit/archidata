package org.atriasoft.archidata.catcher;

import org.atriasoft.archidata.exception.RequestBodyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that answers HTTP 400 for a {@link RequestBodyException}: the JSON body sent by the
 * caller cannot be parsed or does not match the expected type.
 */
public class RequestBodyExceptionCatcher implements ExceptionMapper<RequestBodyException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestBodyExceptionCatcher.class);

	/** Default constructor used by the JAX-RS provider framework. */
	public RequestBodyExceptionCatcher() {}

	/**
	 * Maps a {@link RequestBodyException} to a JSON error response with HTTP 400.
	 * @param exception the RequestBodyException raised while reading the request body
	 * @return a {@link Response} containing the error details as JSON with HTTP 400 status
	 */
	@Override
	public Response toResponse(final RequestBodyException exception) {
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.BAD_REQUEST, "Catch JSON Exception",
				exception.getMessage());
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(ret.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}
}
