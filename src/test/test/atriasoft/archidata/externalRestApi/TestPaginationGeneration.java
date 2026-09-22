package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.method.PaginationContext;
import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.DotGenerateApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * {@code Pagination<T>} across the generators: the type reaches the API in several
 * shapes, and each one has its own way of getting it wrong.
 */
public class TestPaginationGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestPaginationGeneration.class);

	public static class SampleRow extends OIDGenericDataSoftDelete {
		public String label;
	}

	@Path("/sample-paginated/{entity}")
	@Produces(MediaType.APPLICATION_JSON)
	public static class SamplePaginatedResource {
		@GET
		public Pagination<SampleRow> list(
				@PathParam("entity") final String entity,
				@PaginationContext final org.atriasoft.archidata.dataAccess.model.PaginationContext page) {
			return null;
		}
	}

	private static AnalyzeApi api() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SamplePaginatedResource.class));
		return api;
	}

	@Test
	public void testPythonClientCanRequestAPage() throws Exception {
		final Map<java.nio.file.Path, String> generation = PythonGenerateApi.generateApi(api());
		final String client = generation.get(Paths.get("api/sample_paginated_resource.py"));
		LOGGER.info("sample_paginated_resource.py:\n{}", client);
		// the page is an input of the method, and it reaches the helper
		Assertions.assertTrue(client.contains("        offset: int | None = None,"), client);
		Assertions.assertTrue(client.contains("        limit: int | None = None,"), client);
		Assertions.assertTrue(client.contains("            offset=offset,\n            limit=limit,\n"), client);
		Assertions.assertTrue(client.contains(") -> Pagination[SampleRow]:"), client);
		// @PaginationContext is a server-side concern, it must not leak
		Assertions.assertFalse(client.contains("PaginationContext"), client);
	}

	@Test
	public void testOpenApiDescribesTheWire() throws Exception {
		final String specification = OpenApiGenerateApi.generateJson(api(), "Test", "1.0");
		LOGGER.info("openapi:\n{}", specification);
		// the body on the wire is the plain item list; the totals travel in headers
		Assertions.assertTrue(specification.contains("\"type\" : \"array\""), specification);
		Assertions.assertTrue(specification.contains("SampleRow"), specification);
		Assertions.assertTrue(specification.contains("X-Total-Count"), specification);
		Assertions.assertTrue(specification.contains("Link"), specification);
	}

	@Test
	public void testDotGraphIsProduced() throws Exception {
		// used to throw "Impossible model: ClassPaginationModel", which is why the back
		// had to switch its architecture-graph CI step off
		final java.nio.file.Path target = Paths.get("target", "generated-pagination", "architecture.dot");
		java.nio.file.Files.createDirectories(target.getParent());
		DotGenerateApi.generateApi(api(), target.toString());
		final String graph = java.nio.file.Files.readString(target);
		LOGGER.info("dot:\n{}", graph);
		Assertions.assertTrue(graph.contains("SampleRow"), graph);
		Assertions.assertTrue(graph.contains("Pagination&lt;SampleRow&gt;"), graph);
	}
}
