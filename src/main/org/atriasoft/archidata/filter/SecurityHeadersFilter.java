package org.atriasoft.archidata.filter;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS response filter that adds the default HTTP security headers to every response.
 *
 * <ul>
 * <li>{@code X-Content-Type-Options: nosniff}: the browser trusts the {@code Content-Type} and never guesses an
 * executable type;</li>
 * <li>{@code Referrer-Policy: no-referrer}: no URL of the API (which may carry a token in its query string)
 * leaks to another site;</li>
 * <li>{@code X-Frame-Options: DENY}: no page of this server can be framed (clickjacking);</li>
 * <li>{@code Strict-Transport-Security: max-age=31536000}, only when the request came over https: the browser
 * will refuse plain http for a year. Behind a reverse proxy that terminates TLS, the request seen here is
 * http: let the proxy send this header.</li>
 * </ul>
 * <p>
 * A header already set by the resource is kept. Register it like the other filters:
 * {@code rc.register(SecurityHeadersFilter.class);}
 * </p>
 */
@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {
	/** Value of {@code Strict-Transport-Security} (one year, sent over https only). */
	public static final String HSTS_VALUE = "max-age=31536000";

	/** Default constructor used by the JAX-RS provider framework. */
	public SecurityHeadersFilter() {}

	private static void addIfAbsent(
			final MultivaluedMap<String, Object> headers,
			final String name,
			final String value) {
		if (!headers.containsKey(name)) {
			headers.putSingle(name, value);
		}
	}

	/**
	 * Adds the security headers to the response.
	 *
	 * @param request the container request context (its scheme decides about HSTS)
	 * @param response the container response context
	 * @throws IOException declared by the {@link ContainerResponseFilter} contract
	 */
	@Override
	public void filter(final ContainerRequestContext request, final ContainerResponseContext response)
			throws IOException {
		final MultivaluedMap<String, Object> headers = response.getHeaders();
		addIfAbsent(headers, "X-Content-Type-Options", "nosniff");
		addIfAbsent(headers, "Referrer-Policy", "no-referrer");
		addIfAbsent(headers, "X-Frame-Options", "DENY");
		if (request.getUriInfo() != null && request.getUriInfo().getRequestUri() != null
				&& "https".equalsIgnoreCase(request.getUriInfo().getRequestUri().getScheme())) {
			addIfAbsent(headers, "Strict-Transport-Security", HSTS_VALUE);
		}
	}
}
