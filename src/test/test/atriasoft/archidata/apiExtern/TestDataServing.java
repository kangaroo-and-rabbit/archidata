package test.atriasoft.archidata.apiExtern;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.imageio.ImageIO;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.RESTApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** How {@code DataResource} serves the stored data: headers that keep an uploaded file from running on the API origin. */
public class TestDataServing {
	static WebLauncherTest webInterface = null;
	static RESTApi api = null;
	static Path workDir = null;
	/** HTTP/1.1 only: the default client tries an h2c upgrade, and the answer then loses its Content-Type. */
	static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		workDir = Files.createTempDirectory("archidata-data-serving");
		ConfigBaseVariable.setTmpDataFolder(workDir.resolve("tmp").toString());
		ConfigBaseVariable.setDataFolder(workDir.resolve("media").toString());
		webInterface = new WebLauncherTest();
		webInterface.process();
		api = new RESTApi(ConfigBaseVariable.getLocalAddress());
		api.setToken(Common.USER_TOKEN);
	}

	@AfterAll
	public static void stopWebServer() throws Exception {
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();
	}

	static byte[] png(final int width, final int height) throws IOException {
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	static String upload(final String fileName, final byte[] content) throws Exception {
		final File file = workDir.resolve(fileName).toFile();
		Files.write(file.toPath(), content);
		final String raw = api.request("data", "upload").post().bodyMultipart(Map.of("file", file)).fetch(String.class);
		return raw.replace("\"", "").trim();
	}

	static HttpResponse<byte[]> get(final String path) throws Exception {
		final HttpRequest request = HttpRequest.newBuilder(URI.create(ConfigBaseVariable.getLocalAddress() + path))
				.header("Authorization", "Bearer " + Common.USER_TOKEN).GET().build();
		return CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	@Test
	public void svgIsServedAsSandboxedAttachment() throws Exception {
		final String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(document.domain)</script></svg>";
		final String oid = upload("evil.svg", svg.getBytes(StandardCharsets.UTF_8));
		for (final String path : new String[] { "data/" + oid, "data/" + oid + "/evil.svg", "data/thumbnail/" + oid }) {
			final HttpResponse<byte[]> response = get(path);
			Assertions.assertEquals(200, response.statusCode(), path);
			if (!path.startsWith("data/thumbnail")) {
				Assertions.assertEquals("image/svg+xml", response.headers().firstValue("Content-Type").orElse(null),
						path);
			}
			Assertions.assertEquals("attachment", response.headers().firstValue("Content-Disposition").orElse(null),
					path);
			Assertions.assertEquals("sandbox; default-src 'none'",
					response.headers().firstValue("Content-Security-Policy").orElse(null), path);
			Assertions.assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(null),
					path);
		}
		// A range request takes the other branch of the stream builder.
		final HttpRequest ranged = HttpRequest
				.newBuilder(URI.create(ConfigBaseVariable.getLocalAddress() + "data/" + oid))
				.header("Authorization", "Bearer " + Common.USER_TOKEN).header("Range", "bytes=0-9").GET().build();
		final HttpResponse<byte[]> partial = CLIENT.send(ranged, HttpResponse.BodyHandlers.ofByteArray());
		Assertions.assertEquals(206, partial.statusCode());
		Assertions.assertEquals("attachment", partial.headers().firstValue("Content-Disposition").orElse(null));
	}

	@Test
	public void pictureIsServedInlineWithNosniff() throws Exception {
		final String oid = upload("picture.png", png(16, 8));
		final HttpResponse<byte[]> response = get("data/" + oid);
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("image/png", response.headers().firstValue("Content-Type").orElse(null));
		Assertions.assertTrue(response.headers().firstValue("Content-Disposition").isEmpty());
		Assertions.assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(null));
		final HttpResponse<byte[]> thumbnail = get("data/thumbnail/" + oid);
		Assertions.assertEquals(200, thumbnail.statusCode());
		Assertions.assertEquals("nosniff", thumbnail.headers().firstValue("X-Content-Type-Options").orElse(null));
	}
}
