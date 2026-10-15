package test.atriasoft.archidata.catcher;

import org.atriasoft.archidata.catcher.InputExceptionCatcher;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

class TestInputExceptionCatcher {
	@Test
	void inputExceptionBodyFollowsExceptionStatus() {
		final Response response = new InputExceptionCatcher()
				.toResponse(new InputException(Response.Status.NOT_FOUND, "id", "not found"));
		Assertions.assertEquals(404, response.getStatus());
		final RestErrorResponse body = (RestErrorResponse) response.getEntity();
		Assertions.assertEquals(404, body.status);
		Assertions.assertEquals("Not Found", body.statusMessage);
	}

	@Test
	void inputExceptionDefaultIsBadRequest() {
		final Response response = new InputExceptionCatcher().toResponse(new InputException("id", "bad"));
		Assertions.assertEquals(400, response.getStatus());
		final RestErrorResponse body = (RestErrorResponse) response.getEntity();
		Assertions.assertEquals(400, body.status);
		Assertions.assertEquals("Bad Request", body.statusMessage);
	}
}
