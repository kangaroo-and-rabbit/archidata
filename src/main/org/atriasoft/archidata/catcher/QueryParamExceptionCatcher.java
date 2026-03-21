package org.atriasoft.archidata.catcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.glassfish.jersey.server.ParamException.QueryParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that catches {@link QueryParamException} instances
 * and converts them into standardized JSON error responses with input error details.
 */
public class QueryParamExceptionCatcher implements ExceptionMapper<QueryParamException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryParamExceptionCatcher.class);

	/**
	 * Maps a {@link QueryParamException} to a BAD_REQUEST JSON error response with details about the invalid query parameter.
	 * @param exception the QueryParamException that was thrown
	 * @return a {@link Response} containing the error details as JSON with HTTP 400 status
	 */
	@Override
	public Response toResponse(final QueryParamException exception) {
		LOGGER.trace("Catch IllegalArgumentException: {}", exception.getLocalizedMessage());
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		return Response.status(Response.Status.BAD_REQUEST).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final QueryParamException exception) {
		final List<RestInputError> inputError = new ArrayList<>();
		inputError.add(new RestInputError("query", exception.getParameterName(), exception.getCause().getMessage()));
		final String errorType = "Error on query input='" + exception.getParameterName() + "'";
		return new RestErrorResponse(Response.Status.BAD_REQUEST, Instant.now().toString(), errorType,
				"Input parsing fail", inputError);
	}

}
