package org.kar.archidata.catcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.glassfish.jersey.server.ParamException.QueryParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class QueryParamExceptionCatcher implements ExceptionMapper<QueryParamException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryParamExceptionCatcher.class);

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
