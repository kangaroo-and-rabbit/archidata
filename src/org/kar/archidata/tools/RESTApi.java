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

import com.fasterxml.jackson.core.type.TypeReference;
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
	
	public <T> List<T> gets(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.GET().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		return this.mapper.readValue(httpResponse.body(), new TypeReference<List<T>>() {});
	}
	
	public <T> T get(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.GET().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			// LOGGER.error("catch error from REST API: {}", httpResponse.body());
			try {
				final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
				throw new RESTErrorResponseExeption(out.uuid, out.time, out.error, out.message, out.status, out.statusMessage);
			} catch (final MismatchedInputException ex) {
				throw new IOException("Fail to get the data [" + httpResponse.statusCode() + "] " + httpResponse.body());
			}
		}
		// LOGGER.error("status code: {}", httpResponse.statusCode());
		// LOGGER.error("data: {}", httpResponse.body());
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
	
	public <T, U> T post(final Class<T> clazz, final String urlOffset, final U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		final String body = this.mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.error("status code: {}", httpResponse.statusCode());
			LOGGER.error("data: {}", httpResponse.body());
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
	
	public <T> T postMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		final String body = this.mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
	
	public <T, U> T put(final Class<T> clazz, final String urlOffset, final U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final String body = this.mapper.writeValueAsString(data);
		return putJson(clazz, urlOffset, body);
	}
	
	public <T, U> T putJson(final Class<T> clazz, final String urlOffset, final String body) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		LOGGER.trace("call PUT: {}", URI.create(this.baseUrl + urlOffset));
		LOGGER.trace("DATA: {}", body);
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
	
	public <T> T putMap(final Class<T> clazz, final String urlOffset, final Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		final String body = this.mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		final HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
	
	public <T, U> T delete(final Class<T> clazz, final String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		final HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().version(Version.HTTP_1_1).uri(URI.create(this.baseUrl + urlOffset));
		if (this.token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + this.token);
		}
		final HttpRequest request = requestBuilding.DELETE().build();
		final HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			final RESTErrorResponseExeption out = this.mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		return this.mapper.readValue(httpResponse.body(), clazz);
	}
}
