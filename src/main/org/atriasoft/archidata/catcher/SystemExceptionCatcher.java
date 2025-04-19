package org.atriasoft.archidata.catcher;

import org.atriasoft.archidata.exception.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class SystemExceptionCatcher implements ExceptionMapper<SystemException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SystemExceptionCatcher.class);

	@Override
	public Response toResponse(final SystemException exception) {
		LOGGER.warn("Catch SystemException:");
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		exception.printStackTrace();
		return Response.status(exception.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final SystemException exception) {
		return new RestErrorResponse(exception.status, "System error", exception.getMessage());
	}

}
