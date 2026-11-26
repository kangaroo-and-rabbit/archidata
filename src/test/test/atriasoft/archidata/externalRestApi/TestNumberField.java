package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.atriasoft.archidata.externalRestApi.model.ModelGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A {@code Number} field of a model is generated as a floating number.
 */
public class TestNumberField {

	public static class Price {
		public Number amount;
	}

	@Path("/prices")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class PriceResource {
		@GET
		public Price get() {
			return null;
		}
	}

	@Test
	public void testNumberHasAModel() {
		final ModelGroup group = new ModelGroup();
		Assertions.assertNotNull(group.add(Number.class));
		Assertions.assertSame(group.add(Double.class), group.add(Number.class));
	}

	@Test
	public void testNumberIsADoubleInEveryGenerator() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(PriceResource.class));

		final Map<java.nio.file.Path, String> ts = TsGenerateApi.generateApi(api);
		Assertions.assertTrue(ts.get(Paths.get("model").resolve("price.ts")).contains("amount: ZodDouble"));

		final Map<java.nio.file.Path, String> py = PythonGenerateApi.generateApi(api);
		final String pyPrice = py.get(Paths.get("model").resolve("price.py"));
		Assertions.assertNotNull(pyPrice, py.keySet().toString());
		Assertions.assertTrue(pyPrice.contains("amount: float"), pyPrice);

		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");
		final Map<String, Object> schemas = map(map(spec.get("components")).get("schemas"));
		final Map<String, Object> amount = map(map(map(schemas.get("Price")).get("properties")).get("amount"));
		Assertions.assertEquals("number", amount.get("type"));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(final Object value) {
		return (Map<String, Object>) value;
	}
}
