package org.atriasoft.archidata.tools;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.exception.RESTErrorResponseException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.ws.rs.core.HttpHeaders;

public class RESTApi {
	static final Logger LOGGER = LoggerFactory.getLogger(RESTApi.class);
	final String baseUrl;
	private String tokenKey = null;
	private String token = null;
	final ObjectMapper mapper;
	boolean showIOStrean = false;

	public RESTApi(final String baseUrl) {
		this.baseUrl = baseUrl;
		this.mapper = ContextGenericTools.createObjectMapper();
	}

	public void showIOStrean() {
		this.showIOStrean = true;
	}

	public void setToken(final String token) {
		this.token = token;
		this.tokenKey = "Bearer";
	}

	public void setToken(final String token, final String tokenKey) {
		this.token = token;
		this.tokenKey = tokenKey;
	}

	public RESTApiRequest request() {
		return request("");
	}

	public RESTApiRequest request(final Object... urlOffset) {
		final StringBuilder url = new StringBuilder();
		url.append(this.baseUrl.replaceAll("/*$", ""));
		for (final Object elem : urlOffset) {
			if (elem == null) {
				continue;
			}
			url.append("/");
			url.append(elem.toString().replaceAll("/*$", ""));
		}
		final RESTApiRequest out = new RESTApiRequest(url.toString(), this.tokenKey, this.token);
		if (this.showIOStrean) {
			out.showIOStrean();
		}
		return out;
	}

