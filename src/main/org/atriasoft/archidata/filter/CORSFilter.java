package org.atriasoft.archidata.filter;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

	@Override
	public void filter(final ContainerRequestContext request, final ContainerResponseContext response)
			throws IOException {
		// System.err.println("filter cors ..." + request.toString());

		response.getHeaders().add("Access-Control-Allow-Origin", "*");
		response.getHeaders().add("Access-Control-Allow-Range", "bytes");
		response.getHeaders().add("access-control-expose-headers", "range");
		response.getHeaders().add("Access-Control-Allow-Headers",
				"Origin, content-type, Content-type, Accept, Authorization, mime-type, filename, Range");
		response.getHeaders().add("Access-Control-Allow-Credentials", "true");
		response.getHeaders().add("Access-Control-Allow-Methods",
				"GET, POST, PUT, PATCH, DELETE, ARCHIVE, RESTORE, OPTIONS, HEAD, CALL");
	}
}
