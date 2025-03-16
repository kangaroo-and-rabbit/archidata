package org.kar.archidata.catcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
		final List<RestInputError> inputError = new ArrayList<>();
		for (final var cv : exception.getConstraintViolations()) {
			if (cv == null) {
				continue;
			}
			inputError.add(new RestInputError(cv.getPropertyPath(), cv.getMessage()));
		}
		Collections.sort(inputError, Comparator.comparing(RestInputError::getFullPath));
		String errorType = "Multiple error on input";
		if (inputError.size() == 0) {
			errorType = "Constraint Violation";
		} else if (inputError.size() == 1) {
			errorType = "Error on input='" + inputError.get(0).path + "'";
		}
		return new RestErrorResponse(Response.Status.BAD_REQUEST, Instant.now().toString(), errorType,
				exception.getMessage(), inputError);
	}

}
