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
import java.util.ArrayList;
import java.util.Collections;
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

public class RESTApiRequest {
	final static Logger LOGGER = LoggerFactory.getLogger(RESTApiRequest.class);
	private final String url;
	private final String token;
	private final ObjectMapper mapper;
	private String serializedBodyString = null;
	private byte[] serializedBodyByte = null;
	private String contentType = null;
	private final Map<String, String> headers = new HashMap<>();
	private final Map<String, List<String>> queryParam = new HashMap<>();
	private String verb = "GET";
	private boolean showIOStrean = false;
	private boolean formatBody = false;

	String tryPrettyJson(final String data) {
		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			final JsonNode jsonNode = objectMapper.readTree(data);
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			return objectMapper.writeValueAsString(jsonNode);
		} catch (final Exception e) {
			return data;
		}
	}

	/**
	 * Constructor to initialize the RESTApiRequest with base URL and authorization token.
	 *
	 * @param url   The base URL of the API.
	 * @param token The Bearer token for authentication.
	 */
	public RESTApiRequest(final String url, final String token) {
		this.url = url;
		this.token = token;
		this.mapper = ContextGenericTools.createObjectMapper();
	}

	/**
	 * Sets the HTTP method (GET, POST, etc.).
	 *
	 * @param verb The HTTP verb to use.
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest verb(final String verb) {
		this.verb = verb;
		return this;
	}

	/**
	 * Sets the request body as a raw String.
	 *
	 * @param body The raw string body.
	 * @param contentType The content type of the request body.
	 * @return The updated RESTApiRequest instance.
	 */
	public <TYPE_BODY> RESTApiRequest bodyString(final String body, final String contentType) {
		this.serializedBodyByte = null;
		this.serializedBodyString = body;
		this.contentType = contentType;
		return this;
	}

	/**
	 * Sets the request body as a raw String.
	 *
	 * @param body The raw string body (consider as "text/plain").
	 * @return The updated RESTApiRequest instance.
	 */
	public <TYPE_BODY> RESTApiRequest bodyString(final String body) {
		this.serializedBodyByte = null;
		this.serializedBodyString = body;
		this.contentType = "text/plain";
		return this;
	}

	/**
	 * Serializes a Map to a JSON string and sets it as the body.
	 *
	 * @param data A map representing the body content.
	 * @return The updated RESTApiRequest instance.
	 * @throws JsonProcessingException If the map fails to serialize.
	 */
	public <TYPE_BODY> RESTApiRequest bodyMap(final Map<String, Object> data) throws JsonProcessingException {
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
	 * Sets the request body as a byte array.
	 *
	 * @param body        The byte array representing the body.
	 * @param contentType The content type of the body.
	 * @return The updated RESTApiRequest instance.
	 */
	public <TYPE_BODY> RESTApiRequest bodyByte(final byte[] body, final String contentType) {
		this.serializedBodyByte = body;
		this.serializedBodyString = null;
		this.contentType = contentType;
		return this;
	}

	/**
	 * Serializes a POJO into JSON and sets it as the body.
	 *
	 * @param body A Java object to serialize.
	 * @return The updated RESTApiRequest instance.
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
	 * Set data as a json body.
	 *
	 * @param body a serialized Json object.
	 * @return The updated RESTApiRequest instance.
	 */
	public <TYPE_BODY> RESTApiRequest bodyAsJson(final String body) {
		this.serializedBodyString = body;
		this.contentType = "application/json";
		return this;
	}

	/**
	 * Builds a multipart/form-data request body from a map of fields.
	 * Handles both File and standard form fields.
	 *
	 * @param body A map of key-values where values can be File or String.
	 * @return The updated RESTApiRequest instance.
	 * @throws IOException If reading files fails.
	 */
	public <TYPE_BODY> RESTApiRequest bodyMultipart(final Map<String, Object> body) throws IOException {
		this.serializedBodyString = null;
		LOGGER.trace("body (MULTIPART)");
		// Create multipart key element
		final String boundary = (new ObjectId()).toString();
		this.contentType = "multipart/form-data; boundary=" + boundary;
		// create the body;
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

	/**
	 * Adds a query parameter to the request URL (add an element in the key if the key already exist).
	 *
	 * @param paramKey The parameter name.
	 * @param paramValue The parameter value.
	 * @return The updated RESTApiRequest instance.
	 */
	public <TYPE_PARAM> RESTApiRequest queryParam(final String paramKey, final TYPE_PARAM paramValue) {
		final List<String> data = this.queryParam.getOrDefault(paramKey, new ArrayList<>());
		data.add(paramValue.toString());
		this.queryParam.put(paramKey, data);
		return this;
	}

	/**
	 * Adds a query parameter to the request URL (add an element in the key if the key already exist).
	 *
	 * @param paramKey The parameter name.
	 * @param paramValues The parameter values to add (multiple).
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest queryParam(final String paramKey, final String... paramValues) {
		final List<String> data = this.queryParam.getOrDefault(paramKey, new ArrayList<>());
		Collections.addAll(data, paramValues);
		this.queryParam.put(paramKey, data);
		return this;
	}

	/**
	 * Sets the HTTP verb to GET.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest get() {
		verb("GET");
		return this;
	}

	/**
	 * Sets the HTTP verb to PUT.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest put() {
		verb("PUT");
		return this;
	}

	/**
	 * Sets the HTTP verb to POST.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest post() {
		verb("POST");
		return this;
	}

	/**
	 * Sets the HTTP verb to PATCH.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest patch() {
		verb("PATCH");
		return this;
	}

	/**
	 * Sets the HTTP verb to DELETE.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest delete() {
		verb("DELETE");
		return this;
	}

	/**
	 * Sets the HTTP verb to ARCHIVE.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest archive() {
		verb("ARCHIVE");
		return this;
	}

	/**
	 * Sets the HTTP verb to RESTORE.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest restore() {
		verb("RESTORE");
		return this;
	}

	/**
	 * Sets the HTTP verb to VALL.
	 *
	 * @return The updated RESTApiRequest instance.
	 */
	public RESTApiRequest call() {
		verb("CALL");
		return this;
	}

	/**
	 * Sends the request and parses the response into a List of objects.
	 *
	 * @param clazz The class of each element in the expected response.
	 * @return List of parsed response objects.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException                If the request fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	public <TYPE_RESPONSE> List<TYPE_RESPONSE> fetchList(final Class<TYPE_RESPONSE> clazz)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return callAndParseRequestList(clazz);
	}

	/**
	 * Sends the request and parses the response into a single object.
	 *
	 * @param clazz The class of the expected response.
	 * @return Parsed response object.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException                If the request fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	public <TYPE_RESPONSE> TYPE_RESPONSE fetch(final Class<TYPE_RESPONSE> clazz)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return callAndParseRequest(clazz);
	}

	/**
	 * Sends the request and check errors.
	 *
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException                If the request fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	public void fetch() throws RESTErrorResponseException, IOException, InterruptedException {
		fetch(void.class);
	}

	/**
	 * Builds a query parameter string from a map of key-value pairs.
	 *
	 * <p>This method encodes each key and value using UTF-8 encoding to ensure that
	 * the resulting query string is safe for use in a URL. The encoded key-value pairs
	 * are then joined together with `&amp;` separators.</p>
	 *
	 * @param params A map containing query parameter names and their corresponding values.
	 *               Both keys and values will be URL-encoded.
	 * @return A URL-encoded query string.
	 */
	public static String buildQueryParams(final Map<String, List<String>> params) {
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
	 * Builds the final HttpRequest based on method, headers, and body.
	 *
	 * @return A built HttpRequest instance.
	 * @throws RESTErrorResponseException If error happens during body serialization.
	 * @throws IOException                If body serialization fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	public HttpRequest generateRequest() throws RESTErrorResponseException, IOException, InterruptedException {
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
		HttpRequest out = null;
		if (this.serializedBodyString != null) {
			out = requestBuilding.method(this.verb, BodyPublishers.ofString(this.serializedBodyString)).build();
		} else if (this.serializedBodyByte != null) {
			return requestBuilding.method(this.verb, BodyPublishers.ofByteArray(this.serializedBodyByte)).build();
		} else {
			out = requestBuilding.method(this.verb, BodyPublishers.ofString("")).build();
		}
		displayResquest(out);
		if (this.showIOStrean) {
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

	/**
	 * Performs a fetch request and returns the raw HttpResponse as bytes.
	 *
	 * @return Raw HTTP response in byte array.
	 * @throws IOException          If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 * @throws RESTErrorResponseException If an error occur in the server
	 */
	public HttpResponse<byte[]> fetchByte() throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpRequest request = generateRequest();
		final HttpClient client = HttpClient.newHttpClient();
		final HttpResponse<byte[]> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
		displayResponse(httpResponse);
		return httpResponse;
	}

	/**
	 * Performs a fetch request and returns the raw HttpResponse as string.
	 *
	 * @return Raw HTTP response in string.
	 * @throws IOException          If the request fails.
	 * @throws InterruptedException If the request is interrupted.
	 * @throws RESTErrorResponseException If an error occur in the server
	 */
	public HttpResponse<String> fetchString() throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpRequest request = generateRequest();
		final HttpClient client = HttpClient.newHttpClient();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

		displayResponse(httpResponse);

		return httpResponse;
	}

	/**
	 * Creates and initializes a HttpRequest.Builder with authorization and URL.
	 *
	 * @param urlOffset The URL path relative to the base URL.
	 * @return Initialized HttpRequest.Builder.
	 */
	private Builder createRequestBuilder(final String urlOffset) {
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.url + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		return requestBuilding;
	}

	/**
	 * Sends the request and parses the response as a single object.
	 *
	 * @param clazzReturn The expected class of the response.
	 * @param request     The built HttpRequest to send.
	 * @return Parsed object of the response.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException                If the response cannot be parsed or network fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	@SuppressWarnings("unchecked")
	private <TYPE_RESPONSE> TYPE_RESPONSE callAndParseRequest(final Class<TYPE_RESPONSE> clazzReturn)
			throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpResponse<String> httpResponse = fetchString();
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.trace("Receive Error: {}", httpResponse.body());
			try {
				final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseException.class);
				throw out;
			} catch (final InvalidDefinitionException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			} catch (final MismatchedInputException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			} catch (final JsonParseException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
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
	 * Display element of the response
	 * @param httpResponse response to display
	 */
	private void displayResponse(final HttpResponse<?> httpResponse) {
		if (!this.showIOStrean) {
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

	/**
	 * Display element of the response
	 * @param httpResponse response to display
	 */
	private void displayResquest(final HttpRequest httpRequest) {
		if (!this.showIOStrean) {
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
	 * Sends the request and parses the response as a list of objects.
	 *
	 * @param clazzReturn The class of the expected response elements.
	 * @param request     The HttpRequest to send.
	 * @return List of parsed response objects.
	 * @throws RESTErrorResponseException If an error response is received.
	 * @throws IOException                If the response cannot be parsed or network fails.
	 * @throws InterruptedException       If the request is interrupted.
	 */
	private <TYPE_RESPONSE> List<TYPE_RESPONSE> callAndParseRequestList(final Class<TYPE_RESPONSE> clazzReturn)
			throws IOException, InterruptedException, RESTErrorResponseException {
		final HttpResponse<String> httpResponse = fetchString();
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.trace("Receive Error: {}", httpResponse.body());
			try {
				final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseException.class);
				throw out;
			} catch (final InvalidDefinitionException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			} catch (final MismatchedInputException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			} catch (final JsonParseException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException("RestAPI Fail to parse the error " + ex.getClass().getName() + " ["
						+ httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		LOGGER.trace("Receive model: List<{}> with data: '{}'", clazzReturn.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(),
				this.mapper.getTypeFactory().constructCollectionType(List.class, clazzReturn));
	}

	public RESTApiRequest showIOStrean() {
		this.showIOStrean = true;
		return this;
	}

	public RESTApiRequest formatBody() {
		this.formatBody = true;
		return this;
	}

}
