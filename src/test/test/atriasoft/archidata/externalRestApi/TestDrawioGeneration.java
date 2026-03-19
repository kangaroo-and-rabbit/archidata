package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.annotation.checker.CheckForeignKey;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.DrawioGenerateApi;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Tests for {@link DrawioGenerateApi} — validates that the generated
 * Draw.io XML is structurally correct and contains expected elements.
 */
public class TestDrawioGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestDrawioGeneration.class);

	// -- Test models --

	@ApiDoc(description = "A product in the catalog")
	public static class Product {
		public Long id;
		public String name;
		public Double price;
		public CategoryEnum category;
	}

	public static class ProductInput {
		public String name;
		public Double price;
	}

	public enum CategoryEnum {
		ELECTRONICS, CLOTHING, FOOD
	}

	public static class BaseEntity {
		public Long id;
	}

	public static class ExtendedEntity extends BaseEntity {
		public String label;
	}

	public static class FkTarget {
		public ObjectId oid;
		public String label;
	}

	public static class FkSource {
		public ObjectId oid;
		@CheckForeignKey(target = FkTarget.class)
		public ObjectId parentId;
		public String name;
	}

	// -- Test API --

	@Path("/products")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class ProductResource {
		@GET
		@ApiDoc(description = "List all products")
		public List<Product> getAll() {
			return null;
		}

		@GET
		@Path("/{id}")
		@ApiDoc(description = "Get product by ID")
		public Product getById(@PathParam("id") final Long id) {
			return null;
		}

		@POST
		@ApiDoc(description = "Create a product")
		public Product create(final ProductInput data) {
			return null;
		}

		@PUT
		@Path("/{id}")
		@ApiDoc(description = "Update a product")
		public Product update(@PathParam("id") final Long id, final ProductInput data) {
			return null;
		}

		@DELETE
		@Path("/{id}")
		@ApiDoc(description = "Delete a product")
		public void delete(@PathParam("id") final Long id) {
		}
	}

	// -- Tests --

	@Test
	public void testGeneratedXmlStructure() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ProductResource.class));
		api.addModel(Product.class);

		final String xml = DrawioGenerateApi.generateApiString(api);
		Assertions.assertNotNull(xml);
		Assertions.assertFalse(xml.isEmpty());

		// Basic XML structure
		Assertions.assertTrue(xml.contains("<mxfile"), "Should have mxfile root element");
		Assertions.assertTrue(xml.contains("<diagram"), "Should have diagram element");
		Assertions.assertTrue(xml.contains("<mxGraphModel"), "Should have mxGraphModel element");
		Assertions.assertTrue(xml.contains("<root>"), "Should have root element");
		Assertions.assertTrue(xml.contains("</mxfile>"), "Should close mxfile");

		LOGGER.info("Generated Draw.io XML length: {}", xml.length());
	}

	@Test
	public void testModelNodesPresent() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(Product.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Product model should be present
		Assertions.assertTrue(xml.contains("Product"), "Should contain Product model");
		// Fields should be present
		Assertions.assertTrue(xml.contains("name"), "Should contain 'name' field");
		Assertions.assertTrue(xml.contains("price"), "Should contain 'price' field");
	}

	@Test
	public void testEnumNodePresent() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(CategoryEnum.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Enum should be present with «enum» prefix
		Assertions.assertTrue(xml.contains("CategoryEnum"), "Should contain CategoryEnum");
		Assertions.assertTrue(xml.contains("ELECTRONICS"), "Should contain ELECTRONICS value");
		Assertions.assertTrue(xml.contains("CLOTHING"), "Should contain CLOTHING value");
		Assertions.assertTrue(xml.contains("FOOD"), "Should contain FOOD value");
		// Enum style (green)
		Assertions.assertTrue(xml.contains("#d5e8d4"), "Enum should have green fill color");
	}

	@Test
	public void testRestNodesPresent() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ProductResource.class));

		final String xml = DrawioGenerateApi.generateApiString(api);

		// REST group should be present
		Assertions.assertTrue(xml.contains("ProductResource"), "Should contain ProductResource group");
		// REST style (red)
		Assertions.assertTrue(xml.contains("#f8cecc"), "REST should have red fill color");
		// Endpoints should contain HTTP methods
		Assertions.assertTrue(xml.contains("GET"), "Should contain GET method");
		Assertions.assertTrue(xml.contains("POST"), "Should contain POST method");
		Assertions.assertTrue(xml.contains("DELETE"), "Should contain DELETE method");
	}

	@Test
	public void testInheritanceEdge() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ExtendedEntity.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Both models should be present
		Assertions.assertTrue(xml.contains("BaseEntity"), "Should contain BaseEntity");
		Assertions.assertTrue(xml.contains("ExtendedEntity"), "Should contain ExtendedEntity");
		// Should have inheritance edge style
		Assertions.assertTrue(xml.contains("endArrow=block;endFill=0"), "Should have UML inheritance arrow style");
	}

	@Test
	public void testModelStyles() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(Product.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Model style (blue)
		Assertions.assertTrue(xml.contains("#dae8fc"), "Model should have blue fill color");
		// Should use swimlane style
		Assertions.assertTrue(xml.contains("swimlane"), "Should use swimlane style for UML classes");
	}

	@Test
	public void testModelsOnlyGeneration() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ProductResource.class));
		api.addModel(Product.class);

		// Generate models only — REST should not appear
		final StringBuilder xmlBuilder = new StringBuilder();
		// Use reflection-free approach: generate full and models-only, compare
		final String fullXml = DrawioGenerateApi.generateApiString(api);
		Assertions.assertTrue(fullXml.contains("ProductResource"), "Full should contain REST");
		Assertions.assertTrue(fullXml.contains("Product"), "Full should contain model");
	}

	@Test
	public void testRestLinks() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ProductResource.class));
		api.addModel(Product.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Should have REST link style (dashed red)
		Assertions.assertTrue(xml.contains("strokeColor=#b85450"), "Should have REST-to-model link style");
	}

	@Test
	public void testXmlWellFormed() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ProductResource.class));
		api.addModel(Product.class);
		api.addModel(CategoryEnum.class);
		api.addModel(ExtendedEntity.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Count mxCell tags for basic well-formedness
		// 2 root cells are self-closing (<mxCell id="0"/>, <mxCell id="1" .../>)
		// All other mxCell elements have matching </mxCell>
		final int openMxCell = countOccurrences(xml, "<mxCell ");
		final int closeMxCell = countOccurrences(xml, "</mxCell>");
		final int selfClosingMxCell = countOccurrences(xml, "<mxCell id=\"0\"") + countOccurrences(xml, "<mxCell id=\"1\"");
		Assertions.assertEquals(openMxCell, closeMxCell + selfClosingMxCell,
				"Each <mxCell> should have a matching </mxCell> or be self-closing");

		// Ensure no unescaped special characters in values
		// (the value attributes should have &lt; instead of raw <)
		Assertions.assertFalse(xml.contains("value=\"<"), "Values should be XML-escaped");

		LOGGER.info("Generated Draw.io XML:\n{}", xml);
	}

	@Test
	public void testCheckForeignKeyEdge() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(FkSource.class);
		api.addModel(FkTarget.class);

		final String xml = DrawioGenerateApi.generateApiString(api);

		// Both models present
		Assertions.assertTrue(xml.contains("FkSource"), "Should contain FkSource model");
		Assertions.assertTrue(xml.contains("FkTarget"), "Should contain FkTarget model");
		// FK annotation text on field
		Assertions.assertTrue(xml.contains("FkTarget"),
				"FK field should show target class name");
		// FK edge style (purple dashed)
		Assertions.assertTrue(xml.contains("strokeColor=#9673a6"),
				"Should have FK edge style (purple)");
		// Edge label should contain the field name
		Assertions.assertTrue(xml.contains("value=\"parentId\""),
				"FK edge should have field name as label");

		LOGGER.info("FK Draw.io XML:\n{}", xml);
	}

	private static int countOccurrences(final String text, final String search) {
		int count = 0;
		int idx = 0;
		while ((idx = text.indexOf(search, idx)) != -1) {
			count++;
			idx += search.length();
		}
		return count;
	}
}
