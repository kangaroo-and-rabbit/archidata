package test.atriasoft.archidata.pagination;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.catcher.GenericCatcher;
import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.dataAccess.model.PaginationContext;
import org.atriasoft.archidata.pagination.PaginationFeature;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Characterization of the pagination at runtime: a server with {@link PaginationFeature}, a resource
 * returning {@link Pagination}, and what the client reads on the wire (body, {@code X-Total-Count},
 * {@code Link}, the way the offset and the limit of the request are resolved).
 */
public class TestPaginationOverHttp {
	private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final int TOTAL = 120;
	private static final String OFFSET = "X-Pagination-Offset";
	private static final String LIMIT = "X-Pagination-Limit";

	/** A collection of {@value #TOTAL} items served by slices. */
	@Path("/page")
	@PermitAll
	@Produces(MediaType.APPLICATION_JSON)
	public static class PagedResource {
		private static List<String> slice(final PaginationContext page, final int total) {
			final List<String> out = new ArrayList<>();
			for (long i = page.offset(); i < Math.min(total, page.offset() + page.limit()); i++) {
				out.add(String.format("item-%03d", i));
			}
			return out;
		}

		@GET
		@Path("items")
		public Pagination<String> items(
				@org.atriasoft.archidata.annotation.method.PaginationContext final PaginationContext page,
				@QueryParam("filter") final String filter) {
			return new Pagination<>(slice(page, TOTAL), TOTAL, page.offset(), page.limit());
		}

		@GET
		@Path("empty")
		public Pagination<String> empty(
				@org.atriasoft.archidata.annotation.method.PaginationContext final PaginationContext page) {
			return new Pagination<>(List.of(), 0, page.offset(), page.limit());
		}

		@GET
		@Path("echo")
		@Produces(MediaType.TEXT_PLAIN)
		public String echo(@org.atriasoft.archidata.annotation.method.PaginationContext final PaginationContext page) {
			return "offset=" + page.offset() + " limit=" + page.limit();
		}

		@GET
		@Path("plain")
		public List<String> plain() {
			return List.of("a", "b");
		}
	}

	private static HttpServer server;
	private static String base;

	@BeforeAll
	public static void startServer() {
		final ResourceConfig rc = new ResourceConfig();
		GenericCatcher.addAll(rc);
		rc.register(PaginationFeature.class);
		rc.register(PagedResource.class);
		ContextGenericTools.registerArchidataJson(rc);
		rc.register(JacksonFeature.class);
		server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0/"), rc);
		base = "http://127.0.0.1:" + server.getListeners().iterator().next().getPort() + "/page/";
	}

	@AfterAll
	public static void stopServer() {
		server.shutdownNow();
	}

