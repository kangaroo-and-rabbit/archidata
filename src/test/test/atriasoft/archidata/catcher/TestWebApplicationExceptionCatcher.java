package test.atriasoft.archidata.catcher;

import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.catcher.WebApplicationExceptionCatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code StatusType.toEnum()} is {@code null} for a code {@link Response.Status} does not list: the mapper used to
 * fail on it with a {@link NullPointerException} instead of answering.
 */
public class TestWebApplicationExceptionCatcher {
	private final WebApplicationExceptionCatcher catcher = new WebApplicationExceptionCatcher();

	@Test
	void aClientCodeUnknownToTheEnumKeepsItsStatusAndMessage() {
		final Response response = this.catcher
				.toResponse(new WebApplicationException("client gone", Response.status(499).build()));
		Assertions.assertEquals(499, response.getStatus());
		Assertions.assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		final RestErrorResponse body = (RestErrorResponse) response.getEntity();
		Assertions.assertEquals(499, body.status);
		Assertions.assertEquals("client gone", body.message);
		Assertions.assertNotNull(body.statusMessage);
	}

	@Test
	void aServerCodeUnknownToTheEnumHidesItsMessage() {
		final Response response = this.catcher
				.toResponse(new WebApplicationException("secret detail", Response.status(599).build()));
		Assertions.assertEquals(599, response.getStatus());
		final RestErrorResponse body = (RestErrorResponse) response.getEntity();
		Assertions.assertEquals(599, body.status);
		Assertions.assertEquals(ExceptionCatcher.GENERIC_MESSAGE, body.message);
	}

	@Test
	void aListedStatusIsUnchanged() {
		final Response response = this.catcher.toResponse(new NotFoundException("media NOT FOUND"));
		Assertions.assertEquals(404, response.getStatus());
		final RestErrorResponse body = (RestErrorResponse) response.getEntity();
		Assertions.assertEquals(404, body.status);
		Assertions.assertEquals("Not Found", body.statusMessage);
		Assertions.assertEquals("media NOT FOUND", body.message);
	}
}
