package org.atriasoft.archidata.api;

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
//@Produces(MediaType.APPLICATION_JSON)
public class ProxyResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyResource.class);

	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getImageFromUrl(@QueryParam("url") final String url) {
		if (url == null || url.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("URL manquante").build();
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
		}
	}
}
