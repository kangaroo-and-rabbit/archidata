package test.atriasoft.archidata.catcher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.JacksonExceptionCatcher;
import org.atriasoft.archidata.catcher.RequestBodyExceptionCatcher;
import org.atriasoft.archidata.catcher.RequestBodyReaderInterceptor;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.catcher.WebApplicationExceptionCatcher;
import org.atriasoft.archidata.exception.RequestBodyException;
import org.junit.jupiter.api.Assertions;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

class TestJacksonExceptionCatcher {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	public static class Bean {
		public int value;
	}

	public static class NotSerializable {
		private final int hidden = 1;
	}

	/** Minimal interceptor context whose {@code proceed()} reads the JSON with Jackson, as the provider does. */
	private static ReaderInterceptorContext readingJson(final String json) {
		return readingJson(MAPPER, json);
	}

	private static ReaderInterceptorContext readingJson(final ObjectMapper mapper, final String json) {
		return new ReaderInterceptorContext() {
			@Override
			public Object proceed() throws IOException {
				return mapper.readValue(json, Bean.class);
			}

			@Override
			public InputStream getInputStream() {
				return null;
			}

			@Override
			public void setInputStream(final InputStream is) {}

			@Override
			public MultivaluedMap<String, String> getHeaders() {
				return null;
			}

			@Override
			public Object getProperty(final String name) {
				return null;
			}

			@Override
			public Collection<String> getPropertyNames() {
				return List.of();
			}

			@Override
			public void setProperty(final String name, final Object object) {}

			@Override
			public void removeProperty(final String name) {}

			@Override
			public Annotation[] getAnnotations() {
				return new Annotation[0];
			}

			@Override
			public void setAnnotations(final Annotation[] annotations) {}

			@Override
			public Class<?> getType() {
				return Bean.class;
			}

			@Override
			public void setType(final Class<?> type) {}

			@Override
			public Type getGenericType() {
				return Bean.class;
			}

			@Override
			public void setGenericType(final Type genericType) {}

			@Override
			public MediaType getMediaType() {
				return MediaType.APPLICATION_JSON_TYPE;
			}

			@Override
			public void setMediaType(final MediaType mediaType) {}
		};
	}

