package test.atriasoft.archidata.catcher;

import org.atriasoft.archidata.catcher.JacksonExceptionCatcher;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import jakarta.ws.rs.core.Response;

class TestJacksonExceptionCatcher {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static class Bean {
		public int value;
	}

	public static class NotSerializable {
		private final int hidden = 1;
	}

	/**
	 * Puts the exception in the stack Jersey has when it reads a request entity, as if the JSON came
	 * from the body of the request.
	 */
	private static <T extends Throwable> T asReadFromRequestBody(final T exception) {
		final StackTraceElement[] original = exception.getStackTrace();
		final StackTraceElement[] frames = new StackTraceElement[original.length + 2];
		System.arraycopy(original, 0, frames, 0, original.length);
		frames[original.length] = new StackTraceElement(
				"org.glassfish.jersey.message.internal.ReaderInterceptorExecutor$TerminalReaderInterceptor",
				"invokeReadFrom", "ReaderInterceptorExecutor.java", 1);
		frames[original.length + 1] = new StackTraceElement("org.glassfish.jersey.server.ContainerRequest",
				"readEntity", "ContainerRequest.java", 1);
		exception.setStackTrace(frames);
		return exception;
	}

	/** Same, but the entity is read by the Jersey client: an upstream response, not the request body. */
	private static <T extends Throwable> T asReadByJerseyClient(final T exception) {
		final T readEntity = asReadFromRequestBody(exception);
		final StackTraceElement[] original = readEntity.getStackTrace();
		final StackTraceElement[] frames = new StackTraceElement[original.length + 1];
		System.arraycopy(original, 0, frames, 0, original.length);
		frames[original.length] = new StackTraceElement("org.glassfish.jersey.client.InboundJaxrsResponse",
				"readEntity", "InboundJaxrsResponse.java", 1);
		readEntity.setStackTrace(frames);
		return readEntity;
	}

	@Test
	void malformedRequestBodyIsBadRequest() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{not json", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(asReadFromRequestBody(ex));
		Assertions.assertEquals(400, response.getStatus());
		Assertions.assertEquals(400, ((RestErrorResponse) response.getEntity()).status);
	}

	@Test
	void wrongTypeRequestBodyIsBadRequest() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{\"value\":\"abc\"}", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(asReadFromRequestBody(ex));
		Assertions.assertEquals(400, response.getStatus());
	}

	@Test
	void malformedJsonParsedByTheServerIsInternalError() {
		// e.g. a stored document or an upstream response read with an ObjectMapper
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{not json", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(500, response.getStatus());
		Assertions.assertEquals(500, ((RestErrorResponse) response.getEntity()).status);
	}

	@Test
	void wrongTypeJsonParsedByTheServerIsInternalError() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{\"value\":\"abc\"}", Bean.class));
		Assertions.assertEquals(500, new JacksonExceptionCatcher().toResponse(ex).getStatus());
	}

	@Test
	void upstreamResponseReadByJerseyClientIsInternalError() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{\"value\":\"abc\"}", Bean.class));
		Assertions.assertEquals(500, new JacksonExceptionCatcher().toResponse(asReadByJerseyClient(ex)).getStatus());
	}

	@Test
	void serverSideMappingErrorStaysInternalError() {
		final JacksonException ex = Assertions.assertThrows(InvalidDefinitionException.class,
				() -> MAPPER.writeValueAsString(new NotSerializable()));
		final Response response = new JacksonExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(500, response.getStatus());
		Assertions.assertEquals(500, ((RestErrorResponse) response.getEntity()).status);
	}
}
