package org.atriasoft.archidata.catcher;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Utility class that registers all standard exception catchers for consistent REST error handling.
 */
public class GenericCatcher {
	private GenericCatcher() {
		// Utility class
	}

	/**
	 * Registers all standard exception catchers on the given resource configuration to standardize error responses.
	 * <p>
	 * It also registers {@link RequestBodyReaderInterceptor}, which marks the JSON errors of the request body:
	 * it is registered here and not in {@code ContextGenericTools.addJsr310} because it only decides which of
	 * these mappers answers (400 for the request body, 500 for any other Jackson error), whatever the JSON
	 * provider in use.
	 * @param rc the Jersey resource configuration to register catchers on
	 */
	public static void addAll(final ResourceConfig rc) {
		// Json errors: the request body ones are marked by the interceptor (400), the others are server errors (500)
		rc.register(RequestBodyReaderInterceptor.class);
		rc.register(RequestBodyExceptionCatcher.class);
		rc.register(JacksonExceptionCatcher.class);
		// Archidata exceptions
		rc.register(InputExceptionCatcher.class);
		rc.register(SystemExceptionCatcher.class);
		rc.register(FailExceptionCatcher.class);
		// generic Exception catcher
		rc.register(ConstraintViolationExceptionCatcher.class);
		rc.register(QueryParamExceptionCatcher.class);
		rc.register(ExceptionCatcher.class);
		// Catch jakarta generic errors
		rc.register(WebApplicationExceptionCatcher.class);
	}

}
