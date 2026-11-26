package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * An endpoint answering a {@code byte[]} in the Python client.
 */
public class TestPythonBinaryAnswer {

	@Path("/blobs")
	public static class BlobResource {
		@GET
		@Produces(MediaType.APPLICATION_OCTET_STREAM)
		public byte[] get() {
			return null;
		}
	}

	public static class Picture {
		public String name;
		public byte[] content;
	}

	@Path("/pictures")
	public static class PictureResource {
		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public Picture get() {
			return null;
		}

		@POST
		@Produces(MediaType.APPLICATION_JSON)
		public byte[] checksum(final Picture picture) {
			return null;
		}
	}

	@Path("/texts")
	public static class TextResource {
		@POST
		@Produces(MediaType.APPLICATION_JSON)
		public CharSequence put(final CharSequence body) {
			return null;
		}
	}

	@Test
	public void testByteArrayAnswerHasNoModelFile() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(BlobResource.class));

		final Map<java.nio.file.Path, String> generation = PythonGenerateApi.generateApi(api);

		for (final java.nio.file.Path path : generation.keySet()) {
			final String name = path.toString();
			Assertions.assertFalse(name.contains("[") || name.contains("]"), "invalid file name: " + name);
			Assertions.assertFalse(name.contains("char_sequence") || name.contains("CharSequence"),
					"junk model: " + name);
		}
		final String data = generation.get(Paths.get("api").resolve("blob_resource.py"));
		Assertions.assertNotNull(data, generation.keySet().toString());
		Assertions.assertTrue(data.contains(") -> bytes:"), data);
	}

	@Test
	public void testByteArrayOfJsonIsBase64() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(PictureResource.class));

		final Map<java.nio.file.Path, String> generation = PythonGenerateApi.generateApi(api);

		// Jackson writes it as base64 text: pydantic must decode it
		final String model = generation.get(Paths.get("model").resolve("picture.py"));
		Assertions.assertNotNull(model, generation.keySet().toString());
		Assertions.assertTrue(model.contains("from pydantic import "), model);
		Assertions.assertTrue(model.matches("(?s).*from pydantic import [^\\n]*Base64Bytes.*"), model);
		Assertions.assertTrue(model.contains("content: Base64Bytes"), model);
		final String data = generation.get(Paths.get("api").resolve("picture_resource.py"));
		Assertions.assertTrue(data.contains(") -> Base64Bytes:"), data);
		Assertions.assertTrue(data.matches("(?s).*from pydantic import [^\\n]*Base64Bytes.*"), data);
	}

	@Test
	public void testCharSequenceIsAString() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(TextResource.class));

		final Map<java.nio.file.Path, String> generation = PythonGenerateApi.generateApi(api);

		Assertions.assertFalse(generation.containsKey(Paths.get("model").resolve("char_sequence.py")),
				generation.keySet().toString());
		final String data = generation.get(Paths.get("api").resolve("text_resource.py"));
		Assertions.assertTrue(data.contains(") -> str:"), data);
	}
}
