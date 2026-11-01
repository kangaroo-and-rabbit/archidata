package org.atriasoft.archidata.tools;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

/**
 * Minimal HTTP/1.1 GET client for URLs that come from user input, protected against SSRF.
 *
 * <p>
 * Each hop of a fetch:
 * </p>
 * <ol>
 * <li>checks the URL syntax with {@link SsrfGuard#checkUriSyntax(URI)} (only {@code http} and {@code https});</li>
 * <li>resolves the host <b>once</b> and refuses it when any of its addresses is forbidden
 * ({@link SsrfGuard#isForbidden(InetAddress)} by default);</li>
 * <li>opens the TCP connection to that validated address itself, so a second DNS answer (DNS rebinding)
 * is never used. For {@code https} the TLS layer still checks the certificate against the host name
 * (SNI and endpoint identification);</li>
 * <li>never follows a redirection on its own: a {@code 3xx} answer with a {@code Location} goes through the
 * same checks, at most {@link #DEFAULT_MAX_REDIRECTS} times.</li>
 * </ol>
 * <p>
 * The connection, every read and the whole exchange are bounded in time, and the body is bounded in size
 * (a {@code Content-Length} over the limit is refused before reading anything).
 * </p>
 */
public class SafeHttpFetcher {
	private static final Logger LOGGER = LoggerFactory.getLogger(SafeHttpFetcher.class);
	/** Default connection timeout, in milliseconds. */
	public static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
	/** Default timeout of a single read on the socket, in milliseconds. */
	public static final int DEFAULT_READ_TIMEOUT_MS = 30_000;
	/** Default limit for the whole exchange (all redirections included), in milliseconds. */
	public static final int DEFAULT_TOTAL_TIMEOUT_MS = 120_000;
	/** Default number of redirections followed (each one validated again). */
	public static final int DEFAULT_MAX_REDIRECTS = 3;
	/** Largest status line plus headers accepted. */
	private static final int MAX_HEADER_BYTES = 32 * 1024;
	private static final int BUFFER_SIZE = 64 * 1024;

	/**
	 * Answer of a fetch.
	 * @param status The HTTP status of the last hop.
	 * @param contentType The {@code Content-Type} header of the last hop, or {@code null}.
	 * @param body The body of the last hop (empty when there is none).
	 */
	public record Result(
			int status,
			String contentType,
			byte[] body) {
		/**
		 * Compact constructor copying the body.
		 */
		public Result {
			body = body == null ? null : body.clone();
		}

		/**
		 * Returns the body of the last hop.
		 *
		 * @return a copy of the body: keep it in a variable rather than calling this method twice
		 */
		@Override
		public byte[] body() {
			return this.body == null ? null : this.body.clone();
		}
	}

	private final Predicate<InetAddress> forbidden;
	private final int connectTimeoutMs;
	private final int readTimeoutMs;
	private final int totalTimeoutMs;
	private final int maxRedirects;

	/** Creates a fetcher that refuses the addresses of {@link SsrfGuard#isForbidden(InetAddress)}, with the default limits. */
	public SafeHttpFetcher() {
		this(SsrfGuard::isForbidden, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, DEFAULT_TOTAL_TIMEOUT_MS,
				DEFAULT_MAX_REDIRECTS);
	}

	/**
	 * Creates a fetcher with its own address policy and limits.
	 * @param forbidden Tells which resolved addresses are refused (for example {@link SsrfGuard#isForbidden(InetAddress)}).
	 * @param connectTimeoutMs Connection timeout, in milliseconds.
	 * @param readTimeoutMs Timeout of a single read, in milliseconds.
	 * @param totalTimeoutMs Limit of the whole exchange, in milliseconds.
	 * @param maxRedirects Number of redirections followed (0 to follow none).
	 */
	public SafeHttpFetcher(final Predicate<InetAddress> forbidden, final int connectTimeoutMs, final int readTimeoutMs,
			final int totalTimeoutMs, final int maxRedirects) {
		this.forbidden = forbidden;
		this.connectTimeoutMs = connectTimeoutMs;
		this.readTimeoutMs = readTimeoutMs;
		this.totalTimeoutMs = totalTimeoutMs;
		this.maxRedirects = maxRedirects;
	}

