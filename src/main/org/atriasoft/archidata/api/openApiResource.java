package org.atriasoft.archidata.api;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
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
 * <p>The endpoint is public ({@code @PermitAll}) but <b>disabled by default</b>: it answers 404 unless
 * {@code API_DOC_ENABLED=true} ({@link ConfigBaseVariable#getApiDocEnabled()}). Enable it in development, or
 * behind a network restriction.
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
	private static final Object SPEC_LOCK = new Object();

	/** Default constructor used by the JAX-RS resource framework. */
	public openApiResource() {}

	/**
	 * Configure the OpenAPI resource with analyzed API data.
	 *
	 * @param api     the analyzed API (endpoints + models)
	 * @param title   the API title
	 * @param version the API version
	 */
	public static void configure(final AnalyzeApi api, final String title, final String version) {
		synchronized (SPEC_LOCK) {
			analyzeApi = api;
			apiTitle = title;
			apiVersion = version;
			cachedSpec = null; // invalidate cache
		}
	}

	/** The specification, generated at the first call after {@link #configure}, or {@code null} before it. */
	private static String specification() throws Exception {
		synchronized (SPEC_LOCK) {
			if (analyzeApi == null) {
				return null;
			}
			if (cachedSpec == null) {
				cachedSpec = OpenApiGenerateApi.generateJson(analyzeApi, apiTitle, apiVersion);
			}
			return cachedSpec;
		}
	}

	/**
	 * Returns the OpenAPI 3.0.3 specification as a JSON response.
	 * @return A JSON response containing the OpenAPI specification, a 404 when {@code API_DOC_ENABLED} is not
	 *         {@code true}, or a 503 error if not configured.
	 * @throws Exception If the spec generation fails.
	 */
	@GET
	@Path("swagger.json")
	@Produces({ MediaType.APPLICATION_JSON })
	@PermitAll
	@ApiDoc(description = "Get the OpenAPI specification", group = "DEVELOPEMENT")
	public Response getDescription() throws Exception {
		if (!ConfigBaseVariable.getApiDocEnabled()) {
			// Off by default: the specification maps the whole API for an anonymous caller.
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		final String specification = specification();
		if (specification == null) {
			LOGGER.error("OpenAPI resource not configured. Call openApiResource.configure() at startup.");
			return Response.status(Response.Status.SERVICE_UNAVAILABLE)
					.entity("{\"error\": \"OpenAPI not configured\"}").type(MediaType.APPLICATION_JSON).build();
		}
		return Response.ok(specification, MediaType.APPLICATION_JSON).build();
	}
}
