package test.atriasoft.archidata.externalRestApi;

import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ApiTool;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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

	// -- @ApiDoc(hidden = true) --

	public static class ModelWithHiddenField {
		public String visibleField;

		@ApiDoc(description = "internal", hidden = true)
		public String secretInternalField;
	}

	public static class ModelWithHiddenGetter {
		private String visibleBean;
		private String secretGetterField;

		public String getVisibleBean() {
			return this.visibleBean;
		}

		public void setVisibleBean(final String visibleBean) {
			this.visibleBean = visibleBean;
		}

		@ApiDoc(description = "internal", hidden = true)
		public String getSecretGetterField() {
			return this.secretGetterField;
		}

		public void setSecretGetterField(final String secretGetterField) {
			this.secretGetterField = secretGetterField;
		}
	}

	@Path("/test-hidden")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class ApiWithHiddenMethod {
		@GET
		@Path("visible")
		public ModelWithHiddenField visibleEndpoint() {
			return null;
		}

		@GET
		@Path("hidden")
		@ApiDoc(description = "internal", hidden = true)
		public String secretHiddenEndpoint() {
			return null;
		}
	}

	@Path("/test-hidden-class")
	@Produces({ MediaType.APPLICATION_JSON })
	@ApiDoc(hidden = true)
	public static class HiddenApi {
		@GET
		public String secretHiddenClassEndpoint() {
			return null;
		}
	}

	@ApiDoc(hidden = true)
	public interface HiddenApiContract {}

	@Path("/test-hidden-interface")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class HiddenByInterfaceApi implements HiddenApiContract {
		@GET
		public String secretInterfaceEndpoint() {
			return null;
		}
	}

	@ApiDoc(hidden = true)
	public abstract static class HiddenApiBase {}

	@Path("/test-hidden-superclass")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class HiddenBySuperclassApi extends HiddenApiBase {
		@GET
		public String secretSuperclassEndpoint() {
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

	@Test
	public void testHiddenIsRespectedByTheAnalyzer() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiWithHiddenMethod.class, HiddenApi.class));
		Assertions.assertEquals(1, api.getAllApi().size());
		Assertions.assertNotNull(api.getAllApi().get(0).getInterfaceNamed("visibleEndpoint"));
		Assertions.assertNull(api.getAllApi().get(0).getInterfaceNamed("secretHiddenEndpoint"));
		final ClassObjectModel model = findModel(api, ModelWithHiddenField.class);
		Assertions.assertNotNull(model);
		Assertions.assertNotNull(findField(model, "visibleField"));
		Assertions.assertNull(findField(model, "secretInternalField"));
	}

	@Test
	public void testHiddenIsRespectedByTheGenerators() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiWithHiddenMethod.class, HiddenApi.class));
		final StringBuilder all = new StringBuilder();
		all.append(new ObjectMapper().writeValueAsString(OpenApiGenerateApi.generate(api, "Test", "1.0")));
		for (final Map<java.nio.file.Path, String> generation : List.of(TsGenerateApi.generateApi(api),
				PythonGenerateApi.generateApi(api))) {
			for (final String content : generation.values()) {
				all.append(content);
			}
		}
		final String text = all.toString();
		Assertions.assertTrue(text.contains("visibleField"));
		Assertions.assertFalse(text.contains("secretInternalField"));
		Assertions.assertFalse(text.contains("secretHiddenEndpoint"));
		Assertions.assertFalse(text.contains("secret_hidden_endpoint"));
		Assertions.assertFalse(text.contains("secretHiddenClassEndpoint"));
		Assertions.assertFalse(text.contains("secret_hidden_class_endpoint"));
		Assertions.assertFalse(text.contains("test-hidden-class"));
		Assertions.assertFalse(text.contains("secret_internal_field"));
	}

	@Test
	public void testHiddenOnGetterIsRespected() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithHiddenGetter.class);
		final ClassObjectModel model = findModel(api, ModelWithHiddenGetter.class);
		Assertions.assertNotNull(model);
		Assertions.assertNotNull(findField(model, "visibleBean"));
		Assertions.assertNull(findField(model, "secretGetterField"));
	}

	@Test
	public void testHiddenOnInterfaceOrSuperclassIsRespected() throws Exception {
		Assertions.assertTrue(ApiTool.apiIsHidden(HiddenByInterfaceApi.class));
		Assertions.assertTrue(ApiTool.apiIsHidden(HiddenBySuperclassApi.class));
		Assertions.assertFalse(ApiTool.apiIsHidden(ApiWithHiddenMethod.class));
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiWithHiddenMethod.class, HiddenByInterfaceApi.class, HiddenBySuperclassApi.class));
		Assertions.assertEquals(1, api.getAllApi().size());
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
