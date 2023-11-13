package org.kar.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class FailException404API implements ExceptionMapper<ClientErrorException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FailException404API.class);

	@Override
	public Response toResponse(final ClientErrorException exception) {
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error UUID={}", ret.uuid);
		return Response.status(exception.getResponse().getStatusInfo().toEnum()).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final ClientErrorException exception) {
		return new RestErrorResponse(exception.getResponse().getStatusInfo().toEnum(), "Catch system exception", exception.getMessage());
	}

}
