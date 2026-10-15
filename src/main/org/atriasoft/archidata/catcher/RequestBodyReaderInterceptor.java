package org.atriasoft.archidata.catcher;

import java.io.IOException;

import org.atriasoft.archidata.exception.RequestBodyException;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;

/**
 * Server {@link ReaderInterceptor} that marks the JSON errors of the request body as the caller's mistake.
 * <p>
 * It surrounds the reading of the request entity: a body that cannot be parsed ({@link StreamReadException})
 * or does not match the expected type ({@link MismatchedInputException}, {@link ValueInstantiationException})
 * is rethrown as a {@link RequestBodyException}, answered with HTTP 400. Any Jackson exception that is not
 * wrapped here (a parsing done by the server itself, a bad class definition, a serialization failure) reaches
 * {@link JacksonExceptionCatcher} and gives HTTP 500.
 * <p>
 * It is registered by {@link GenericCatcher#addAll}, next to the mappers whose answer it decides.
 */
public class RequestBodyReaderInterceptor implements ReaderInterceptor {
	/** Default constructor used by the JAX-RS provider framework. */
	public RequestBodyReaderInterceptor() {}

	/**
	 * Reads the request entity and wraps its parsing and input mapping errors in a {@link RequestBodyException}.
	 * @param context the interceptor context of the request entity
	 * @return the entity read by the next interceptor or the message body reader
	 * @throws IOException a {@link RequestBodyException} for an invalid body, or any other read error
	 * @throws WebApplicationException as thrown by the next interceptor or the message body reader
	 */
	@Override
	public Object aroundReadFrom(final ReaderInterceptorContext context) throws IOException, WebApplicationException {
		try {
			return context.proceed();
		} catch (final StreamReadException | MismatchedInputException | ValueInstantiationException ex) {
			throw new RequestBodyException(ex);
		}
	}
}
