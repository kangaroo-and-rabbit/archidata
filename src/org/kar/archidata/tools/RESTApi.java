package org.kar.archidata.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.kar.archidata.exception.RESTErrorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.ws.rs.core.HttpHeaders;

public class RESTApi {
	final static Logger LOGGER = LoggerFactory.getLogger(RESTApi.class);
	final String baseUrl;
	private String token = null;
	final ObjectMapper mapper;

	public RESTApi(final String baseUrl) {
		this.baseUrl = baseUrl;
		this.mapper = ContextGenericTools.createObjectMapper();
	}

	public void setToken(final String token) {
		this.token = token;
	}

	public <TYPE_RESPONSE> List<TYPE_RESPONSE> gets(final Class<TYPE_RESPONSE> clazz, final String urlOffset)
			throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
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

	@SuppressWarnings("unchecked")
	public <TYPE_RESPONSE> TYPE_RESPONSE modelSendJson(
			final String model,
			final Class<TYPE_RESPONSE> clazz,
			final String urlOffset,
			String body) throws RESTErrorResponseException, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		// client.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true);
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		LOGGER.trace("call {}: {}", model, URI.create(this.baseUrl + urlOffset));
		LOGGER.trace("DATA: {}", body);
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		if (body == null) {
			body = "";
		} else {
			requestBuilding = requestBuilding.header("Content-Type", "application/json");
		}
		LOGGER.trace("publish body: {}", body);
		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
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
		if (clazz == Void.class || clazz == void.class) {
			return null;
		}
		if (clazz.equals(String.class)) {
			return (TYPE_RESPONSE) httpResponse.body();
		}
		LOGGER.trace("Receive model: {} with data: '{}'", clazz.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(), clazz);
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
