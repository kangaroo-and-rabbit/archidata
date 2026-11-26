package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The {@code @Produces} values of an endpoint in the TypeScript client.
 */
public class TestTypeScriptProduces {

	@Path("media")
	public class MediaResource {
		@GET
		@Path("picture")
		@Produces({ "image/png", "image/jpeg" })
		public String picture() {
			return null;
		}

		@GET
		@Path("export")
		@Produces({ MediaType.APPLICATION_JSON, "text/csv", "application/x-unknown" })
		public String export() {
			return null;
		}

		@GET
		@Path("unknown")
		@Produces({ "application/x-one", "application/x-two" })
		public String unknown() {
			return null;
		}
	}

	private static String generateApiFile() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(MediaResource.class));
		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);
		return generation.get(Paths.get("api").resolve("media-resource.ts"));
	}

	@Test
	public void testKnownMediaTypesAreOffered() throws Exception {
		final String data = generateApiFile();
		Assertions.assertTrue(
				data.contains(
						"\t\tproduce: (typeof HTTPMimeType)['IMAGE_PNG'] | (typeof HTTPMimeType)['IMAGE_JPEG'],\n"),
				data);
		Assertions.assertTrue(
				data.contains("\t\tproduce: (typeof HTTPMimeType)['JSON'] | (typeof HTTPMimeType)['CSV'],\n"), data);
		Assertions.assertTrue(data.contains("accept: produce,"), data);
	}

	@Test
	public void testNoSyntaxErrorWhenNoMediaTypeIsKnown() throws Exception {
		final String data = generateApiFile();
		Assertions.assertFalse(data.contains("produce: ,"), data);
		// the endpoint "unknown" has neither the parameter nor the accept header
		final int start = data.indexOf("export function unknown(");
		Assertions.assertTrue(start > 0, data);
		final int end = data.indexOf("export function", start + 1);
		final String unknown = end < 0 ? data.substring(start) : data.substring(start, end);
		Assertions.assertFalse(unknown.contains("produce"), unknown);
		Assertions.assertFalse(unknown.contains("accept"), unknown);
	}
}