	/**
	 * Fetches a URL with a GET request.
	 * @param url The URL, as given by the user.
	 * @param maxBodySize The largest body accepted, in bytes.
	 * @return The answer of the last hop (a {@code 3xx} is returned as is once the redirections are exhausted).
	 * @throws InputException If the URL, or one of its redirections, is refused by the SSRF checks.
	 * @throws FailException If the body is larger than {@code maxBodySize} (HTTP 413).
	 * @throws IOException If the connection fails or a timeout expires.
	 */
	public Result get(final String url, final long maxBodySize) throws InputException, FailException, IOException {
		final long deadline = System.nanoTime() + this.totalTimeoutMs * 1_000_000L;
		URI current = SsrfGuard.checkUrlSyntax(url);
		int redirects = 0;
		while (true) {
			final InetAddress address = SsrfGuard.resolveAllowed(current, this.forbidden);
			final Hop hop = fetchOnce(current, address, maxBodySize, deadline);
			if (isRedirect(hop.status) && hop.location != null && redirects < this.maxRedirects) {
				redirects++;
				final URI next;
				try {
					next = current.resolve(hop.location.trim());
				} catch (final IllegalArgumentException ex) {
					throw new InputException("url", "Invalid redirection");
				}
				current = SsrfGuard.checkUriSyntax(next);
				LOGGER.debug("Follow redirection {} to host {}", redirects, current.getHost());
				continue;
			}
			return new Result(hop.status, hop.contentType, hop.body);
		}
	}

	private static boolean isRedirect(final int status) {
		return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
	}

	private record Hop(
			int status,
			String contentType,
			String location,
			byte[] body) {}

