package test.atriasoft.archidata.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
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
 * loopback address: its policy is relaxed for the test server only, every other forbidden range stays refused.
 */
class TestDataToolsCoverFromUri {
	private static HttpServer server;
	private static String baseUrl;
	private static Path workDir;
	private static Predicate<InetAddress> savedPolicy;

	/**
	 * Replaces the address policy of the fetcher of {@code DataTools}. Reflection is needed here: the guard of
	 * {@code DataTools} refuses the loopback address and the test server is local, and the fetcher is a private static
	 * final field of {@code DataTools} with no setter. This writes a private final field of {@code SafeHttpFetcher}, which
	 * JDK 26 and later warn about (JEP 500, "final field mutation"); a later JDK may refuse it.
	 */
	@SuppressWarnings("unchecked")
	private static void setFetcherPolicy(final Predicate<InetAddress> policy) throws Exception {
		final Field fetcherField = DataTools.class.getDeclaredField("REMOTE_FETCHER");
		fetcherField.setAccessible(true);
		final Object fetcher = fetcherField.get(null);
		final Field policyField = SafeHttpFetcher.class.getDeclaredField("forbidden");
		policyField.setAccessible(true);
		if (savedPolicy == null) {
			savedPolicy = (Predicate<InetAddress>) policyField.get(fetcher);
		}
		policyField.set(fetcher, policy);
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
		setFetcherPolicy(address -> !address.isLoopbackAddress() && SsrfGuard.isForbidden(address));
	}

	@AfterAll
	static void stop() throws Exception {
		try {
			server.stop(0);
		} finally {
			try {
				if (savedPolicy != null) {
					setFetcherPolicy(savedPolicy);
				}
			} finally {
				try {
					deleteTree(workDir);
				} finally {
					ConfigureDb.clear();
				}
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
	void theDownloadedPictureBecomesACoverOfTheEntity() throws Exception {
		final User user = new User();
		user.setLogin("cover-owner");
		final User stored = ConfigureDb.da.insert(user);
		DataTools.uploadCoverFromUri(ConfigureDb.da, User.class, stored.getOid(), baseUrl + "/cover.png");
		final User read = DataAccess.getById(User.class, stored.getOid(), new ReadAllColumn());
		Assertions.assertNotNull(read.getCovers(), "the picture is stored but not linked to the entity");
		Assertions.assertEquals(1, read.getCovers().size());
		final Data data = DataAccess.getById(Data.class, read.getCovers().get(0));
		Assertions.assertEquals("image/png", data.getMimeType());
	}
}