	public <TYPE_RESPONSE> List<TYPE_RESPONSE> gets(final Class<TYPE_RESPONSE> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, this.tokenKey + " " + this.token);
		}
		final HttpRequest request = requestBuilding.GET().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			try {
				final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseException.class);
				throw out;
			} catch (final MismatchedInputException ex) {
				throw new IOException(
						"Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		return this.mapper.readValue(httpResponse.body(),
				this.mapper.getTypeFactory().constructCollectionType(List.class, clazz));
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE get(final Class<TYPE_RESPONSE> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendJson("GET", clazz, urlOffset, null);
	}

	public HttpResponse<byte[]> getRaw(final String urlOffset) throws IOException, InterruptedException {
		final Builder requestBuilding = createRequestBuilder(urlOffset);
		final HttpRequest request = requestBuilding.method("GET", BodyPublishers.ofString("")).build();
		final HttpClient client = HttpClient.newHttpClient();
		// client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
		return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
	}

	public <TYPE_RESPONSET, TYPE_BODY> TYPE_RESPONSET postMultipart(
			final Class<TYPE_RESPONSET> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendMultipart("POST", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSET, TYPE_BODY> TYPE_RESPONSET post(
			final Class<TYPE_RESPONSET> clazz,
			final String urlOffset,
			final TYPE_BODY data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSend("POST", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE postJson(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final String body) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendJson("POST", clazz, urlOffset, body);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE postMap(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendMap("POST", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE, TYPE_BODY> TYPE_RESPONSE put(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final TYPE_BODY data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSend("PUT", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE putJson(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final String body) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendJson("PUT", clazz, urlOffset, body);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE putMap(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendMap("PUT", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE putMultipart(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendMultipart("PUT", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE, TYPE_BODY> TYPE_RESPONSE patch(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final TYPE_BODY data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSend("PATCH", clazz, urlOffset, data);
	}

	public <TYPE_RESPONSE, TYPE_BODY> TYPE_RESPONSE patchJson(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final String body) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendJson("PATCH", clazz, urlOffset, body);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE patchMap(
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		return modelSendMap("PATCH", clazz, urlOffset, data);
	}

	protected <TYPE_RESPONSE, TYPE_BODY> TYPE_RESPONSE modelSend(
			final String model,
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final TYPE_BODY data) throws RESTErrorResponseException, IOException, InterruptedException {
		if (data == null) {
			return modelSendJson(model, clazz, urlOffset, null);
		} else {
			final String body = this.mapper.writeValueAsString(data);
			return modelSendJson(model, clazz, urlOffset, body);
		}
	}

	public Builder createRequestBuilder(final String urlOffset) {
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		return requestBuilding;
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE modelSendMultipart(
			final String model,
			final Class<TYPE_RESPONSE> clazzReturn,
			final String urlOffset,
			final Map<String, Object> params) throws RESTErrorResponseException, IOException, InterruptedException {

		LOGGER.trace("call (MULTIPART) {}: {}", model, URI.create(this.baseUrl + urlOffset));
		Builder requestBuilding = createRequestBuilder(urlOffset);

		// Create multipart key element
		final String boundary = (new ObjectId()).toString();
		requestBuilding = requestBuilding.header("Content-Type", "multipart/form-data; boundary=" + boundary);
		// create the body;
		final List<byte[]> bodyParts = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : params.entrySet()) {
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
		final byte[] finalBody = new byte[totalSize];
		int position = 0;
		for (final byte[] part : bodyParts) {
			System.arraycopy(part, 0, finalBody, position, part.length);
			position += part.length;
		}

		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofByteArray(finalBody)).build();
		return callAndParseRequest(clazzReturn, request);
	}

	public <TYPE_RESPONSE> TYPE_RESPONSE modelSendJson(
			final String model,
			final Class<TYPE_RESPONSE> clazzReturn,
			final String urlOffset,
			String body) throws RESTErrorResponseException, IOException, InterruptedException {
		LOGGER.trace("DATA: {}", body);
		Builder requestBuilding = createRequestBuilder(urlOffset);

		if (body == null) {
			body = "";
		} else {
			requestBuilding = requestBuilding.header("Content-Type", "application/json");
		}
		LOGGER.trace("publish body: {}", body);
		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofString(body)).build();
		return callAndParseRequest(clazzReturn, request);
	}

	@SuppressWarnings("unchecked")
	public <TYPE_RESPONSE> TYPE_RESPONSE callAndParseRequest(
			final Class<TYPE_RESPONSE> clazzReturn,
			final HttpRequest request) throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		// client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (clazzReturn == HttpResponse.class) {
			return (TYPE_RESPONSE) httpResponse;
		}
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
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
		if (clazzReturn == Void.class || clazzReturn == void.class) {
			return null;
		}
		if (clazzReturn.equals(String.class)) {
			return (TYPE_RESPONSE) httpResponse.body();
		}
		LOGGER.trace("Receive model: {} with data: '{}'", clazzReturn.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(), clazzReturn);
	}

	@SuppressWarnings("unchecked")
	public <TYPE_RESPONSE> TYPE_RESPONSE modelSendMap(
			final String model,
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		String body = null;
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		if (data == null) {
			body = "";
		} else {
			body = this.mapper.writeValueAsString(data);
			requestBuilding = requestBuilding.header("Content-Type", "application/json");
		}
		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			try {
				final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseException.class);
				throw out;
			} catch (final MismatchedInputException ex) {
				throw new IOException(
						"Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		if (clazz == Void.class || clazz == void.class) {
			return null;
		}
		if (clazz.equals(String.class)) {
			return (TYPE_RESPONSE) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}

	/**
	 * Call a DELETE on a REST API
	 * @param urlOffset Offset to call the API
	 */
	public void delete(final String urlOffset) throws RESTErrorResponseException, IOException, InterruptedException {
		delete(Void.class, urlOffset);
	}

	/**
	 * Call a DELETE on a REST API with retrieving some data
	 * @param <TYPE_RESPONSE> Type of data that might be received.
	 * @param clazz Class model of the data that might be parsed.
	 * @param urlOffset Offset to call the API
	 * @return The parsed object received.
	 */
	public <TYPE_RESPONSE> TYPE_RESPONSE delete(final Class<TYPE_RESPONSE> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return simpleRequest("DELETE", clazz, urlOffset);
	}

	/**
	 * Call an ARCHIVE on a REST API
	 * @param urlOffset Offset to call the API
	 */
	public void archive(final String urlOffset) throws RESTErrorResponseException, IOException, InterruptedException {
		archive(Void.class, urlOffset);
	}

	/**
	 * Call a ARCHIVE on a REST API with retrieving some data
	 * @param <TYPE_RESPONSE> Type of data that might be received.
	 * @param clazz Class model of the data that might be parsed.
	 * @param urlOffset Offset to call the API
	 * @return The parsed object received.
	 */
	public <TYPE_RESPONSE> TYPE_RESPONSE archive(final Class<TYPE_RESPONSE> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return simpleRequest("ARCHIVE", clazz, urlOffset);
	}

	/**
	 * Call an RESTORE on a REST API
	 * @param urlOffset Offset to call the API
	 */
	public void restore(final String urlOffset) throws RESTErrorResponseException, IOException, InterruptedException {
		restore(Void.class, urlOffset);
	}

	/**
	 * Call a RESTORE on a REST API with retrieving some data
	 * @param <T> Type of data that might be received.
	 * @param clazz Class model of the data that might be parsed.
	 * @param urlOffset Offset to call the API
	 * @return The parsed object received.
	 */
	public <T> T restore(final Class<T> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		return simpleRequest("RESTORE", clazz, urlOffset);
	}

	/**
	 * Call a key on a REST API with retrieving some data
	 * @param <TYPE_RESPONSE> Type of data that might be received.
	 * @param model name of the key for the REST call
	 * @param clazz Class model of the data that might be parsed.
	 * @param urlOffset Offset to call the API
	 * @return The parsed object received.
	 */
	@SuppressWarnings("unchecked")
	public <TYPE_RESPONSE> TYPE_RESPONSE simpleRequest(
			final String model,
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset) throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofString("")).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			try {
				final RESTErrorResponseException out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseException.class);
				throw out;
			} catch (final MismatchedInputException ex) {
				throw new IOException(
						"Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		if (clazz == Void.class || clazz == void.class) {
			return null;
		}
		if (clazz.equals(String.class)) {
			return (TYPE_RESPONSE) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
}
