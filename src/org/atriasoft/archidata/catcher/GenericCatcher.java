package org.atriasoft.archidata.catcher;

import org.glassfish.jersey.server.ResourceConfig;

public class GenericCatcher {

	/**
	 * Add All the the generic catcher to standardize returns.
	 * @param rc Resource exception model.
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