	private static HttpResponse<String> get(final String path, final String... headers) throws Exception {
		final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path)).GET();
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	/** The ids of the items of the body, as "first..last/count". */
	private static String range(final HttpResponse<String> response) throws Exception {
		final JsonNode body = MAPPER.readTree(response.body());
		Assertions.assertTrue(body.isArray(), "the body is the plain list: " + response.body());
		if (body.isEmpty()) {
			return "empty";
		}
		return body.get(0).asText() + ".." + body.get(body.size() - 1).asText() + "/" + body.size();
	}

	private static String link(final String offset, final String limit, final String rel) {
		return "</page/items?" + LIMIT + "=" + limit + "&" + OFFSET + "=" + offset + ">; rel=\"" + rel + "\"";
	}

	@Test
	public void testDefaultFirstPage() throws Exception {
		final HttpResponse<String> response = get("items");
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
		Assertions.assertEquals("item-000..item-049/50", range(response));
		Assertions.assertEquals("120", response.headers().firstValue("X-Total-Count").orElse(null));
		Assertions.assertEquals(
				String.join(", ", link("0", "50", "first"), link("50", "50", "next"), link("100", "50", "last")),
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testMiddlePageFromTheHeaders() throws Exception {
		final HttpResponse<String> response = get("items", OFFSET, "50", LIMIT, "25");
		Assertions.assertEquals("item-050..item-074/25", range(response));
		Assertions.assertEquals("120", response.headers().firstValue("X-Total-Count").orElse(null));
		Assertions.assertEquals(String.join(", ", link("0", "25", "first"), link("25", "25", "prev"),
				link("75", "25", "next"), link("100", "25", "last")),
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testPageFromTheQueryString() throws Exception {
		final HttpResponse<String> response = get("items?" + OFFSET + "=50&" + LIMIT + "=25");
		Assertions.assertEquals("item-050..item-074/25", range(response));
		Assertions.assertEquals(String.join(", ", link("0", "25", "first"), link("25", "25", "prev"),
				link("75", "25", "next"), link("100", "25", "last")),
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testHeaderWinsOverTheQueryString() throws Exception {
		final HttpResponse<String> response = get("items?" + OFFSET + "=10&" + LIMIT + "=5", OFFSET, "20", LIMIT, "3");
		Assertions.assertEquals("item-020..item-022/3", range(response));
	}

	@Test
	public void testBlankHeaderFallsBackToTheQueryString() throws Exception {
		final HttpResponse<String> response = get("items?" + OFFSET + "=10&" + LIMIT + "=5", OFFSET, " ");
		Assertions.assertEquals("item-010..item-014/5", range(response));
	}

	@Test
	public void testLastPageIsShortAndHasNoNext() throws Exception {
		final HttpResponse<String> response = get("items", OFFSET, "100", LIMIT, "50");
		Assertions.assertEquals("item-100..item-119/20", range(response));
		Assertions.assertEquals(
				String.join(", ", link("0", "50", "first"), link("50", "50", "prev"), link("100", "50", "last")),
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testPageExactlyAtTheEndHasNoNext() throws Exception {
		final HttpResponse<String> response = get("items", OFFSET, "90", LIMIT, "30");
		Assertions.assertEquals("item-090..item-119/30", range(response));
		Assertions.assertEquals(
				String.join(", ", link("0", "30", "first"), link("60", "30", "prev"), link("90", "30", "last")),
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testOffsetPastTheEndGivesAnEmptyPage() throws Exception {
		final HttpResponse<String> response = get("items", OFFSET, "500", LIMIT, "50");
		Assertions.assertEquals(200, response.statusCode());
		Assertions.assertEquals("empty", range(response));
		Assertions.assertEquals("120", response.headers().firstValue("X-Total-Count").orElse(null));
		// the Link header is not pinned here: its prev link points at offset 450, itself past the end
		Assertions.assertTrue(response.headers().firstValue("Link").orElse("").endsWith(link("100", "50", "last")));
	}

	@Test
	public void testEmptyCollection() throws Exception {
		final HttpResponse<String> response = get("empty");
		Assertions.assertEquals("empty", range(response));
		Assertions.assertEquals("0", response.headers().firstValue("X-Total-Count").orElse(null));
		final String point = "</page/empty?" + LIMIT + "=50&" + OFFSET + "=0>; rel=\"";
		Assertions.assertEquals(point + "first\", " + point + "last\"",
				response.headers().firstValue("Link").orElse(null));
	}

	@Test
	public void testOtherQueryParametersAreKeptInTheLinks() throws Exception {
		final HttpResponse<String> response = get("items?filter=a%20b&" + LIMIT + "=100");
		Assertions.assertEquals("item-000..item-099/100", range(response));
		final String links = response.headers().firstValue("Link").orElse("");
		final String start = "</page/items?filter=a%20b&" + LIMIT + "=100&" + OFFSET + "=";
		Assertions.assertEquals(
				start + "0>; rel=\"first\", " + start + "100>; rel=\"next\", " + start + "100>; rel=\"last\"", links);
	}

	@Test
	public void testDefaultsWhenNothingIsGiven() throws Exception {
		Assertions.assertEquals("offset=0 limit=50", get("echo").body());
	}

	@Test
	public void testValuesOfTheRequestAreTaken() throws Exception {
		Assertions.assertEquals("offset=7 limit=9", get("echo", OFFSET, "7", LIMIT, "9").body());
		Assertions.assertEquals("offset=7 limit=9", get("echo?" + OFFSET + "=7&" + LIMIT + "=9").body());
	}

	@Test
	public void testLimitIsClampedToTheMaximum() throws Exception {
		Assertions.assertEquals("offset=0 limit=500", get("echo", LIMIT, "501").body());
		Assertions.assertEquals("offset=0 limit=500", get("echo", LIMIT, "99999999999").body());
		Assertions.assertEquals("offset=0 limit=500", get("echo", LIMIT, "500").body());
	}

	@Test
	public void testOffsetIsClampedToWhatSkipAccepts() throws Exception {
		Assertions.assertEquals("offset=" + Integer.MAX_VALUE + " limit=50", get("echo", OFFSET, "99999999999").body());
	}

	@Test
	public void testInvalidValuesFallBackToTheDefaults() throws Exception {
		Assertions.assertEquals("offset=0 limit=50", get("echo", OFFSET, "-1", LIMIT, "0").body());
		Assertions.assertEquals("offset=0 limit=50", get("echo", LIMIT, "-5").body());
		Assertions.assertEquals("offset=0 limit=50", get("echo", OFFSET, "abc", LIMIT, "1.5").body());
		Assertions.assertEquals("offset=0 limit=50", get("echo?" + OFFSET + "=x&" + LIMIT + "=y").body());
	}

	@Test
	public void testAnAnswerThatIsNotPaginatedCarriesNoPaginationHeader() throws Exception {
		final HttpResponse<String> response = get("plain");
		Assertions.assertEquals("[\"a\",\"b\"]", response.body());
		Assertions.assertTrue(response.headers().firstValue("X-Total-Count").isEmpty());
		Assertions.assertTrue(response.headers().firstValue("Link").isEmpty());
	}
}
