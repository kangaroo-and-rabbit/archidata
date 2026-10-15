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
		server.createContext("/ko", exchange -> reply(exchange, 404,
				"{\"name\":\"Request Fail\",\"message\":\"nope\",\"time\":\"t\",\"status\":404,\"statusMessage\":\"Not Found\"}"));
		server.createContext("/html", exchange -> reply(exchange, 502, "<html>bad gateway</html>"));
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
}
