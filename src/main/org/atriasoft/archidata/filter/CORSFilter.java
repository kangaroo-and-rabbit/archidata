package org.atriasoft.archidata.filter;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.tools.ConfigBaseVariable;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS response filter that adds Cross-Origin Resource Sharing (CORS) headers to the responses.
 *
 * <p>
 * The allowed origins come from {@link ConfigBaseVariable#getCorsAllowedOrigins()} (environment variable
 * {@code CORS_ALLOWED_ORIGINS}, comma separated):
 * </p>
 * <ul>
 * <li>not set: {@code Access-Control-Allow-Origin: *}, without {@code Access-Control-Allow-Credentials}
 * (browsers never send credentials to a {@code *} origin anyway);</li>
 * <li>set, and the {@code Origin} of the request is in the list: that origin is sent back, with
 * {@code Access-Control-Allow-Credentials: true};</li>
 * <li>set, and the origin is not in the list (or the request has none): no CORS header, the browser
 * refuses to hand the response to the page.</li>
 * </ul>
 * <p>
 * When the list is set, every response carries {@code Vary: Origin} (origin allowed, refused or absent), so
 * that a shared cache never serves the answer made for one origin to another one.
 * </p>
 * <p>
 * CORS only binds browsers: a server-to-server call, a script or curl is not affected by these headers.
 * It is not an access control: the endpoints still need their {@code @RolesAllowed}.
 * </p>
 */
@Provider
public class CORSFilter implements ContainerResponseFilter {

	/** Default constructor used by the JAX-RS provider framework. */
	public CORSFilter() {}

	/**
	 * Returns the allowed origins, by default {@link ConfigBaseVariable#getCorsAllowedOrigins()}. Override it
	 * to take them from elsewhere.
	 * @return The allowed origins, or {@code null} to allow any origin without credentials.
	 */
	protected List<String> allowedOrigins() {
		return ConfigBaseVariable.getCorsAllowedOrigins();
	}

	/**
	 * Adds CORS headers to the response.
	 *
	 * @param request the container request context
	 * @param response the container response context to which CORS headers are added
	 * @throws IOException if an I/O error occurs during filtering
	 */
	@Override
	public void filter(final ContainerRequestContext request, final ContainerResponseContext response)
			throws IOException {
		final List<String> allowed = allowedOrigins();
		final MultivaluedMap<String, Object> headers = response.getHeaders();
		if (allowed == null) {
			headers.putSingle("Access-Control-Allow-Origin", "*");
		} else {
			// The answer depends on the Origin whatever it is (absent, refused or allowed): a shared cache must
			// not hand the answer made for one origin to another one.
			addVaryOrigin(headers);
			final String origin = request.getHeaderString("Origin");
			if (origin == null || !allowed.contains(origin)) {
				return;
			}
			headers.putSingle("Access-Control-Allow-Origin", origin);
			headers.putSingle("Access-Control-Allow-Credentials", "true");
		}
		headers.putSingle("Access-Control-Allow-Range", "bytes");
		// Expose pagination metadata so cross-origin clients can read the total
		// count and the RFC 5988 navigation links emitted by PaginationResponseFilter.
		headers.putSingle("access-control-expose-headers", "range, X-Total-Count, Link");
		headers.putSingle("Access-Control-Allow-Headers",
				"Origin, content-type, Content-type, Accept, Authorization, mime-type, filename, Range, X-Pagination-Offset, X-Pagination-Limit");
		headers.putSingle("Access-Control-Allow-Methods",
				"GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD, ARCHIVE, RESTORE, CALL");
	}

	/**
	 * Adds {@code Origin} to the {@code Vary} header, unless a {@code Vary} value already names it.
	 * @param headers the response headers
	 */
	private static void addVaryOrigin(final MultivaluedMap<String, Object> headers) {
		final List<Object> vary = headers.get("Vary");
		if (vary != null) {
			for (final Object value : vary) {
				for (final String item : String.valueOf(value).split(",")) {
					final String name = item.trim();
					if (name.equalsIgnoreCase("Origin") || name.equals("*")) {
						return;
					}
				}
			}
		}
		headers.add("Vary", "Origin");
	}
}
