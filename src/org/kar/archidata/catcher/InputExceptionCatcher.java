package org.kar.archidata.catcher;

import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class InputExceptionCatcher implements ExceptionMapper<InputException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(InputExceptionCatcher.class);

	@Override
	public Response toResponse(final InputException exception) {
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error UUID={} ==> '{}'=>'{}'", ret.uuid, exception.missingVariable, exception.getLocalizedMessage());
		// exception.printStackTrace();
		return Response.status(exception.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final InputException exception) {
		return new RestErrorResponse(exception.status, "Error on input='" + exception.missingVariable + "'", exception.getMessage());
	}

}
