package org.atriasoft.archidata.api;

import java.util.Locale;

import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.tools.DataTools;
import org.atriasoft.archidata.tools.SafeHttpFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * JAX-RS resource that proxies HTTP GET requests to external URLs, with SSRF protections.
 *
 * <p>The fetch goes through {@link SafeHttpFetcher}: only HTTP/HTTPS, no internal network address (checked
 * again on each redirection, on the address actually connected to), timeouts and a size limit. Only raster
 * image types are sent back as such, anything else as {@code application/octet-stream}, with
 * {@code X-Content-Type-Options: nosniff}.</p>
 *
 * <p>The method carries no security annotation: {@code AuthenticationFilter} answers 403 to it. Subclass it
 * and annotate the override ({@code @RolesAllowed}) to expose it.</p>
 */
@Path("/proxy")
public class ProxyResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);
	private static final SafeHttpFetcher FETCHER = new SafeHttpFetcher();

	/** Default constructor used by the JAX-RS resource framework. */
	public ProxyResource() {}

	/**
	 * Fetches content from the given external URL and returns it to the client.
	 * @param url The external URL to fetch content from.
	 * @return A response containing the fetched content with appropriate headers.
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getImageFromUrl(@QueryParam("url") final String url) {
		final SafeHttpFetcher.Result response;
		try {
			// Resolves the host once, connects to the validated address and checks every redirection
			response = FETCHER.get(url, DataTools.getMaxRemoteDataSize());
		} catch (final InputException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (final FailException e) {
			return Response.status(e.status).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (final Exception e) {
			LOGGER.error("Proxy request failed: {}", e.getMessage(), e);
			return Response.status(Status.BAD_GATEWAY).entity("Can not get the image").type(MediaType.TEXT_PLAIN)
					.build();
		}
		if (response.status() != 200) {
			return Response.status(Status.BAD_GATEWAY).entity("Can not get the image : " + response.status())
					.type(MediaType.TEXT_PLAIN).build();
		}
		return Response.ok(response.body()).type(safeContentType(response.contentType()))
				.header("X-Content-Type-Options", "nosniff").build();
	}

	/**
	 * Keeps the upstream content type only for raster images: anything else (HTML, SVG, scripts) is sent
	 * back as {@code application/octet-stream}, so it never runs on the origin of the API.
	 * @param upstream The {@code Content-Type} sent by the remote server.
	 * @return The content type to answer with.
	 */
	static String safeContentType(final String upstream) {
		if (upstream == null) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		final String type = upstream.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		if (type.startsWith("image/") && !type.contains("svg")) {
			return type;
		}
		return MediaType.APPLICATION_OCTET_STREAM;
	}
}
