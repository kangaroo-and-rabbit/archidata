package org.atriasoft.archidata.tools;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.atriasoft.archidata.exception.RESTErrorResponseException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.ws.rs.core.HttpHeaders;

/**
 * Fluent HTTP request builder for REST API calls.
 *
 * <p>Instances are created via {@link RESTApi#request(Object...)} and configured
 * using method chaining before executing with one of the {@code fetch} methods.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * api.request("users")
 *     .post()
 *     .bodyJson(newUser)
 *     .fetch(User.class);
 * }</pre>
 */
public class RESTApiRequest {
	static final Logger LOGGER = LoggerFactory.getLogger(RESTApiRequest.class);
	private final String url;
	private final String token;
	private final String tokenKey;
	private final ObjectMapper mapper;
	private String serializedBodyString = null;
	private byte[] serializedBodyByte = null;
	private String contentType = null;
	private final Map<String, String> headers = new HashMap<>();
	private final Map<String, List<String>> queryParam = new HashMap<>();
	private String verb = "GET";
	private boolean showIOStream = false;
	private boolean formatBody = false;

	/**
	 * Attempts to pretty-print a JSON string. Returns the original string if parsing fails.
	 * @param data The JSON string to format.
	 * @return Pretty-printed JSON, or the original string if not valid JSON.
	 */
	private String tryPrettyJson(final String data) {
		try {
			final JsonNode jsonNode = this.mapper.readTree(data);
			return this.mapper.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jsonNode);
		} catch (final Exception e) {
			return data;
		}
	}

	/**
	 * Creates a new RESTApiRequest with the given URL and authentication credentials.
	 *
	 * @param url The full URL for the request.
	 * @param tokenKey The authentication scheme (e.g. "Bearer"). Defaults to "Bearer" if null.
	 * @param token The authentication token value.
	 */
	public RESTApiRequest(final String url, final String tokenKey, final String token) {
		this.url = url;
		this.token = token;
		this.tokenKey = tokenKey != null ? tokenKey : "Bearer";
		this.mapper = ContextGenericTools.createObjectMapper();
	}

	// ====================================================================
	// HTTP verb setters
	// ====================================================================

	/**
	 * Sets the HTTP method (GET, POST, etc.).
	 * @param verb The HTTP verb to use.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest verb(final String verb) {
		this.verb = verb;
		return this;
	}

	/**
	 * Sets the HTTP verb to GET.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest get() {
		this.verb = "GET";
		return this;
	}

	/**
	 * Sets the HTTP verb to POST.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest post() {
		this.verb = "POST";
		return this;
	}

	/**
	 * Sets the HTTP verb to PUT.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest put() {
		this.verb = "PUT";
		return this;
	}

	/**
	 * Sets the HTTP verb to PATCH.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest patch() {
		this.verb = "PATCH";
		return this;
	}

	/**
	 * Sets the HTTP verb to DELETE.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest delete() {
		this.verb = "DELETE";
		return this;
	}

	/**
	 * Sets the HTTP verb to ARCHIVE.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest archive() {
		this.verb = "ARCHIVE";
		return this;
	}

	/**
	 * Sets the HTTP verb to RESTORE.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest restore() {
		this.verb = "RESTORE";
		return this;
	}

	/**
	 * Sets the HTTP verb to CALL.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest call() {
		this.verb = "CALL";
		return this;
	}

	// ====================================================================
	// Body setters
	// ====================================================================

	/**
	 * Sets the request body as a raw string with the given content type.
	 * @param body The raw string body.
	 * @param contentType The content type of the request body.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest bodyString(final String body, final String contentType) {
		this.serializedBodyByte = null;
		this.serializedBodyString = body;
		this.contentType = contentType;
		return this;
	}

	/**
	 * Sets the request body as a raw string with "text/plain" content type.
	 * @param body The raw string body.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest bodyString(final String body) {
		this.serializedBodyByte = null;
		this.serializedBodyString = body;
		this.contentType = "text/plain";
		return this;
	}

	/**
	 * Serializes a Map to JSON and sets it as the request body.
	 * @param data A map representing the body content.
	 * @return This request instance for chaining.
	 * @throws JsonProcessingException If the map fails to serialize.
	 */
	public RESTApiRequest bodyMap(final Map<String, Object> data) throws JsonProcessingException {
		this.serializedBodyByte = null;
		if (this.formatBody) {
			this.serializedBodyString = this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
		} else {
			this.serializedBodyString = this.mapper.writeValueAsString(data);
		}
		this.contentType = "application/json";
		return this;
	}

	/**
	 * Sets the request body as a byte array with the given content type.
	 * @param body The byte array body.
	 * @param contentType The content type of the body.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest bodyByte(final byte[] body, final String contentType) {
		this.serializedBodyByte = body;
		this.serializedBodyString = null;
		this.contentType = contentType;
		return this;
	}

	/**
	 * Serializes a POJO into JSON and sets it as the request body.
	 * @param <TYPE_BODY> The type of the body object.
	 * @param body A Java object to serialize.
	 * @return This request instance for chaining.
	 * @throws JsonProcessingException If serialization fails.
	 */
	public <TYPE_BODY> RESTApiRequest bodyJson(final TYPE_BODY body) throws JsonProcessingException {
		if (this.formatBody) {
			this.serializedBodyString = this.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
		} else {
			this.serializedBodyString = this.mapper.writeValueAsString(body);
		}
		this.contentType = "application/json";
		return this;
	}

	/**
	 * Sets a pre-serialized JSON string as the request body.
	 * @param body A pre-serialized JSON string.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest bodyAsJson(final String body) {
		this.serializedBodyString = body;
		this.contentType = "application/json";
		return this;
	}

	/**
	 * Builds a multipart/form-data request body from a map of fields.
	 * Values can be {@link File} (sent as binary) or any other object (sent as string).
	 * @param body A map of field names to values.
	 * @return This request instance for chaining.
	 * @throws IOException If reading files fails.
	 */
	public RESTApiRequest bodyMultipart(final Map<String, Object> body) throws IOException {
		this.serializedBodyString = null;
		LOGGER.trace("body (MULTIPART)");
		final String boundary = (new ObjectId()).toString();
		this.contentType = "multipart/form-data; boundary=" + boundary;
		final List<byte[]> bodyParts = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : body.entrySet()) {
			final StringBuilder partHeader = new StringBuilder();
			partHeader.append("--").append(boundary).append("\r\n");
			if (entry.getValue() instanceof File) {
				final File file = (File) entry.getValue();
				partHeader.append("Content-Disposition: form-data; name=\"").append(entry.getKey())
						.append("\"; filename=\"").append(file.getName()).append("\"\r\n");
				partHeader.append("Content-Type: application/octet-stream\r\n\r\n");
				bodyParts.add(partHeader.toString().getBytes());
				bodyParts.add(Files.readAllBytes(file.toPath()));
				bodyParts.add("\r\n".getBytes());
			} else {
				partHeader.append("Content-Disposition: form-data; name=\"").append(entry.getKey())
						.append("\"\r\n\r\n");
				if (entry.getValue() == null) {
					partHeader.append("null\r\n");
				} else {
					partHeader.append(entry.getValue().toString()).append("\r\n");
				}
				bodyParts.add(partHeader.toString().getBytes());
			}
		}
		bodyParts.add(("--" + boundary + "--\r\n").getBytes());

		final int totalSize = bodyParts.stream().mapToInt(b -> b.length).sum();
		this.serializedBodyByte = new byte[totalSize];
		int position = 0;
		for (final byte[] part : bodyParts) {
			System.arraycopy(part, 0, this.serializedBodyByte, position, part.length);
			position += part.length;
		}
		return this;
	}

	// ====================================================================
	// Query parameters
	// ====================================================================

	/**
	 * Adds query parameter(s) to the request URL.
	 * If the key already exists, the new values are appended.
	 * Supports {@link ZonedDateTime} and {@link Date} formatting.
	 * @param paramKey The parameter name.
	 * @param paramValues The parameter values to add.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest queryParam(final String paramKey, final Object... paramValues) {
		final List<String> data = this.queryParam.getOrDefault(paramKey, new ArrayList<>());
		for (final Object paramValue : paramValues) {
			if (paramValue == null) {
				data.add("null");
			} else if (paramValue instanceof final ZonedDateTime paramValueFormatted) {
				data.add(paramValueFormatted.format(DateTimeFormatter.ISO_DATE_TIME));
			} else if (paramValue instanceof final Date paramValueFormatted) {
				data.add(DateTimeFormatter.ISO_INSTANT.format(paramValueFormatted.toInstant()));
			} else {
				data.add(paramValue.toString());
			}
		}
		this.queryParam.put(paramKey, data);
		return this;
	}

	// ====================================================================
	// Configuration
	// ====================================================================

	/**
	 * Enables detailed HTTP request/response logging for this request.
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest showIOStream() {
		this.showIOStream = true;
		return this;
	}

	/**
	 * Enables pretty-printing of JSON bodies (for debugging).
	 * @return This request instance for chaining.
	 */
	public RESTApiRequest formatBody() {
		this.formatBody = true;
		return this;
	}

	// ====================================================================
	// Request execution
	// ====================================================================

	/**
	 * Sends the request and parses the response into a single object.
	 * @param <TYPE_RESPONSE> The expected response type.
	 * @param clazz The class of the expected response.
	 * @return Parsed response object.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 */
	public <TYPE_RESPONSE> TYPE_RESPONSE fetch(final Class<TYPE_RESPONSE> clazz)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return callAndParseRequest(clazz);
	}

	/**
	 * Sends the request and parses the response into a List of objects.
	 * @param <TYPE_RESPONSE> The expected element type.
	 * @param clazz The class of each element in the expected response.
	 * @return List of parsed response objects.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 */
	public <TYPE_RESPONSE> List<TYPE_RESPONSE> fetchList(final Class<TYPE_RESPONSE> clazz)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return callAndParseRequestList(clazz);
	}

	/**
	 * Sends the request without parsing the response body. Only checks for errors.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 */
	public void fetch() throws RESTErrorResponseException, IOException, InterruptedException {
		fetch(void.class);
	}

	/**
	 * Sends the request and returns the raw HTTP response as bytes.
	 * @return Raw HTTP response with byte array body.
	 * @throws IOException If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 * @throws RESTErrorResponseException If an error occurs in the server.
	 */
	public HttpResponse<byte[]> fetchByte() throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpRequest request = generateRequest();
		final HttpClient client = HttpClient.newHttpClient();
		final HttpResponse<byte[]> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		displayResponse(httpResponse);
		return httpResponse;
	}

	/**
	 * Sends the request and returns the raw HTTP response as a string.
	 * @return Raw HTTP response with string body.
	 * @throws IOException If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 * @throws RESTErrorResponseException If an error occurs in the server.
	 */
	public HttpResponse<String> fetchString() throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpRequest request = generateRequest();
		final HttpClient client = HttpClient.newHttpClient();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		displayResponse(httpResponse);
		return httpResponse;
	}

	/**
	 * Builds the final {@link HttpRequest} based on verb, headers, query parameters, and body.
	 * @return A built HttpRequest instance ready to be sent.
	 * @throws IOException If body serialization fails.
	 */
	public HttpRequest generateRequest() throws IOException {
		Builder requestBuilding = null;
		final String queryParams = buildQueryParams(this.queryParam);
		if (queryParams == null || queryParams.isEmpty()) {
			requestBuilding = createRequestBuilder("");
		} else {
			requestBuilding = createRequestBuilder("?" + queryParams);
		}
		if (this.contentType != null) {
			requestBuilding.header("Content-Type", this.contentType);
		}
		for (final Map.Entry<String, String> entry : this.headers.entrySet()) {
			requestBuilding.header(entry.getKey(), entry.getValue());
		}
		final HttpRequest out;
		if (this.serializedBodyString != null) {
			out = requestBuilding.method(this.verb, BodyPublishers.ofString(this.serializedBodyString)).build();
		} else if (this.serializedBodyByte != null) {
			out = requestBuilding.method(this.verb, BodyPublishers.ofByteArray(this.serializedBodyByte)).build();
		} else {
			out = requestBuilding.method(this.verb, BodyPublishers.ofString("")).build();
		}
		displayRequest(out);
		if (this.showIOStream) {
			if (this.serializedBodyString != null) {
				LOGGER.info("    content size: {}", this.serializedBodyString.length());
				LOGGER.info("    body(String): \"\"\"{}\"\"\"", tryPrettyJson(this.serializedBodyString));
			} else if (this.serializedBodyByte != null) {
				LOGGER.info("    content size: {}", this.serializedBodyByte.length);
				LOGGER.info("    body(byte[]): \"\"\"{}\"\"\"", this.serializedBodyByte);
			}
		}
		return out;
	}

	// ====================================================================
	// Internal helpers
	// ====================================================================

	/**
	 * Creates and initializes an HttpRequest.Builder with authorization and URL.
	 * @param urlOffset Query string to append (e.g. "?key=value").
	 * @return Initialized HttpRequest.Builder.
	 */
	private Builder createRequestBuilder(final String urlOffset) {
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.url + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, this.tokenKey + " " + this.token);
		}
		return requestBuilding;
	}

	/**
	 * Builds a URL-encoded query parameter string from a map of key-value pairs.
	 * @param params Query parameter names mapped to their values.
	 * @return A URL-encoded query string (without the leading "?").
	 */
	private static String buildQueryParams(final Map<String, List<String>> params) {
		final StringBuilder out = new StringBuilder();
		boolean first = true;
		for (final Entry<String, List<String>> param : params.entrySet()) {
			final String key = URLEncoder.encode(param.getKey(), StandardCharsets.UTF_8);
			for (final String value : param.getValue()) {
				if (!first) {
					out.append("&");
				}
				first = false;
				out.append(key);
				if (value != null) {
					out.append("=");
					out.append(URLEncoder.encode(value, StandardCharsets.UTF_8));
				}
			}
		}
		return out.toString();
	}

	/**
	 * Checks the HTTP response status and throws an appropriate exception for error responses (non-2xx).
	 * @param httpResponse The HTTP response to check.
	 * @throws RESTErrorResponseException If the response body can be parsed as an error.
	 * @throws IOException If the error response cannot be parsed.
	 */
	private void checkHttpError(final HttpResponse<String> httpResponse)
			throws RESTErrorResponseException, IOException {
		if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
			return;
		}
		LOGGER.trace("Receive Error: {}", httpResponse.body());
		try {
			final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
					RESTErrorResponseException.class);
			throw out;
		} catch (final InvalidDefinitionException ex) {
			LOGGER.error("RestAPI fail to parse error response, body: {}", httpResponse.body(), ex);
			throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
					+ httpResponse.statusCode() + "] " + httpResponse.body());
		} catch (final MismatchedInputException ex) {
			LOGGER.error("RestAPI fail to parse error response, body: {}", httpResponse.body(), ex);
			throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
					+ httpResponse.statusCode() + "] " + httpResponse.body());
		} catch (final JsonParseException ex) {
			LOGGER.error("RestAPI fail to parse error response, body: {}", httpResponse.body(), ex);
			throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
					+ httpResponse.statusCode() + "] " + httpResponse.body());
		}
	}

	/**
	 * Sends the request and parses the response as a single object.
	 * @param clazzReturn The expected class of the response.
	 * @return Parsed object of the response.
	 */
	@SuppressWarnings("unchecked")
	private <TYPE_RESPONSE> TYPE_RESPONSE callAndParseRequest(final Class<TYPE_RESPONSE> clazzReturn)
			throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpResponse<String> httpResponse = fetchString();
		checkHttpError(httpResponse);
		if (clazzReturn == Void.class || clazzReturn == void.class) {
			return null;
		}
		if (clazzReturn.equals(String.class)) {
			return (TYPE_RESPONSE) httpResponse.body();
		}
		LOGGER.trace("Receive model: {} with data: '{}'", clazzReturn.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(), clazzReturn);
	}

	/**
	 * Sends the request and parses the response as a list of objects.
	 * @param clazzReturn The class of the expected response elements.
	 * @return List of parsed response objects.
	 */
	private <TYPE_RESPONSE> List<TYPE_RESPONSE> callAndParseRequestList(final Class<TYPE_RESPONSE> clazzReturn)
			throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpResponse<String> httpResponse = fetchString();
		checkHttpError(httpResponse);
		LOGGER.trace("Receive model: List<{}> with data: '{}'", clazzReturn.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(),
				this.mapper.getTypeFactory().constructCollectionType(List.class, clazzReturn));
	}

	/**
	 * Logs the details of an outgoing HTTP request (if IO logging is enabled).
	 * @param httpRequest The request to display.
	 */
	private void displayRequest(final HttpRequest httpRequest) {
		if (!this.showIOStream) {
			return;
		}
		LOGGER.info("Request:");
		LOGGER.info("    verb: {}", httpRequest.method());
		if (httpRequest.version().isPresent()) {
			LOGGER.info("    version: {}", httpRequest.version().get());
		}
		LOGGER.info("    url: {}", httpRequest.uri());
		LOGGER.info("    headers:");
		for (final Map.Entry<String, List<String>> header : httpRequest.headers().map().entrySet()) {
			LOGGER.info("        - \"{}\": \"{}\"", header.getKey(), String.join(", ", header.getValue()));
		}
	}

	/**
	 * Logs the details of an HTTP response (if IO logging is enabled).
	 * @param httpResponse The response to display.
	 */
	private void displayResponse(final HttpResponse<?> httpResponse) {
		if (!this.showIOStream) {
			return;
		}
		LOGGER.info("Response:");
		LOGGER.info("    statusCode: {}", httpResponse.statusCode());
		LOGGER.info("    headers:");
		for (final Map.Entry<String, List<String>> header : httpResponse.headers().map().entrySet()) {
			LOGGER.info("        - \"{}\": \"{}\"", header.getKey(), String.join(", ", header.getValue()));
		}
		if (httpResponse.body() instanceof final String tmpBody) {
			LOGGER.info("    body: \"\"\"{}\"\"\"", tryPrettyJson(tmpBody));
		} else {
			LOGGER.info("    body: \"\"\"{}\"\"\"", httpResponse.body());
		}
	}

}
