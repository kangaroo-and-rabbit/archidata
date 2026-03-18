package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Tests for the {@link ApiDoc} annotation and its fallback to {@link Schema}/{@link Operation}.
 */
public class TestApiDoc {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestApiDoc.class);

	// -- Models with @ApiDoc --

	@ApiDoc(description = "A model with ApiDoc", example = "{\"value\": 42}")
	public static class ModelWithApiDoc {
		@ApiDoc(description = "The value field", example = "42")
		public Integer value;

		@ApiDoc(description = "The name field")
		public String name;
	}

	// -- Models with legacy @Schema (should still work with deprecation warning) --

	@Schema(description = "A model with Schema", example = "{\"value\": 99}")
	public static class ModelWithSchema {
		@Schema(description = "Legacy value field", example = "99")
		public Integer value;
	}

	// -- Models with both @ApiDoc and @Schema (@ApiDoc should win) --

	@ApiDoc(description = "ApiDoc wins", example = "apidoc-example")
	@Schema(description = "Schema loses", example = "schema-example")
	public static class ModelWithBoth {
		@ApiDoc(description = "ApiDoc field", example = "apidoc-field")
		@Schema(description = "Schema field", example = "schema-field")
		public String data;
	}

	// -- API with @ApiDoc on method --

	@Path("/test-apidoc")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class ApiWithApiDoc {
		@GET
		@ApiDoc(description = "Get data via ApiDoc", group = "TEST")
		public String getData() {
			return null;
		}
	}

	// -- API with legacy @Operation --

	@Path("/test-operation")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class ApiWithOperation {
		@GET
		@Operation(description = "Get data via Operation", tags = "LEGACY")
		public String getData() {
			return null;
		}
	}

	// -- Tests --

	@Test
	public void testApiDocOnClass() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithApiDoc.class);

		final ClassObjectModel model = findModel(api, ModelWithApiDoc.class);
		Assertions.assertNotNull(model);
		Assertions.assertEquals("A model with ApiDoc", model.getDescription());
		Assertions.assertEquals("{\"value\": 42}", model.getExample());
	}

	@Test
	public void testApiDocOnFields() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithApiDoc.class);

		final ClassObjectModel model = findModel(api, ModelWithApiDoc.class);
		Assertions.assertNotNull(model);

		final FieldProperty valueField = findField(model, "value");
		Assertions.assertNotNull(valueField);
		Assertions.assertEquals("The value field", valueField.comment());
		Assertions.assertEquals("42", valueField.example());

		final FieldProperty nameField = findField(model, "name");
		Assertions.assertNotNull(nameField);
		Assertions.assertEquals("The name field", nameField.comment());
	}

	@Test
	public void testSchemaFallback() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithSchema.class);

		final ClassObjectModel model = findModel(api, ModelWithSchema.class);
		Assertions.assertNotNull(model);
		// @Schema should still work as fallback
		Assertions.assertEquals("A model with Schema", model.getDescription());
		Assertions.assertEquals("{\"value\": 99}", model.getExample());

		final FieldProperty valueField = findField(model, "value");
		Assertions.assertNotNull(valueField);
		Assertions.assertEquals("Legacy value field", valueField.comment());
		Assertions.assertEquals("99", valueField.example());
	}

	@Test
	public void testApiDocPriorityOverSchema() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithBoth.class);

		final ClassObjectModel model = findModel(api, ModelWithBoth.class);
		Assertions.assertNotNull(model);
		// @ApiDoc should take priority
		Assertions.assertEquals("ApiDoc wins", model.getDescription());
		Assertions.assertEquals("apidoc-example", model.getExample());

		final FieldProperty dataField = findField(model, "data");
		Assertions.assertNotNull(dataField);
		Assertions.assertEquals("ApiDoc field", dataField.comment());
		Assertions.assertEquals("apidoc-field", dataField.example());
	}

	@Test
	public void testApiDocOnApiMethod() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiWithApiDoc.class));

		Assertions.assertEquals(1, api.getAllApi().size());
		final ApiModel endpoint = api.getAllApi().get(0).getInterfaceNamed("getData");
		Assertions.assertNotNull(endpoint);
		Assertions.assertEquals("Get data via ApiDoc", endpoint.description);
	}

	@Test
	public void testOperationFallbackOnApiMethod() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiWithOperation.class));

		Assertions.assertEquals(1, api.getAllApi().size());
		final ApiModel endpoint = api.getAllApi().get(0).getInterfaceNamed("getData");
		Assertions.assertNotNull(endpoint);
		// @Operation should still work as fallback
		Assertions.assertEquals("Get data via Operation", endpoint.description);
	}

	// -- Helpers --

	private static ClassObjectModel findModel(final AnalyzeApi api, final Class<?> clazz) {
		for (final var m : api.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == clazz) {
				return com;
			}
		}
		return null;
	}

	private static FieldProperty findField(final ClassObjectModel model, final String name) {
		for (final FieldProperty fp : model.getFields()) {
			if (name.equals(fp.name())) {
				return fp;
			}
		}
		return null;
	}
}
