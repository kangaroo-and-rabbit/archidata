package org.atriasoft.archidata.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@PreMatching
public class OptionFilter implements ContainerRequestFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(OptionFilter.class);

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		LOGGER.warn("Receive message from: [{}] '{}'", requestContext.getMethod(),
				requestContext.getUriInfo().getPath());
		final Map<String, List<String>> queryParams = requestContext.getUriInfo().getQueryParameters();
		for (final Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			LOGGER.warn("queryParam: '{}' => '{}'", entry.getKey(), entry.getValue());
		}
		if (requestContext.getMethod().contentEquals("OPTIONS")) {
			requestContext.abortWith(Response.status(Response.Status.NO_CONTENT).build());
		}
	}
}