	@Test
	void malformedRequestBodyIsMarkedAndBadRequest() {
		final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
				() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{not json")));
		Assertions.assertInstanceOf(JacksonException.class, ex.getCause());
		final Response response = new RequestBodyExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(400, response.getStatus());
		Assertions.assertEquals(400, ((RestErrorResponse) response.getEntity()).status);
	}

	@Test
	void wrongTypeRequestBodyIsMarkedAndBadRequest() {
		final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
				() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{\"value\":\"abc\"}")));
		Assertions.assertEquals(400, new RequestBodyExceptionCatcher().toResponse(ex).getStatus());
	}

	@Test
	void validRequestBodyIsReturned() throws Exception {
		final Object entity = new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{\"value\":3}"));
		Assertions.assertEquals(3, ((Bean) entity).value);
	}

	@Test
	void rawJacksonExceptionIsInternalError() {
		// e.g. a stored document or an upstream response read with an ObjectMapper
		final JacksonException ex = Assertions.assertThrows(JacksonException.class,
				() -> MAPPER.readValue("{not json", Bean.class));
		final Response response = new JacksonExceptionCatcher().toResponse(ex);
		Assertions.assertEquals(500, response.getStatus());
		Assertions.assertEquals(500, ((RestErrorResponse) response.getEntity()).status);
	}

	@Test
	void serverSideMappingErrorStaysInternalError() {
		final JacksonException ex = Assertions.assertThrows(InvalidDefinitionException.class,
				() -> MAPPER.writeValueAsString(new NotSerializable()));
		Assertions.assertEquals(500, new JacksonExceptionCatcher().toResponse(ex).getStatus());
	}

	// ===== no internal detail in the answers (W-13, D-10) =====

	private static String messageOf(final Response response) {
		return ((RestErrorResponse) response.getEntity()).message;
	}

	@Test
	void unknownFieldNamesTheFieldButNotTheClassNorTheProperties() {
		final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
				() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{\"secretField\":1}")));
		final String message = messageOf(new RequestBodyExceptionCatcher().toResponse(ex));
		Assertions.assertEquals("Unknown field 'secretField'", message);
		Assertions.assertFalse(message.contains("Bean"));
		Assertions.assertFalse(message.contains("value"));
	}

	@Test
	void wrongTypeNamesTheField() {
		final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
				() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{\"value\":\"abc\"}")));
		Assertions.assertEquals("Invalid value for field 'value'",
				messageOf(new RequestBodyExceptionCatcher().toResponse(ex)));
	}

	@Test
	void malformedBodyGivesThePosition() {
		final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
				() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson("{not json")));
		Assertions.assertTrue(
				messageOf(new RequestBodyExceptionCatcher().toResponse(ex)).startsWith("Malformed JSON at line 1"));
	}

	@Test
	void unquotedValueIsNeitherAnsweredNorLogged() {
		final String secret = "abcdef0123456789";
		final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(RequestBodyExceptionCatcher.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			// the default mapper quotes the token, the archidata one must not
			for (final ObjectMapper mapper : List.of(MAPPER, ContextGenericTools.createObjectMapper())) {
				appender.list.clear();
				final String body = "{\"password\": " + secret + "}";
				final RequestBodyException ex = Assertions.assertThrows(RequestBodyException.class,
						() -> new RequestBodyReaderInterceptor().aroundReadFrom(readingJson(mapper, body)));
				Assertions.assertFalse(String.valueOf(ex.getMessage()).contains(secret));
				final Response response = new RequestBodyExceptionCatcher().toResponse(ex);
				Assertions.assertEquals(400, response.getStatus());
				Assertions.assertFalse(messageOf(response).contains(secret));
				Assertions.assertFalse(appender.list.isEmpty());
				for (final ILoggingEvent event : appender.list) {
					Assertions.assertFalse(event.getFormattedMessage().contains(secret));
					Assertions.assertNull(event.getThrowableProxy());
				}
			}
			// defence in depth: the mapper of the server does not even put the token in the Jackson message
			final JacksonException raw = Assertions.assertThrows(JacksonException.class, () -> ContextGenericTools
					.createObjectMapper().readValue("{\"value\": " + secret + "}", Bean.class));
			Assertions.assertFalse(raw.getMessage().contains(secret));
		} finally {
			logger.detachAppender(appender);
		}
	}

	@Test
	void serverJacksonErrorIsGeneric() {
		final JacksonException ex = Assertions.assertThrows(InvalidDefinitionException.class,
				() -> MAPPER.writeValueAsString(new NotSerializable()));
		Assertions.assertEquals(ExceptionCatcher.GENERIC_MESSAGE,
				messageOf(new JacksonExceptionCatcher().toResponse(ex)));
	}

	@Test
	void unexpectedExceptionIsGeneric() {
		final Response response = new ExceptionCatcher().toResponse(new IllegalStateException(
				"E11000 duplicate key error collection: test.user index: email_1 dup key: { email: \"a@b.c\" }"));
		Assertions.assertEquals(500, response.getStatus());
		Assertions.assertEquals(ExceptionCatcher.GENERIC_MESSAGE, messageOf(response));
		Assertions.assertNotNull(((RestErrorResponse) response.getEntity()).oid);
	}

	@Test
	void serverWebApplicationExceptionIsGenericClientOneIsKept() {
		final Response server = new WebApplicationExceptionCatcher()
				.toResponse(new InternalServerErrorException(new java.io.IOException("/srv/data/secret/path")));
		Assertions.assertEquals(500, server.getStatus());
		Assertions.assertEquals(ExceptionCatcher.GENERIC_MESSAGE, messageOf(server));
		final Response client = new WebApplicationExceptionCatcher()
				.toResponse(new NotFoundException("media NOT FOUND"));
		Assertions.assertEquals(404, client.getStatus());
		Assertions.assertEquals("media NOT FOUND", messageOf(client));
	}
}
