package test.atriasoft.archidata.externalRestApi;

import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Tests for {@link OpenApiGenerateApi} — validates that the generated
 * OpenAPI 3.0.3 spec is structurally correct.
 */
public class TestOpenApiGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestOpenApiGeneration.class);

	// -- Test models --

	@ApiDoc(description = "A simple item")
	public static class Item {
		@ApiDoc(description = "Item identifier", example = "42")
		@NotNull
		public Long id;

		@ApiDoc(description = "Item name")
		@Size(min = 1, max = 200)
		public String name;

		@Min(0)
		@Max(100)
		public Integer quantity;
	}

	public static class ItemInput {
		@Size(min = 1, max = 200)
		public String name;

		public Integer quantity;
	}

	public enum ItemStatus {
		ACTIVE, INACTIVE, ARCHIVED
	}

	public static class ParentModel {
		public Long id;
	}

	public static class ChildModel extends ParentModel {
		public String extra;
	}

	// -- Test API --

	@Path("/items")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public static class ItemResource {
		@GET
		@ApiDoc(description = "List all items", group = "ITEMS")
		public List<Item> getAll() {
			return null;
		}

		@GET
		@Path("/{id}")
		@ApiDoc(description = "Get item by ID", group = "ITEMS")
		public Item getById(@PathParam("id") final Long id) {
			return null;
		}

		@POST
		@ApiDoc(description = "Create a new item", group = "ITEMS")
		public Item create(final ItemInput data) {
			return null;
		}

		@PUT
		@Path("/{id}")
		@ApiDoc(description = "Update an item", group = "ITEMS")
		public Item update(@PathParam("id") final Long id, final ItemInput data) {
			return null;
		}

		@DELETE
		@Path("/{id}")
		@ApiDoc(description = "Delete an item", group = "ITEMS")
		public void delete(@PathParam("id") final Long id) {
		}
	}

	@Path("/search")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class SearchResource {
		@GET
		@ApiDoc(description = "Search items by query")
		public List<Item> search(@QueryParam("q") final String query, @QueryParam("limit") final Integer limit) {
			return null;
		}
	}

	// -- Tests --

	@Test
	public void testSpecStructure() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));
		api.addModel(Item.class);
		api.addModel(ItemInput.class);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test API", "1.0.0");

		Assertions.assertEquals("3.0.3", spec.get("openapi"));
		Assertions.assertNotNull(spec.get("info"));
		Assertions.assertNotNull(spec.get("paths"));
		Assertions.assertNotNull(spec.get("components"));

		@SuppressWarnings("unchecked")
		final Map<String, Object> info = (Map<String, Object>) spec.get("info");
		Assertions.assertEquals("Test API", info.get("title"));
		Assertions.assertEquals("1.0.0", info.get("version"));
	}

	@Test
	public void testPathsGeneration() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
		Assertions.assertNotNull(paths);

		// Should have /items and /items/{id}
		Assertions.assertTrue(paths.containsKey("/items"), "Should have /items path");
		Assertions.assertTrue(paths.containsKey("/items/{id}"), "Should have /items/{id} path");

		// /items should have GET and POST
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemsPath = (Map<String, Object>) paths.get("/items");
		Assertions.assertTrue(itemsPath.containsKey("get"), "/items should have GET");
		Assertions.assertTrue(itemsPath.containsKey("post"), "/items should have POST");

		// /items/{id} should have GET, PUT, DELETE
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemIdPath = (Map<String, Object>) paths.get("/items/{id}");
		Assertions.assertTrue(itemIdPath.containsKey("get"), "/items/{id} should have GET");
		Assertions.assertTrue(itemIdPath.containsKey("put"), "/items/{id} should have PUT");
		Assertions.assertTrue(itemIdPath.containsKey("delete"), "/items/{id} should have DELETE");
	}

	@Test
	public void testOperationDetails() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemsPath = (Map<String, Object>) paths.get("/items");
		@SuppressWarnings("unchecked")
		final Map<String, Object> getOp = (Map<String, Object>) itemsPath.get("get");

		// Check summary
		Assertions.assertEquals("List all items", getOp.get("summary"));

		// Check tags
		@SuppressWarnings("unchecked")
		final List<String> tags = (List<String>) getOp.get("tags");
		Assertions.assertNotNull(tags);
		Assertions.assertTrue(tags.contains("ItemResource"));

		// Check operationId
		Assertions.assertEquals("getAll", getOp.get("operationId"));
	}

	@Test
	public void testPathParameters() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemIdPath = (Map<String, Object>) paths.get("/items/{id}");
		@SuppressWarnings("unchecked")
		final Map<String, Object> getOp = (Map<String, Object>) itemIdPath.get("get");

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> parameters = (List<Map<String, Object>>) getOp.get("parameters");
		Assertions.assertNotNull(parameters);
		Assertions.assertFalse(parameters.isEmpty());

		// Find the "id" path parameter
		Map<String, Object> idParam = null;
		for (final Map<String, Object> p : parameters) {
			if ("id".equals(p.get("name"))) {
				idParam = p;
				break;
			}
		}
		Assertions.assertNotNull(idParam, "Should have 'id' path parameter");
		Assertions.assertEquals("path", idParam.get("in"));
		Assertions.assertEquals(true, idParam.get("required"));
	}

	@Test
	public void testQueryParameters() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SearchResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
		@SuppressWarnings("unchecked")
		final Map<String, Object> searchPath = (Map<String, Object>) paths.get("/search");
		@SuppressWarnings("unchecked")
		final Map<String, Object> getOp = (Map<String, Object>) searchPath.get("get");

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> parameters = (List<Map<String, Object>>) getOp.get("parameters");
		Assertions.assertNotNull(parameters);
		Assertions.assertEquals(2, parameters.size());

		// Verify both are query parameters
		for (final Map<String, Object> p : parameters) {
			Assertions.assertEquals("query", p.get("in"));
		}
	}

	@Test
	public void testRequestBody() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemsPath = (Map<String, Object>) paths.get("/items");
		@SuppressWarnings("unchecked")
		final Map<String, Object> postOp = (Map<String, Object>) itemsPath.get("post");

		Assertions.assertNotNull(postOp.get("requestBody"), "POST should have requestBody");

		@SuppressWarnings("unchecked")
		final Map<String, Object> requestBody = (Map<String, Object>) postOp.get("requestBody");
		Assertions.assertEquals(true, requestBody.get("required"));
		Assertions.assertNotNull(requestBody.get("content"));
	}

	@Test
	public void testSchemaGeneration() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(Item.class);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> components = (Map<String, Object>) spec.get("components");
		@SuppressWarnings("unchecked")
		final Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
		Assertions.assertNotNull(schemas);
		Assertions.assertTrue(schemas.containsKey("Item"), "Should have Item schema");

		@SuppressWarnings("unchecked")
		final Map<String, Object> itemSchema = (Map<String, Object>) schemas.get("Item");
		Assertions.assertEquals("A simple item", itemSchema.get("description"));

		@SuppressWarnings("unchecked")
		final Map<String, Object> properties = (Map<String, Object>) itemSchema.get("properties");
		Assertions.assertNotNull(properties);
		Assertions.assertTrue(properties.containsKey("id"));
		Assertions.assertTrue(properties.containsKey("name"));
		Assertions.assertTrue(properties.containsKey("quantity"));

		// Check field descriptions
		@SuppressWarnings("unchecked")
		final Map<String, Object> idProp = (Map<String, Object>) properties.get("id");
		Assertions.assertEquals("Item identifier", idProp.get("description"));
		Assertions.assertEquals("42", idProp.get("example"));
	}

	@Test
	public void testSchemaConstraints() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(Item.class);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> components = (Map<String, Object>) spec.get("components");
		@SuppressWarnings("unchecked")
		final Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
		@SuppressWarnings("unchecked")
		final Map<String, Object> itemSchema = (Map<String, Object>) schemas.get("Item");
		@SuppressWarnings("unchecked")
		final Map<String, Object> properties = (Map<String, Object>) itemSchema.get("properties");

		// name: @Size(min=1, max=200)
		@SuppressWarnings("unchecked")
		final Map<String, Object> nameProp = (Map<String, Object>) properties.get("name");
		Assertions.assertEquals(1, nameProp.get("minLength"));
		Assertions.assertEquals(200, nameProp.get("maxLength"));

		// quantity: @Min(0) @Max(100)
		@SuppressWarnings("unchecked")
		final Map<String, Object> quantityProp = (Map<String, Object>) properties.get("quantity");
		Assertions.assertEquals(0L, quantityProp.get("minimum"));
		Assertions.assertEquals(100L, quantityProp.get("maximum"));
	}

	@Test
	public void testEnumSchema() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ItemStatus.class);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> components = (Map<String, Object>) spec.get("components");
		@SuppressWarnings("unchecked")
		final Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
		Assertions.assertTrue(schemas.containsKey("ItemStatus"), "Should have ItemStatus enum schema");

		@SuppressWarnings("unchecked")
		final Map<String, Object> enumSchema = (Map<String, Object>) schemas.get("ItemStatus");
		Assertions.assertEquals("string", enumSchema.get("type"));

		@SuppressWarnings("unchecked")
		final List<Object> enumValues = (List<Object>) enumSchema.get("enum");
		Assertions.assertNotNull(enumValues);
		Assertions.assertEquals(3, enumValues.size());
		Assertions.assertTrue(enumValues.contains("ACTIVE"));
		Assertions.assertTrue(enumValues.contains("INACTIVE"));
		Assertions.assertTrue(enumValues.contains("ARCHIVED"));
	}

	@Test
	public void testInheritanceSchema() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ChildModel.class);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");

		@SuppressWarnings("unchecked")
		final Map<String, Object> components = (Map<String, Object>) spec.get("components");
		@SuppressWarnings("unchecked")
		final Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
		Assertions.assertTrue(schemas.containsKey("ChildModel"), "Should have ChildModel schema");
		Assertions.assertTrue(schemas.containsKey("ParentModel"), "Should have ParentModel schema");

		@SuppressWarnings("unchecked")
		final Map<String, Object> childSchema = (Map<String, Object>) schemas.get("ChildModel");
		// ChildModel should use allOf for inheritance
		Assertions.assertNotNull(childSchema.get("allOf"), "ChildModel should have allOf for inheritance");

		@SuppressWarnings("unchecked")
		final List<Map<String, Object>> allOf = (List<Map<String, Object>>) childSchema.get("allOf");
		Assertions.assertEquals(2, allOf.size());
		// First element should be a $ref to ParentModel
		Assertions.assertEquals("#/components/schemas/ParentModel", allOf.get(0).get("$ref"));
	}

	@Test
	public void testJsonOutput() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ItemResource.class));
		api.addModel(Item.class);

		final String json = OpenApiGenerateApi.generateJson(api, "Test API", "2.0.0");
		Assertions.assertNotNull(json);
		Assertions.assertFalse(json.isEmpty());

		// Validate it's valid JSON
		final ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		final Map<String, Object> parsed = mapper.readValue(json, Map.class);
		Assertions.assertEquals("3.0.3", parsed.get("openapi"));

		LOGGER.info("Generated OpenAPI JSON:\n{}", json);
	}
}
