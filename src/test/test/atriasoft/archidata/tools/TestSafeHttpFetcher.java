package test.atriasoft.archidata.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.tools.SafeHttpFetcher;
import org.atriasoft.archidata.tools.SsrfGuard;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jakarta.ws.rs.core.Response;

/**
 * {@link SafeHttpFetcher} against a local server. The test fetchers allow the loopback address (the
 * local server), every other forbidden range stays refused.
 */
class TestSafeHttpFetcher {
	private static HttpServer server;
	private static String baseUrl;
	/** Same as {@link SsrfGuard#isForbidden(InetAddress)}, except for the loopback of the test server. */
	private static final Predicate<InetAddress> ALLOW_LOOPBACK = address -> !address.isLoopbackAddress()
			&& SsrfGuard.isForbidden(address);

	private static void reply(final HttpExchange exchange, final int status, final byte[] data, final boolean chunked)
			throws IOException {
		exchange.getResponseHeaders().add("Content-Type", "image/png");
		exchange.sendResponseHeaders(status, chunked ? 0 : data.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(data);
		}
	}

	private static void redirect(final HttpExchange exchange, final String location) throws IOException {
		exchange.getResponseHeaders().add("Location", location);
		exchange.sendResponseHeaders(302, -1);
		exchange.close();
	}

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok",
				exchange -> reply(exchange, 200, "picture".getBytes(StandardCharsets.UTF_8), false));
		server.createContext("/chunked",
				exchange -> reply(exchange, 200, "chunked-picture".getBytes(StandardCharsets.UTF_8), true));
		server.createContext("/big", exchange -> reply(exchange, 200, new byte[4096], false));
		server.createContext("/bigChunked", exchange -> reply(exchange, 200, new byte[4096], true));
		server.createContext("/toOk", exchange -> redirect(exchange, "/ok"));
		server.createContext("/toMetadata", exchange -> redirect(exchange, "http://169.254.169.254/latest/meta-data/"));
		server.createContext("/toUla", exchange -> redirect(exchange, "http://[fd00:ec2::254]/"));
		server.createContext("/toFtp", exchange -> redirect(exchange, "ftp://example.com/file"));
		server.createContext("/loop", exchange -> redirect(exchange, "/loop"));
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2_000);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			reply(exchange, 200, "late".getBytes(StandardCharsets.UTF_8), false);
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	private static SafeHttpFetcher fetcher() {
		return new SafeHttpFetcher(ALLOW_LOOPBACK, 1_000, 1_000, 10_000, 3);
	}

	@Test
	void fetchesBodyAndContentType() throws Exception {
		final SafeHttpFetcher.Result result = fetcher().get(baseUrl + "/ok", 1024);
		Assertions.assertEquals(200, result.status());
		Assertions.assertEquals("image/png", result.contentType());
		Assertions.assertEquals("picture", new String(result.body(), StandardCharsets.UTF_8));
	}

	@Test
	void decodesChunkedBody() throws Exception {
		final SafeHttpFetcher.Result result = fetcher().get(baseUrl + "/chunked", 1024);
		Assertions.assertEquals("chunked-picture", new String(result.body(), StandardCharsets.UTF_8));
	}

	@Test
	void followsAllowedRedirection() throws Exception {
		final SafeHttpFetcher.Result result = fetcher().get(baseUrl + "/toOk", 1024);
		Assertions.assertEquals(200, result.status());
		Assertions.assertEquals("picture", new String(result.body(), StandardCharsets.UTF_8));
	}

	@Test
	void redirectionToMetadataIsRefused() {
		Assertions.assertThrows(InputException.class, () -> fetcher().get(baseUrl + "/toMetadata", 1024));
	}

	@Test
	void redirectionToUniqueLocalAddressIsRefused() {
		Assertions.assertThrows(InputException.class, () -> fetcher().get(baseUrl + "/toUla", 1024));
	}

	@Test
	void redirectionToOtherSchemeIsRefused() {
		Assertions.assertThrows(InputException.class, () -> fetcher().get(baseUrl + "/toFtp", 1024));
	}

	@Test
	void redirectionsAreBounded() throws Exception {
		final SafeHttpFetcher.Result result = fetcher().get(baseUrl + "/loop", 1024);
		Assertions.assertEquals(302, result.status());
	}

	@Test
	void noRedirectionWhenDisabled() throws Exception {
		final SafeHttpFetcher noRedirect = new SafeHttpFetcher(ALLOW_LOOPBACK, 1_000, 1_000, 10_000, 0);
		Assertions.assertEquals(302, noRedirect.get(baseUrl + "/toOk", 1024).status());
	}

	@Test
	void contentLengthOverLimitIsRefused() {
		final FailException ex = Assertions.assertThrows(FailException.class,
				() -> fetcher().get(baseUrl + "/big", 1024));
		Assertions.assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.status);
	}

	@Test
	void chunkedBodyOverLimitIsRefused() {
		final FailException ex = Assertions.assertThrows(FailException.class,
				() -> fetcher().get(baseUrl + "/bigChunked", 1024));
		Assertions.assertEquals(Response.Status.REQUEST_ENTITY_TOO_LARGE, ex.status);
	}

	@Test
	void slowServerTimesOut() {
		Assertions.assertThrows(IOException.class, () -> fetcher().get(baseUrl + "/slow", 1024));
	}

	@Test
	void defaultFetcherRefusesLoopback() {
		Assertions.assertThrows(InputException.class, () -> new SafeHttpFetcher().get(baseUrl + "/ok", 1024));
	}

	@Test
	void otherSchemeIsRefused() {
		Assertions.assertThrows(InputException.class, () -> fetcher().get("file:///etc/passwd", 1024));
	}
}
