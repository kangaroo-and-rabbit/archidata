package org.kar.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ExceptionCatcher implements ExceptionMapper<Exception> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatcher.class);

	@Override
	public Response toResponse(final Exception exception) {
		LOGGER.warn("Catch exception (not managed...):");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		exception.printStackTrace();
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ret).type(MediaType.APPLICATION_JSON)
				.build();
	}

	private RestErrorResponse build(final Exception exception) {
		return new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
				"Catch Unknown Exception: " + exception.getClass().getCanonicalName(), exception.getMessage());
	}

}
