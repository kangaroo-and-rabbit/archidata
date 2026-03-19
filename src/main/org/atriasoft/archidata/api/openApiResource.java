package org.atriasoft.archidata.api;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource that serves the OpenAPI 3.0.3 specification.
 *
 * <p>The application must call {@link #configure(AnalyzeApi, String, String)}
 * at startup to provide the analyzed API metadata. When configured, this
 * resource generates the spec from the archidata introspection system
 * (without depending on the Swagger scanner).
 *
 * <p>Usage:
 * <pre>{@code
 * AnalyzeApi api = new AnalyzeApi();
 * api.addAllApi(myResourceClasses);
 * openApiResource.configure(api, "My API", "1.0.0");
 * }</pre>
 */
@Path("/openapi")
public class openApiResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(openApiResource.class);

	private static AnalyzeApi analyzeApi;
	private static String apiTitle = "API";
	private static String apiVersion = "1.0.0";
	private static String cachedSpec;

	/**
	 * Configure the OpenAPI resource with analyzed API data.
	 *
	 * @param api     the analyzed API (endpoints + models)
	 * @param title   the API title
	 * @param version the API version
	 */
	public static void configure(final AnalyzeApi api, final String title, final String version) {
		analyzeApi = api;
		apiTitle = title;
		apiVersion = version;
		cachedSpec = null; // invalidate cache
	}

	@GET
	@Path("swagger.json")
	@Produces({ MediaType.APPLICATION_JSON })
	@PermitAll
	@ApiDoc(description = "Get the OpenAPI specification", group = "DEVELOPEMENT")
	public Response getDescription() throws Exception {
		if (analyzeApi == null) {
			LOGGER.error("OpenAPI resource not configured. Call openApiResource.configure() at startup.");
			return Response.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("{\"error\": \"OpenAPI not configured\"}").type(MediaType.APPLICATION_JSON).build();
		}
		if (cachedSpec == null) {
			cachedSpec = OpenApiGenerateApi.generateJson(analyzeApi, apiTitle, apiVersion);
		}
		return Response.ok(cachedSpec, MediaType.APPLICATION_JSON).build();
	}
}
