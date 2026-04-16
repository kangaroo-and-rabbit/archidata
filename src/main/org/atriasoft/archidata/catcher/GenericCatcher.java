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
	 * @param rc the Jersey resource configuration to register catchers on
	 */
	public static void addAll(final ResourceConfig rc) {
		// Generic Json parsing error
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
