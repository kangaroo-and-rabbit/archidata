package org.kar.archidata.catcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ConstraintViolationExceptionCatcher implements ExceptionMapper<ConstraintViolationException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionCatcher.class);

	@Override
	public Response toResponse(final ConstraintViolationException exception) {
		LOGGER.warn("Catch ConstraintViolationException: {}", exception.getLocalizedMessage());
		final RestErrorResponse ret = build(exception);
		LOGGER.error("Error OID={}", ret.oid);
		return Response.status(Response.Status.BAD_REQUEST).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}

	private RestErrorResponse build(final ConstraintViolationException exception) {
		return new RestErrorResponse(Response.Status.BAD_REQUEST, "Constraint Violation", exception.getMessage());
	}

}
