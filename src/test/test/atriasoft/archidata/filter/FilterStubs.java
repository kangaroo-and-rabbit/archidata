package test.atriasoft.archidata.filter;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;

/** Minimal request and response contexts for the unit tests of the response filters. */
final class FilterStubs {
	private FilterStubs() {}

	/** A request with these headers on this URI; any other call returns null. */
	static ContainerRequestContext request(final String uri, final Map<String, String> headers) {
		final UriInfo uriInfo = (UriInfo) Proxy.newProxyInstance(FilterStubs.class.getClassLoader(),
				new Class<?>[] { UriInfo.class }, (proxy, method, args) -> {
					if ("getRequestUri".equals(method.getName())) {
						return URI.create(uri);
					}
					return null;
				});
		return (ContainerRequestContext) Proxy.newProxyInstance(FilterStubs.class.getClassLoader(),
				new Class<?>[] { ContainerRequestContext.class }, (proxy, method, args) -> {
					if ("getHeaderString".equals(method.getName())) {
						return headers.get((String) args[0]);
					}
					if ("getUriInfo".equals(method.getName())) {
						return uriInfo;
					}
					return null;
				});
	}

	/** A response whose headers are the given map. */
	static ContainerResponseContext response(final MultivaluedMap<String, Object> headers) {
		return (ContainerResponseContext) Proxy.newProxyInstance(FilterStubs.class.getClassLoader(),
				new Class<?>[] { ContainerResponseContext.class }, (proxy, method, args) -> {
					if ("getHeaders".equals(method.getName())) {
						return headers;
					}
					return null;
				});
	}

	static MultivaluedMap<String, Object> headers() {
		return new MultivaluedHashMap<>();
	}
}
