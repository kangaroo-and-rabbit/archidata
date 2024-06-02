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

import org.kar.archidata.exception.RESTErrorResponseExeption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import jakarta.ws.rs.core.HttpHeaders;

public class RESTApi {
	final static Logger LOGGER = LoggerFactory.getLogger(RESTApi.class);
	final String baseUrl;
	private String token = null;
	final ObjectMapper mapper = new ObjectMapper();

	public RESTApi(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setToken(final String token) {
		this.token = token;
	}

	public <T> List<T> gets(final Class<T> clazz, final String urlOffset)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
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
				final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseExeption.class);
				throw out;
			} catch (final MismatchedInputException ex) {
				throw new IOException(
						"Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		//return this.mapper.readValue(httpResponse.body(), new TypeReference<List<T>>() {});
		return this.mapper.readValue(httpResponse.body(),
				this.mapper.getTypeFactory().constructCollectionType(List.class, clazz));
	}

	public <T> T get(final Class<T> clazz, final String urlOffset)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendJson("GET", clazz, urlOffset, null);
	}

	public <T, U> T post(final Class<T> clazz, final String urlOffset, final U data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSend("POST", clazz, urlOffset, data);
	}

	public <T, U> T postJson(final Class<T> clazz, final String urlOffset, final String body)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendJson("POST", clazz, urlOffset, body);
	}

	public <T> T postMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendMap("POST", clazz, urlOffset, data);
	}

	public <T, U> T put(final Class<T> clazz, final String urlOffset, final U data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSend("PUT", clazz, urlOffset, data);
	}

	public <T, U> T putJson(final Class<T> clazz, final String urlOffset, final String body)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendJson("PUT", clazz, urlOffset, body);
	}

	public <T> T putMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendMap("PUT", clazz, urlOffset, data);
	}

	public <T, U> T patch(final Class<T> clazz, final String urlOffset, final U data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSend("PATCH", clazz, urlOffset, data);
	}

	public <T, U> T patchJson(final Class<T> clazz, final String urlOffset, final String body)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendJson("PATCH", clazz, urlOffset, body);
	}

	public <T> T patchMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		return modelSendMap("PATCH", clazz, urlOffset, data);
	}

	protected <T, U> T modelSend(final String model, final Class<T> clazz, final String urlOffset, final U data)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		if (data == null) {
			return modelSendJson(model, clazz, urlOffset, null);
		} else {
			final String body = this.mapper.writeValueAsString(data);
			return modelSendJson(model, clazz, urlOffset, body);
		}
	}

	protected <T, U> T modelSendJson(final String model, final Class<T> clazz, final String urlOffset, String body)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
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
		final HttpRequest request = requestBuilding.method(model, BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.trace("Receive Error: {}", httpResponse.body());
			try {
				final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseExeption.class);
				throw out;
			} catch (final MismatchedInputException ex) {
				throw new IOException(
						"Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			} catch (final JsonParseException ex) {
				ex.printStackTrace();
				LOGGER.error("body: {}", httpResponse.body());
				throw new IOException(
						"Fail to get the ERROR data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		if (clazz == Void.class || clazz == void.class) {
			return null;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		LOGGER.trace("Receive model: {} with data: '{}'", clazz.getCanonicalName(), httpResponse.body());
		return this.mapper.readValue(httpResponse.body(), clazz);
	}

	protected <T> T modelSendMap(
			final String model,
			final Class<T> clazz,
			final String urlOffset,
			final Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
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
				final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseExeption.class);
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
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}

	/**
	 * Call a DELETE on a REST API
	 * @param urlOffset Offset to call the API
	 */
	public void delete(final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		delete(Void.class, urlOffset);
	}

	/**
	 * Call a DELETE on a REST API with retrieving some data
	 * @param <T> Type of data that might be received.
	 * @param clazz Class model of the data that might be parsed.
	 * @param urlOffset Offset to call the API
	 * @return The parsed object received.
	 */
	public <T> T delete(final Class<T> clazz, final String urlOffset)
			throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1)
				.uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		final HttpRequest request = requestBuilding.DELETE().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			try {
				final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(),
						RESTErrorResponseExeption.class);
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
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
}