	private Hop fetchOnce(final URI uri, final InetAddress address, final long maxBodySize, final long deadline)
			throws IOException, FailException {
		final boolean https = "https".equalsIgnoreCase(uri.getScheme());
		final int port = uri.getPort() == -1 ? (https ? 443 : 80) : uri.getPort();
		final String host = SsrfGuard.bareHost(uri);
		final Socket plain = new Socket();
		try {
			plain.connect(new InetSocketAddress(address, port), this.connectTimeoutMs);
			plain.setSoTimeout(this.readTimeoutMs);
			Socket socket = plain;
			if (https) {
				final SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				final SSLSocket ssl = (SSLSocket) factory.createSocket(plain, host, port, true);
				final SSLParameters parameters = ssl.getSSLParameters();
				parameters.setEndpointIdentificationAlgorithm("HTTPS");
				if (!isAddressLiteral(host)) {
					parameters.setServerNames(List.of(new SNIHostName(host)));
				}
				ssl.setSSLParameters(parameters);
				ssl.startHandshake();
				socket = ssl;
			}
			writeRequest(socket.getOutputStream(), uri, host, port, https);
			final InputStream in = new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE);
			int status;
			Map<String, String> headers;
			do {
				status = readStatus(in, deadline);
				headers = readHeaders(in, deadline);
			} while (status == 100);
			final String location = headers.get("location");
			final byte[] body = isRedirect(status) ? new byte[0] : readBody(in, headers, status, maxBodySize, deadline);
			return new Hop(status, headers.get("content-type"), location, body);
		} catch (final SocketTimeoutException ex) {
			throw new IOException("Timeout while fetching the remote data", ex);
		} finally {
			try {
				plain.close();
			} catch (final IOException ex) {
				LOGGER.debug("Fail to close the socket: {}", ex.getMessage());
			}
		}
	}

	private static boolean isAddressLiteral(final String host) {
		return host.indexOf(':') >= 0 || host.matches("[0-9.]+");
	}

	private static void writeRequest(
			final OutputStream out,
			final URI uri,
			final String host,
			final int port,
			final boolean https) throws IOException {
		String path = uri.getRawPath();
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		if (uri.getRawQuery() != null) {
			path += "?" + uri.getRawQuery();
		}
		String hostHeader = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
		if (port != (https ? 443 : 80)) {
			hostHeader += ":" + port;
		}
		final String request = "GET " + path + " HTTP/1.1\r\n" //
				+ "Host: " + hostHeader + "\r\n" //
				+ "User-Agent: archidata\r\n" //
				+ "Accept: */*\r\n" //
				+ "Accept-Encoding: identity\r\n" //
				+ "Connection: close\r\n\r\n";
		out.write(request.getBytes(StandardCharsets.US_ASCII));
		out.flush();
	}

	private static void checkDeadline(final long deadline) throws IOException {
		if (System.nanoTime() - deadline > 0) {
			throw new IOException("Timeout while fetching the remote data");
		}
	}

	private static String readLine(final InputStream in, final int[] budget, final long deadline) throws IOException {
		final ByteArrayOutputStream line = new ByteArrayOutputStream();
		while (true) {
			checkDeadline(deadline);
			final int value = in.read();
			if (value == -1) {
				throw new IOException("Connection closed in the HTTP header");
			}
			if (--budget[0] < 0) {
				throw new IOException("HTTP header too large");
			}
			if (value == '\n') {
				final byte[] raw = line.toByteArray();
				final int length = raw.length > 0 && raw[raw.length - 1] == '\r' ? raw.length - 1 : raw.length;
				return new String(raw, 0, length, StandardCharsets.ISO_8859_1);
			}
			line.write(value);
		}
	}

	private static int readStatus(final InputStream in, final long deadline) throws IOException {
		final int[] budget = { MAX_HEADER_BYTES };
		final String statusLine = readLine(in, budget, deadline);
		final String[] parts = statusLine.split(" ", 3);
		if (parts.length < 2 || !parts[0].startsWith("HTTP/1.")) {
			throw new IOException("Invalid HTTP status line");
		}
		try {
			return Integer.parseInt(parts[1]);
		} catch (final NumberFormatException ex) {
			throw new IOException("Invalid HTTP status", ex);
		}
	}

	private static Map<String, String> readHeaders(final InputStream in, final long deadline) throws IOException {
		final int[] budget = { MAX_HEADER_BYTES };
		final Map<String, String> headers = new HashMap<>();
		while (true) {
			final String line = readLine(in, budget, deadline);
			if (line.isEmpty()) {
				return headers;
			}
			final int colon = line.indexOf(':');
			if (colon <= 0) {
				continue;
			}
			final String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			final String value = line.substring(colon + 1).trim();
			headers.merge(name, value, (first, second) -> first + ", " + second);
		}
	}

	private static byte[] readBody(
			final InputStream in,
			final Map<String, String> headers,
			final int status,
			final long maxBodySize,
			final long deadline) throws IOException, FailException {
		if (status == 204 || status == 304) {
			return new byte[0];
		}
		final String transferEncoding = headers.get("transfer-encoding");
		if (transferEncoding != null && transferEncoding.toLowerCase(Locale.ROOT).contains("chunked")) {
			return readChunked(in, maxBodySize, deadline);
		}
		final String contentLength = headers.get("content-length");
		if (contentLength != null) {
			final long length;
			try {
				length = Long.parseLong(contentLength.trim());
			} catch (final NumberFormatException ex) {
				throw new IOException("Invalid Content-Length", ex);
			}
			if (length < 0) {
				throw new IOException("Invalid Content-Length");
			}
			if (length > maxBodySize) {
				throw tooLarge();
			}
			return readBounded(in, length, maxBodySize, deadline, true);
		}
		return readBounded(in, Long.MAX_VALUE, maxBodySize, deadline, false);
	}

	private static FailException tooLarge() {
		return new FailException(Response.Status.REQUEST_ENTITY_TOO_LARGE, "Remote data is too large");
	}

	private static byte[] readBounded(
			final InputStream in,
			final long expected,
			final long maxBodySize,
			final long deadline,
			final boolean exact) throws IOException, FailException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[BUFFER_SIZE];
		long remaining = expected;
		while (remaining > 0) {
			checkDeadline(deadline);
			final int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
			if (read == -1) {
				if (exact) {
					throw new IOException("Connection closed before the end of the body");
				}
				break;
			}
			if (out.size() + (long) read > maxBodySize) {
				throw tooLarge();
			}
			out.write(buffer, 0, read);
			remaining -= read;
		}
		return out.toByteArray();
	}

	private static byte[] readChunked(final InputStream in, final long maxBodySize, final long deadline)
			throws IOException, FailException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (true) {
			final int[] budget = { MAX_HEADER_BYTES };
			String sizeLine = readLine(in, budget, deadline);
			final int extension = sizeLine.indexOf(';');
			if (extension >= 0) {
				sizeLine = sizeLine.substring(0, extension);
			}
			final long size;
			try {
				size = Long.parseLong(sizeLine.trim(), 16);
			} catch (final NumberFormatException ex) {
				throw new IOException("Invalid chunk size", ex);
			}
			if (size < 0) {
				throw new IOException("Invalid chunk size");
			}
			if (size == 0) {
				// trailers, up to the empty line
				readHeaders(in, deadline);
				return out.toByteArray();
			}
			if (out.size() + size > maxBodySize) {
				throw tooLarge();
			}
			out.write(readBounded(in, size, maxBodySize, deadline, true));
			readLine(in, budget, deadline);
		}
	}
}
