package org.kar.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class WebApplicationExceptionCatcher implements ExceptionMapper<WebApplicationException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionCatcher.class);

	@Override
	public Response toResponse(final WebApplicationException exception) {
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		return Response.status(exception.getResponse().getStatusInfo().toEnum()).entity(ret)
				.type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final WebApplicationException exception) {
		exception.printStackTrace();
		return new RestErrorResponse(exception.getResponse().getStatusInfo().toEnum(), "Catch system exception",
				exception.getMessage());
	}

}
