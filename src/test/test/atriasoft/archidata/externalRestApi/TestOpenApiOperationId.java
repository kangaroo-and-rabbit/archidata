package test.atriasoft.archidata.externalRestApi;

import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The {@code operationId} of an OpenAPI operation is unique in the specification.
 */
public class TestOpenApiOperationId {

	@Path("/cats")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class CatResource {
		@GET
		public String get() {
			return null;
		}

		@POST
		public String create() {
			return null;
		}
	}

	@Path("/dogs")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class DogResource {
		@GET
		@Path("{id}")
		public String get(@jakarta.ws.rs.PathParam("id") final String id) {
			return null;
		}

		@GET
		public String get() {
			return null;
		}

		@POST
		public String build() {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(final Object value) {
		return (Map<String, Object>) value;
	}

	private static String operationId(final Map<String, Object> spec, final String path, final String verb) {
		return (String) map(map(map(spec.get("paths")).get(path)).get(verb)).get("operationId");
	}

	@Test
	public void testOperationIdIsAlwaysResourceAndMethod() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(CatResource.class, DogResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		Assertions.assertEquals("CatResource_get", operationId(spec, "/cats", "get"));
		Assertions.assertEquals("CatResource_create", operationId(spec, "/cats", "post"));
		Assertions.assertEquals("DogResource_build", operationId(spec, "/dogs", "post"));
		// two overloads of one name: ordered by verb, then path, then parameter types (not by the JVM)
		Assertions.assertEquals("DogResource_get", operationId(spec, "/dogs", "get"));
		Assertions.assertEquals("DogResource_get_2", operationId(spec, "/dogs/{id}", "get"));
	}

	@Test
	public void testIdOfOneResourceDoesNotDependOnTheOthers() throws Exception {
		final AnalyzeApi alone = new AnalyzeApi();
		alone.addAllApi(List.of(CatResource.class));
		final AnalyzeApi together = new AnalyzeApi();
		together.addAllApi(List.of(CatResource.class, DogResource.class));

		Assertions.assertEquals(operationId(OpenApiGenerateApi.generate(alone, "Test", "1.0"), "/cats", "get"),
				operationId(OpenApiGenerateApi.generate(together, "Test", "1.0"), "/cats", "get"));
	}
}
