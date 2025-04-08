package org.atriasoft.archidata.api;

import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/openapi")
public class openApiResource extends BaseOpenApiResource {
	@Context
	ServletConfig config;

	@Context
	Application app;

	@GET
	@Path("swagger.json")
	@Produces({ MediaType.APPLICATION_JSON })
	@PermitAll
	@Operation(hidden = true, description = "Get the OPEN-API description", tags = "SYSTEM")
	public Response getDescription(@Context final HttpHeaders headers, @Context final UriInfo uriInfo)
			throws Exception {
		return getOpenApi(headers, this.config, this.app, uriInfo, "json");
	}
}
