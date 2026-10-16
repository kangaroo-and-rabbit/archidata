package org.atriasoft.archidata.catcher;

import org.atriasoft.archidata.exception.RequestBodyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * JAX-RS exception mapper that answers HTTP 400 for a {@link RequestBodyException}: the JSON body sent by the
 * caller cannot be parsed or does not match the expected type.
 * <p>
 * The message names the faulty field or the position in the text ({@link #describe(Throwable)}), never the raw
 * Jackson message (Java class names, list of the properties of the entity), which is logged.
 * </p>
 */
public class RequestBodyExceptionCatcher implements ExceptionMapper<RequestBodyException> {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestBodyExceptionCatcher.class);

	/** Longest field path or name repeated in the answer. */
	private static final int MAX_NAME_LENGTH = 100;

	/** Default constructor used by the JAX-RS provider framework. */
	public RequestBodyExceptionCatcher() {}

	private static String shorten(final String value) {
		return value.length() > MAX_NAME_LENGTH ? value.substring(0, MAX_NAME_LENGTH) + "..." : value;
	}

	private static String pathOf(final JsonMappingException exception) {
		final StringBuilder out = new StringBuilder();
		for (final JsonMappingException.Reference reference : exception.getPath()) {
			if (reference.getFieldName() != null) {
				if (!out.isEmpty()) {
					out.append('.');
				}
				out.append(reference.getFieldName());
			} else if (reference.getIndex() >= 0) {
				out.append('[').append(reference.getIndex()).append(']');
			}
		}
		return out.toString();
	}

	/**
	 * Describes what is wrong in the request body without the raw Jackson message, which names the Java classes
	 * and lists every property of the entity.
	 * @param cause The Jackson exception raised while reading the body.
	 * @return A message that only names the field (path in the JSON) or the position in the text.
	 */
	public static String describe(final Throwable cause) {
		if (cause instanceof final UnrecognizedPropertyException unknown && unknown.getPropertyName() != null) {
			return "Unknown field '" + shorten(unknown.getPropertyName()) + "'";
		}
		if (cause instanceof final JsonMappingException mapping) {
			final String path = pathOf(mapping);
			if (!path.isEmpty()) {
				return "Invalid value for field '" + shorten(path) + "'";
			}
			return "Invalid JSON body";
		}
		if (cause instanceof final StreamReadException read && read.getLocation() != null) {
			return "Malformed JSON at line " + read.getLocation().getLineNr() + ", column "
					+ read.getLocation().getColumnNr();
		}
		return "Invalid JSON body";
	}

	/**
	 * Maps a {@link RequestBodyException} to a JSON error response with HTTP 400.
	 * @param exception the RequestBodyException raised while reading the request body
	 * @return a {@link Response} containing the error details as JSON with HTTP 400 status
	 */
	@Override
	public Response toResponse(final RequestBodyException exception) {
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.BAD_REQUEST, "Catch JSON Exception",
				describe(exception.getCause()));
		LOGGER.error("Error OID={}: {}", ret.oid, exception.getMessage(), exception);
		return Response.status(ret.status).entity(ret).type(MediaType.APPLICATION_JSON).build();
	}
}
