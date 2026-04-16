package org.atriasoft.archidata.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS pre-matching filter that handles HTTP OPTIONS requests by returning a 204 No Content response.
 *
 * <p>
 * This filter short-circuits OPTIONS requests before they reach resource methods,
 * which is typically used in conjunction with CORS preflight handling.
 * </p>
 */
@Provider
@PreMatching
public class OptionFilter implements ContainerRequestFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(OptionFilter.class);

	/**
	 * Filters incoming requests and aborts OPTIONS requests with a 204 No Content response.
	 */
	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		// Not in production: LOGGER.warn("Receive message from: [{}] '{}'", requestContext.getMethod(), requestContext.getUriInfo().getPath());
		/* Not in production:
		final Map<String, List<String>> queryParams = requestContext.getUriInfo().getQueryParameters();
		for (final Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			LOGGER.warn("queryParam: '{}' => '{}'", entry.getKey(), entry.getValue());
		}
		*/
		if (requestContext.getMethod().contentEquals("OPTIONS")) {
			requestContext.abortWith(Response.status(Response.Status.NO_CONTENT).build());
		}
	}
}
