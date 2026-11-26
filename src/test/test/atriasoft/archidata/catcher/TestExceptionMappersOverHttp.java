package test.atriasoft.archidata.catcher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.GenericCatcher;
import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.exception.SystemException;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Characterization of what a client receives for each exception mapper registered by
 * {@link GenericCatcher#addAll}: status, content type and the fields of the JSON body. The server
 * is the Grizzly one on a free port, with a resource that throws on demand.
 */
public class TestExceptionMappersOverHttp {
	private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Set<String> BASE_KEYS = Set.of("oid", "name", "message", "time", "status", "statusMessage");

	public static class Body {
		public int value;
	}

	public static class Invalid {
		@NotNull
		public String alpha;
		@NotNull
		public String beta;
	}

	/** Throws what the path says. */
	@Path("/throw")
	@PermitAll
	@Produces(MediaType.APPLICATION_JSON)
	public static class ThrowingResource {
		@GET
		@Path("fail-default")
		public String failDefault() throws FailException {
			throw new FailException("the fail message");
		}

		@GET
		@Path("fail-conflict")
		public String failConflict() throws FailException {
			throw new FailException(Response.Status.CONFLICT, "already there");
		}

		@GET
		@Path("fail-cause")
		public String failCause() throws FailException {
			throw new FailException(Response.Status.BAD_GATEWAY, "upstream failed", new IllegalStateException("root"));
		}

		@GET
		@Path("system-default")
		public String systemDefault() throws SystemException {
			throw new SystemException("broken disk");
		}

		@GET
		@Path("system-unavailable")
		public String systemUnavailable() throws SystemException {
			throw new SystemException(Response.Status.SERVICE_UNAVAILABLE, "maintenance");
		}

		@GET
		@Path("input-default")
		public String inputDefault() throws InputException {
			throw new InputException("age", "must be positive");
		}

		@GET
		@Path("input-not-found")
		public String inputNotFound() throws InputException {
			throw new InputException(Response.Status.NOT_FOUND, "id", "unknown id");
		}

		@GET
		@Path("web-not-found")
		public String webNotFound() {
			throw new NotFoundException("media NOT FOUND");
		}

		@GET
		@Path("web-too-many")
		public String webTooMany() {
			throw new WebApplicationException("slow down", Response.status(429).build());
		}

		@GET
		@Path("web-retry-after")
		public String webRetryAfter() {
			throw new WebApplicationException("slow down",
					Response.status(429).header("Retry-After", "30").header("X-Rate-Remaining", "0").build());
		}

		@GET
		@Path("web-unauthorized")
		public String webUnauthorized() {
			throw new NotAuthorizedException("who are you", "Bearer realm=\"archidata\"");
		}

		@GET
		@Path("web-server-error")
		public String webServerError() {
			throw new InternalServerErrorException("secret detail of the server");
		}

		@GET
		@Path("unmanaged")
		public String unmanaged() {
			throw new IllegalStateException("secret detail of the unmanaged error");
		}

		@GET
		@Path("query")
		public String query(@QueryParam("count") final int count) {
			return "count=" + count;
		}

		@GET
		@Path("constraint-none")
		public String constraintNone() {
			throw new ConstraintViolationException("nothing wrong", Set.of());
		}

		@GET
		@Path("constraint-one")
		public String constraintOne() {
			final Invalid bean = new Invalid();
			bean.beta = "set";
			throw new ConstraintViolationException("one wrong", validator().validate(bean));
		}

		@GET
		@Path("constraint-two")
		public String constraintTwo() {
			throw new ConstraintViolationException("two wrong", validator().validate(new Invalid()));
		}

		@POST
		@Path("body")
		@Consumes(MediaType.APPLICATION_JSON)
		public String body(final Body body) {
			return "value=" + body.value;
		}
	}

	private static ValidatorFactory factory;
	private static HttpServer server;
	private static String base;

	private static Validator validator() {
		return factory.getValidator();
	}

	@BeforeAll
	public static void startServer() {
		factory = Validation.buildDefaultValidatorFactory();
		final ResourceConfig rc = new ResourceConfig();
		GenericCatcher.addAll(rc);
		rc.register(ThrowingResource.class);
		ContextGenericTools.registerArchidataJson(rc);
		rc.register(JacksonFeature.class);
		server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0/"), rc);
		base = "http://127.0.0.1:" + server.getListeners().iterator().next().getPort() + "/throw/";
	}

	@AfterAll
	public static void stopServer() {
		server.shutdownNow();
		factory.close();
	}

	private static HttpResponse<String> get(final String path) throws Exception {
		return CLIENT.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private static Set<String> keysOf(final JsonNode node) {
		final Set<String> out = new HashSet<>();
		node.fieldNames().forEachRemaining(out::add);
		return out;
	}

	/** The status, the JSON content type and the fields every error body carries. */
	private static JsonNode check(
			final HttpResponse<String> response,
			final int status,
			final String statusMessage,
			final String name,
			final String message) throws Exception {
		Assertions.assertEquals(status, response.statusCode(), response.body());
		Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
				response.headers().toString());
		final JsonNode body = MAPPER.readTree(response.body());
		Assertions.assertTrue(keysOf(body).containsAll(BASE_KEYS), body.toString());
		Assertions.assertEquals(status, body.get("status").asInt());
		Assertions.assertEquals(statusMessage, body.get("statusMessage").asText());
		Assertions.assertEquals(name, body.get("name").asText());
		Assertions.assertEquals(message, body.get("message").asText());
		Assertions.assertTrue(body.get("oid").asText().matches("[0-9a-f]{24}"), body.get("oid").toString());
		Assertions.assertDoesNotThrow(() -> Instant.parse(body.get("time").asText()));
		return body;
	}

	private static List<String> inputErrors(final JsonNode body) {
		final List<String> out = new ArrayList<>();
		for (final JsonNode error : body.get("inputError")) {
			out.add("argument=" + error.path("argument").asText("-") + " path=" + error.path("path").asText("-")
					+ " message=" + error.get("message").asText());
		}
		return out;
	}

	// ---------------------------------------------------------------- FailExceptionCatcher

	@Test
	public void testFailExceptionDefaultsToBadRequest() throws Exception {
		final JsonNode body = check(get("fail-default"), 400, "Bad Request", "Request Fail", "the fail message");
		Assertions.assertFalse(body.has("inputError"));
	}

	@Test
	public void testFailExceptionKeepsItsStatus() throws Exception {
		check(get("fail-conflict"), 409, "Conflict", "Request Fail", "already there");
	}

	@Test
	public void testFailExceptionCauseStaysOutOfTheAnswer() throws Exception {
		final HttpResponse<String> response = get("fail-cause");
		check(response, 502, "Bad Gateway", "Request Fail", "upstream failed");
		Assertions.assertFalse(response.body().contains("root"), response.body());
	}

	// ---------------------------------------------------------------- SystemExceptionCatcher

	@Test
	public void testSystemExceptionDefaultsToInternalServerError() throws Exception {
		final JsonNode body = check(get("system-default"), 500, "Internal Server Error", "System error", "broken disk");
		Assertions.assertFalse(body.has("inputError"));
	}

	@Test
	public void testSystemExceptionKeepsItsStatus() throws Exception {
		check(get("system-unavailable"), 503, "Service Unavailable", "System error", "maintenance");
	}

	// ---------------------------------------------------------------- InputExceptionCatcher

	@Test
	public void testInputExceptionDefaultsToBadRequestWithTheVariable() throws Exception {
		final JsonNode body = check(get("input-default"), 400, "Bad Request", "Constraint Violation",
				"must be positive");
		Assertions.assertEquals(List.of("argument=- path=age message=must be positive"), inputErrors(body));
	}

	@Test
	public void testInputExceptionKeepsItsStatus() throws Exception {
		final JsonNode body = check(get("input-not-found"), 404, "Not Found", "Constraint Violation", "unknown id");
		Assertions.assertEquals(List.of("argument=- path=id message=unknown id"), inputErrors(body));
	}

	// ---------------------------------------------------------------- WebApplicationExceptionCatcher

	@Test
	public void testWebApplicationExceptionOfAClientErrorKeepsItsMessage() throws Exception {
		final JsonNode body = check(get("web-not-found"), 404, "Not Found", "Catch system exception",
				"media NOT FOUND");
		Assertions.assertFalse(body.has("inputError"));
	}

	@Test
	public void testWebApplicationExceptionOfAStatusUnknownToTheEnum() throws Exception {
		check(get("web-too-many"), 429, "Too Many Requests", "Catch system exception", "slow down");
	}

	@Test
	public void testWebApplicationExceptionKeepsTheHeadersOfItsResponse() throws Exception {
		final HttpResponse<String> response = get("web-retry-after");
		check(response, 429, "Too Many Requests", "Catch system exception", "slow down");
		Assertions.assertEquals("30", response.headers().firstValue("Retry-After").orElse(null));
		Assertions.assertEquals("0", response.headers().firstValue("X-Rate-Remaining").orElse(null));
	}

	@Test
	public void testWebApplicationExceptionKeepsTheChallengeOfA401() throws Exception {
		final HttpResponse<String> response = get("web-unauthorized");
		check(response, 401, "Unauthorized", "Catch system exception", "who are you");
		Assertions.assertEquals("Bearer realm=\"archidata\"",
				response.headers().firstValue("WWW-Authenticate").orElse(null));
	}

	@Test
	public void testAMethodNotAllowedKeepsTheAllowHeader() throws Exception {
		final HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(URI.create(base + "query")).POST(HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(405, response.statusCode(), response.body());
		final String allow = response.headers().firstValue("Allow").orElse("");
		Assertions.assertTrue(allow.contains("GET"), "Allow header lost: '" + allow + "'");
	}

	@Test
	public void testWebApplicationExceptionOfAServerErrorHidesItsMessage() throws Exception {
		final HttpResponse<String> response = get("web-server-error");
		check(response, 500, "Internal Server Error", "Catch system exception", ExceptionCatcher.GENERIC_MESSAGE);
		Assertions.assertFalse(response.body().contains("secret"), response.body());
	}

	@Test
	public void testUnknownPathIsMappedAsAWebApplicationException() throws Exception {
		check(get("no-such-route"), 404, "Not Found", "Catch system exception", "HTTP 404 Not Found");
	}

	// ---------------------------------------------------------------- ExceptionCatcher

	@Test
	public void testUnmanagedExceptionHidesItsMessage() throws Exception {
		final HttpResponse<String> response = get("unmanaged");
		final JsonNode body = check(response, 500, "Internal Server Error", "Internal Server Error",
				ExceptionCatcher.GENERIC_MESSAGE);
		Assertions.assertFalse(body.has("inputError"));
		Assertions.assertFalse(response.body().contains("secret"), response.body());
	}

	// ---------------------------------------------------------------- QueryParamExceptionCatcher

	@Test
	public void testQueryParameterThatIsNotParsable() throws Exception {
		final HttpResponse<String> response = get("query?count=abc");
		final JsonNode body = check(response, 400, "Bad Request", "Error on query input='count'", "Input parsing fail");
		Assertions.assertEquals(1, body.get("inputError").size());
		final JsonNode error = body.get("inputError").get(0);
		Assertions.assertEquals("query", error.get("argument").asText());
		Assertions.assertEquals("count", error.get("path").asText());
		Assertions.assertTrue(error.get("message").asText().contains("abc"), error.toString());
		Assertions.assertEquals(Set.of("argument", "path", "message"), keysOf(error));
	}

	@Test
	public void testQueryParameterThatIsParsable() throws Exception {
		final HttpResponse<String> response = get("query?count=12");
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("count=12", response.body());
	}

	// ---------------------------------------------------------------- ConstraintViolationExceptionCatcher

	@Test
	public void testConstraintViolationWithoutViolation() throws Exception {
		final JsonNode body = check(get("constraint-none"), 400, "Bad Request", "Constraint Violation",
				"nothing wrong");
		Assertions.assertTrue(body.has("inputError"));
		Assertions.assertEquals(0, body.get("inputError").size());
	}

	@Test
	public void testConstraintViolationWithOneViolation() throws Exception {
		final Invalid bean = new Invalid();
		bean.beta = "set";
		final Set<ConstraintViolation<Invalid>> violations = validator().validate(bean);
		Assertions.assertEquals(1, violations.size());
		final JsonNode body = check(get("constraint-one"), 400, "Bad Request", "Error on input='alpha'", "one wrong");
		Assertions.assertEquals(List.of("argument=- path=alpha message=must not be null"), inputErrors(body));
	}

	@Test
	public void testConstraintViolationWithSeveralViolationsIsSorted() throws Exception {
		final JsonNode body = check(get("constraint-two"), 400, "Bad Request", "Multiple error on input", "two wrong");
		Assertions.assertEquals(List.of("argument=- path=alpha message=must not be null",
				"argument=- path=beta message=must not be null"), inputErrors(body));
	}

	// ---------------------------------------------------------------- RequestBodyExceptionCatcher

	@Test
	public void testRequestBodyOfAWrongTypeIsBadRequest() throws Exception {
		final HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(URI.create(base + "body")).header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString("{\"value\":\"abc\"}")).build(),
				HttpResponse.BodyHandlers.ofString());
		check(response, 400, "Bad Request", "Catch JSON Exception", "Invalid value for field 'value'");
	}

	@Test
	public void testRequestBodyWithAnUnknownFieldIsBadRequest() throws Exception {
		final HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(URI.create(base + "body")).header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString("{\"other\":1}")).build(),
				HttpResponse.BodyHandlers.ofString());
		check(response, 400, "Bad Request", "Catch JSON Exception", "Unknown field 'other'");
	}

	@Test
	public void testRequestBodyThatIsNotJsonIsBadRequest() throws Exception {
		final HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(URI.create(base + "body")).header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString("{not json")).build(),
				HttpResponse.BodyHandlers.ofString());
		final JsonNode body = check(response, 400, "Bad Request", "Catch JSON Exception",
				MAPPER.readTree(response.body()).get("message").asText());
		Assertions.assertTrue(body.get("message").asText().startsWith("Malformed JSON at line 1, column "),
				body.toString());
	}

	@Test
	public void testRequestBodyValid() throws Exception {
		final HttpResponse<String> response = CLIENT.send(
				HttpRequest.newBuilder(URI.create(base + "body")).header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString("{\"value\":7}")).build(),
				HttpResponse.BodyHandlers.ofString());
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("value=7", response.body());
	}
}
