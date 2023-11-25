package org.kar.archidata.tools;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.kar.archidata.exception.RESTErrorResponseExeption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.HttpHeaders;

public class RESTApi {
	final static Logger LOGGER = LoggerFactory.getLogger(RESTApi.class);
	final String baseUrl;
	private String token = null;

	public RESTApi(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setToken(final String token) {
		this.token = token;
	}

	public <T> List<T> gets(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.GET().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		final List<T> out = mapper.readValue(httpResponse.body(), new TypeReference<List<T>>() {});
		return out;
	}

	public <T> T get(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.GET().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			// LOGGER.error("catch error from REST API: {}", httpResponse.body());
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw new RESTErrorResponseExeption(out.uuid, out.time, out.error, out.message, out.status, out.statusMessage);
		}
		// LOGGER.error("status code: {}", httpResponse.statusCode());
		// LOGGER.error("data: {}", httpResponse.body());
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}

	public <T, U> T post(final Class<T> clazz, final String urlOffset, final U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		final String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.error("status code: {}", httpResponse.statusCode());
			LOGGER.error("data: {}", httpResponse.body());
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}

	public <T> T postMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		final String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}

	public <T, U> T put(final Class<T> clazz, final String urlOffset, final U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		final String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}

	public <T> T putMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		final String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}

	public <T, U> T delete(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final ObjectMapper mapper = new ObjectMapper();
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.DELETE().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		final T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
}
