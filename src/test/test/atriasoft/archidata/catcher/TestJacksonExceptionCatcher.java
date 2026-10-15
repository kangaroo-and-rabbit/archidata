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

	@Test
	void malformedJsonIsBadRequest() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{not json", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(400, response.getStatus());
		Assertions.assertEquals(400, ((RestErrorResponse) response.getEntity()).status);
	}

	@Test
	void wrongTypeJsonIsBadRequest() {
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{\"value\":\"abc\"}", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(400, response.getStatus());
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
