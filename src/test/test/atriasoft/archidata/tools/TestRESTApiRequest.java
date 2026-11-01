package test.atriasoft.archidata.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.atriasoft.archidata.exception.RESTErrorResponseException;
import org.atriasoft.archidata.tools.RESTApiRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class TestRESTApiRequest {
	private static HttpServer server;
	private static String baseUrl;
	private static final AtomicReference<String> LAST_CUSTOM_HEADER = new AtomicReference<>();

	private static void reply(final com.sun.net.httpserver.HttpExchange exchange, final int status, final String body)
			throws IOException {
		final byte[] data = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, data.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(data);
		}
	}

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/ok", exchange -> {
			LAST_CUSTOM_HEADER.set(exchange.getRequestHeaders().getFirst("X-Custom"));
			reply(exchange, 200, "hello");
		});
		server.createContext("/echo", exchange -> reply(exchange, 200,
				new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
		server.createContext("/ko", exchange -> reply(exchange, 404,
				"{\"name\":\"Request Fail\",\"message\":\"nope\",\"time\":\"t\",\"status\":404,\"statusMessage\":\"Not Found\"}"));
		server.createContext("/html", exchange -> reply(exchange, 502, "<html>bad gateway</html>"));
		server.createContext("/slow", exchange -> {
			try {
				Thread.sleep(2_000);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			reply(exchange, 200, "late");
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
	}

	@Test
	void fetchStringReturnsOnSuccess() throws Exception {
		final HttpResponse<String> response = new RESTApiRequest(baseUrl + "/ok", null, null).fetchString();
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("hello", response.body());
	}

	@Test
	void fetchStringReturnsTheErrorResponse() throws Exception {
		// Raw accessor: the caller reads the status (karso and karideo tests rely on it).
		final HttpResponse<String> response = new RESTApiRequest(baseUrl + "/ko", null, null).fetchString();
		Assertions.assertEquals(404, response.statusCode());
	}

	@Test
	void fetchByteReturnsTheErrorResponse() throws Exception {
		final HttpResponse<byte[]> response = new RESTApiRequest(baseUrl + "/ko", null, null).fetchByte();
		Assertions.assertEquals(404, response.statusCode());
	}

	@Test
	void fetchClassThrowsOnError() {
		final RESTErrorResponseException ex = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> new RESTApiRequest(baseUrl + "/ko", null, null).fetch(String.class));
		Assertions.assertEquals(404, ex.status);
	}

	@Test
	void fetchByteReturnsOnSuccess() throws Exception {
		final HttpResponse<byte[]> response = new RESTApiRequest(baseUrl + "/ok", null, null).fetchByte();
		Assertions.assertEquals("hello", new String(response.body(), StandardCharsets.UTF_8));
	}

	@Test
	void fetchClassNonJsonErrorIsIOException() {
		Assertions.assertThrows(IOException.class,
				() -> new RESTApiRequest(baseUrl + "/html", null, null).fetch(String.class));
	}

	@Test
	void customHeaderIsSent() throws Exception {
		LAST_CUSTOM_HEADER.set(null);
		new RESTApiRequest(baseUrl + "/ok", null, null).header("X-Custom", "value42").fetchString();
		Assertions.assertEquals("value42", LAST_CUSTOM_HEADER.get());
	}

	// ===== credentials never logged in clear (D-15, W-22) =====

	@Test
	void credentialHeadersAreMasked() {
		Assertions.assertEquals("Bearer ***",
				RESTApiRequest.maskHeader("Authorization", "Bearer eyJhbGciOi.payload.sig"));
		Assertions.assertEquals("***", RESTApiRequest.maskHeader("apikey", "1234:secret"));
		Assertions.assertEquals("***", RESTApiRequest.maskHeader("Cookie", "session=abc"));
		Assertions.assertEquals("application/json", RESTApiRequest.maskHeader("Content-Type", "application/json"));
	}

	@Test
	void tokenQueryParametersAreMasked() {
		Assertions.assertEquals("http://h/data/1?Authorization=***&x=2",
				RESTApiRequest.maskUrl("http://h/data/1?Authorization=Bearer%20eyJ.a.b&x=2"));
		Assertions.assertEquals("http://h/data/1?x=2&ApiKey=***",
				RESTApiRequest.maskUrl("http://h/data/1?x=2&ApiKey=k"));
		Assertions.assertEquals("http://h/data/1?x=2", RESTApiRequest.maskUrl("http://h/data/1?x=2"));
	}

	@Test
	void requestTimesOut() {
		Assertions.assertThrows(java.net.http.HttpTimeoutException.class,
				() -> new RESTApiRequest(baseUrl + "/slow", null, null).timeout(java.time.Duration.ofMillis(300))
						.fetchString());
	}

	@Test
	void bodyByteSendsTheBytesItReceived() throws Exception {
		final byte[] given = "before".getBytes(StandardCharsets.UTF_8);
		final RESTApiRequest request = new RESTApiRequest(baseUrl + "/echo", null, null).post().bodyByte(given,
				"text/plain");
		System.arraycopy("after!".getBytes(StandardCharsets.UTF_8), 0, given, 0, given.length);
		Assertions.assertEquals("before", request.fetchString().body());
	}
}
