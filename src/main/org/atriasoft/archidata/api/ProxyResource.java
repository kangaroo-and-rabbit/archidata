package org.atriasoft.archidata.api;

import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.tools.SsrfGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * JAX-RS resource that proxies HTTP GET requests to external URLs, with SSRF protections.
 *
 * <p>Validates that the target URL uses HTTP/HTTPS and does not point to internal network addresses.</p>
 */
@Path("/proxy")
public class ProxyResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

	/**
	 * Fetches content from the given external URL and returns it to the client.
	 * @param url The external URL to fetch content from.
	 * @return A response containing the fetched content with appropriate headers.
	 */
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getImageFromUrl(@QueryParam("url") final String url) {
		// Validate URL to prevent SSRF attacks (resolves DNS and checks against private IP ranges)
		try {
			SsrfGuard.validateUrl(url);
		} catch (final InputException e) {
			return Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
		}
		final Client client = ClientBuilder.newClient();
		try {
			final WebTarget target = client.target(url);
			final Response response = target.request().get();
			if (response.getStatus() != 200) {
				return Response.status(Status.BAD_GATEWAY).entity("Can not get the image : " + response.getStatus())
						.build();
			}
			return Response.ok(response.readEntity(byte[].class)).header("Access-Control-Allow-Origin", "*")
					.header("Content-Type", response.getHeaderString("Content-Type")).build();
		} catch (final Exception e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("SERVER internal error : " + e.getMessage())
					.build();
		} finally {
			client.close();
		}
	}
}
