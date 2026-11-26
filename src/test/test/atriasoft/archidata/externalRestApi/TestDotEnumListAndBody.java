package test.atriasoft.archidata.externalRestApi;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.DotGenerateApi;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * A {@code List} of enums and the body of an endpoint in the DOT diagram.
 */
public class TestDotEnumListAndBody {

	public enum Level {
		LOW, HIGH
	}

	public static class Settings {
		public List<Level> levels;
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@jakarta.ws.rs.Path("/settings")
	public static class SettingsResource {
		@POST
		public void create(final Settings body) {}

		@POST
		@jakarta.ws.rs.Path("upload")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		public void upload(
				@FormDataParam("file") final InputStream file,
				@FormDataParam("file") final FormDataContentDisposition fileMeta,
				@FormDataParam("title") final String title) {}
	}

	private static String generate(final Path dir) throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SettingsResource.class));
		final Path target = dir.resolve("architecture.dot");
		DotGenerateApi.generateApi(api, target.toString());
		return Files.readString(target);
	}

	@Test
	public void testListOfEnumsIsDrawnWithItsEnum(@TempDir final Path dir) throws Exception {
		final String graph = generate(dir);
		Assertions.assertTrue(graph.contains("<b> + levels</b>: List&lt;Level&gt;</td></tr>"), graph);
		Assertions.assertTrue(graph.contains("\tSettings:levels:e -> Level:NAME:w\n"), graph);
	}

	@Test
	public void testBodyOfAnEndpointHasItsType(@TempDir final Path dir) throws Exception {
		final String graph = generate(dir);
		Assertions.assertTrue(graph.contains("<b> + create(data: Settings): void</b>"), graph);
		Assertions.assertTrue(graph.contains("<b> + upload(data: {file: File, title: string}): void</b>"), graph);
	}
}
