package org.atriasoft.archidata.api;

import java.net.URI;
import java.net.URISyntaxException;

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

@Path("/proxy")
public class ProxyResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getImageFromUrl(@QueryParam("url") final String url) {
		if (url == null || url.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("URL manquante").build();
		}
		// Validate URL to prevent SSRF attacks
		try {
			final URI uri = new URI(url);
			final String scheme = uri.getScheme();
			if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
				return Response.status(Status.BAD_REQUEST).entity("Only HTTP and HTTPS URLs are allowed").build();
			}
			final String host = uri.getHost();
			if (host == null || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")
					|| host.startsWith("10.") || host.startsWith("192.168.") || host.equals("[::1]")
					|| host.startsWith("169.254.") || host.startsWith("172.16.") || host.startsWith("172.17.")
					|| host.startsWith("172.18.") || host.startsWith("172.19.") || host.startsWith("172.2")
					|| host.startsWith("172.30.") || host.startsWith("172.31.")) {
				return Response.status(Status.FORBIDDEN).entity("Access to internal network addresses is forbidden")
						.build();
			}
		} catch (final URISyntaxException e) {
			return Response.status(Status.BAD_REQUEST).entity("Invalid URL format").build();
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
