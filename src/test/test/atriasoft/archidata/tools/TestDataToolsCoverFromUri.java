package test.atriasoft.archidata.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.model.Data;
import org.atriasoft.archidata.model.User;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.DataTools;
import org.atriasoft.archidata.tools.SafeHttpFetcher;
import org.atriasoft.archidata.tools.SsrfGuard;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import test.atriasoft.archidata.ConfigureDb;

/**
 * {@link DataTools#uploadCoverFromUri} against a local picture server. The fetcher of {@code DataTools} refuses the
 * loopback address: by default the call is refused, and one test replaces the fetcher by one that lets the test server
 * through (every other forbidden range stays refused), then puts the original back.
 */
class TestDataToolsCoverFromUri {
	private static HttpServer server;
	private static String baseUrl;
	private static Path workDir;

	/**
	 * Replaces the fetcher of {@code DataTools}. Reflection is needed here: the guard of {@code DataTools} refuses the
	 * loopback address and the test server is local, and the fetcher is a private static field of {@code DataTools} with
	 * no setter. The field is not final, so no final field is mutated.
	 */
	private static SafeHttpFetcher swapFetcher(final SafeHttpFetcher fetcher) throws Exception {
		final Field fetcherField = DataTools.class.getDeclaredField("remoteFetcher");
		fetcherField.setAccessible(true);
		final SafeHttpFetcher previous = (SafeHttpFetcher) fetcherField.get(null);
		fetcherField.set(null, fetcher);
		return previous;
	}

	private static byte[] png() throws IOException {
		final BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		return out.toByteArray();
	}

	@BeforeAll
	static void start() throws Exception {
		ConfigureDb.configure();
		workDir = Files.createTempDirectory("archidata-cover-from-uri");
		ConfigBaseVariable.setTmpDataFolder(workDir.resolve("tmp").toString());
		ConfigBaseVariable.setDataFolder(workDir.resolve("media").toString());
		Files.createDirectories(workDir.resolve("tmp"));
		Files.createDirectories(workDir.resolve("media"));
		final byte[] picture = png();
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/cover.png", exchange -> {
			exchange.getResponseHeaders().add("Content-Type", "image/png");
			exchange.sendResponseHeaders(200, picture.length);
			try (java.io.OutputStream os = exchange.getResponseBody()) {
				os.write(picture);
			}
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stop() throws Exception {
		try {
			server.stop(0);
		} finally {
			try {
				deleteTree(workDir);
			} finally {
				ConfigureDb.clear();
			}
		}
	}

	private static void deleteTree(final Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(root)) {
			for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	@Test
	void theLoopbackAddressIsRefusedByDefault() throws Exception {
		final User user = new User();
		user.setLogin("cover-owner-refused");
		final User stored = ConfigureDb.da.insert(user);
		Assertions.assertThrows(InputException.class, () -> DataTools.uploadCoverFromUri(ConfigureDb.da, User.class,
				stored.getOid(), baseUrl + "/cover.png"));
		final User read = DataAccess.getById(User.class, stored.getOid(), new ReadAllColumn());
		Assertions.assertTrue(read.getCovers() == null || read.getCovers().isEmpty());
	}

	@Test
	void theDownloadedPictureBecomesACoverOfTheEntity() throws Exception {
		final SafeHttpFetcher testFetcher = new SafeHttpFetcher(
				address -> !address.isLoopbackAddress() && SsrfGuard.isForbidden(address),
				SafeHttpFetcher.DEFAULT_CONNECT_TIMEOUT_MS, SafeHttpFetcher.DEFAULT_READ_TIMEOUT_MS,
				SafeHttpFetcher.DEFAULT_TOTAL_TIMEOUT_MS, SafeHttpFetcher.DEFAULT_MAX_REDIRECTS);
		final SafeHttpFetcher original = swapFetcher(testFetcher);
		try {
			final User user = new User();
			user.setLogin("cover-owner");
			final User stored = ConfigureDb.da.insert(user);
			DataTools.uploadCoverFromUri(ConfigureDb.da, User.class, stored.getOid(), baseUrl + "/cover.png");
			final User read = DataAccess.getById(User.class, stored.getOid(), new ReadAllColumn());
			Assertions.assertNotNull(read.getCovers(), "the picture is stored but not linked to the entity");
			Assertions.assertEquals(1, read.getCovers().size());
			final Data data = DataAccess.getById(Data.class, read.getCovers().get(0));
			Assertions.assertEquals("image/png", data.getMimeType());
		} finally {
			swapFetcher(original);
		}
	}
}
