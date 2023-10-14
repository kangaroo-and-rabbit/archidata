package org.kar.archidata.util;

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
	
	public RESTApi(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public <T> List<T> gets(Class<T> clazz, String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		HttpRequest request = requestBuilding.GET().build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		List<T> out = mapper.readValue(httpResponse.body(), new TypeReference<List<T>>() {});
		return out;
	}
	
	public <T> T get(Class<T> clazz, String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		HttpRequest request = requestBuilding.GET().build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			//LOGGER.error("catch error from REST API: {}", httpResponse.body());
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw new RESTErrorResponseExeption(out.uuid, out.time, out.error, out.message, out.status, out.statusMessage);
		}
		//LOGGER.error("status code: {}", httpResponse.statusCode());
		//LOGGER.error("data: {}", httpResponse.body());
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
	
	public <T, U> T post(Class<T> clazz, String urlOffset, U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			LOGGER.error("status code: {}", httpResponse.statusCode());
			LOGGER.error("data: {}", httpResponse.body());
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
	
	public <T> T postMap(Class<T> clazz, String urlOffset, Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		HttpRequest request = requestBuilding.POST(BodyPublishers.ofString(body)).build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
	
	public <T, U> T put(Class<T> clazz, String urlOffset, U data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
	
	public <T> T putMap(Class<T> clazz, String urlOffset, Map<String, Object> data) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		String body = mapper.writeValueAsString(data);
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		requestBuilding = requestBuilding.header("Content-Type", "application/json");
		HttpRequest request = requestBuilding.PUT(BodyPublishers.ofString(body)).build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
	
	public <T, U> T delete(Class<T> clazz, String urlOffset) throws RESTErrorResponseExeption, IOException, InterruptedException {
		ObjectMapper mapper = new ObjectMapper();
		HttpClient client = HttpClient.newHttpClient();
		Builder requestBuilding = HttpRequest.newBuilder().uri(URI.create(this.baseUrl + urlOffset));
		if (token != null) {
			requestBuilding = requestBuilding.header(HttpHeaders.AUTHORIZATION, "Yota " + token);
		}
		HttpRequest request = requestBuilding.DELETE().build();
		HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
			RESTErrorResponseExeption out = mapper.readValue(httpResponse.body(), RESTErrorResponseExeption.class);
			throw out;
		}
		if (clazz.equals(String.class)) {
			return (T) httpResponse.body();
		}
		T out = mapper.readValue(httpResponse.body(), clazz);
		return out;
	}
}
