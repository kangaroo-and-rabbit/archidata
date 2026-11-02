package test.atriasoft.archidata.apiExtern;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.atriasoft.archidata.annotation.security.PermitTokenInURI;
import org.atriasoft.archidata.api.BackupResource;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.backup.BackupService;
import org.atriasoft.archidata.backup.BackupServiceBridge;
import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.GenericCatcher;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.backup.BackupTestTools;

/** {@code /backups}: an administrator sees and starts the backups, and nothing of the disk leaves the server. */
public class TestBackupResource {
	private static final String BASE_NAME = "resource_base";
	private static final String ROUTE = "backups";
	private static final long WAIT_SECONDS = 20;
	private static final String GET = "GET";
	private static final String POST = "POST";
	/** HTTP/1.1 only: the default client tries an h2c upgrade. */
	private static final HttpClient CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	private static final ObjectMapper MAPPER = ContextGenericTools.createObjectMapper();

	private static WebLauncherTest webInterface = null;

	private Path folder;

	/** An engine whose {@code store} waits to be released: a backup that stays in progress. */
	private static final class BlockedEngine extends BackupEngine {
		final CountDownLatch release = new CountDownLatch(1);

		BlockedEngine(final Path folder) {
			super(folder, BASE_NAME, EngineBackupType.JSON_EXTENDED);
		}

		@Override
		public Date store(final String sequence) throws IOException, DataAccessException {
			try {
				Assertions.assertTrue(this.release.await(WAIT_SECONDS, TimeUnit.SECONDS));
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			return new Date();
		}
	}

	@BeforeAll
	public static void startServer() throws Exception {
		ConfigureDb.configure();
		webInterface = new WebLauncherTest();
		webInterface.process();
	}

	@AfterAll
	public static void stopServer() throws Exception {
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();
	}

	@BeforeEach
	public void configureService() throws Exception {
		this.folder = Files.createTempDirectory("test_backup_resource_");
		BackupResource.setService(BackupServiceBridge.on(this.folder, BASE_NAME, List.of(), List.of()));
	}

	@AfterEach
	public void removeService() throws IOException {
		BackupResource.setService(null);
		if (Files.exists(this.folder)) {
			Files.setPosixFilePermissions(this.folder, PosixFilePermissions.fromString("rwx------"));
			BackupTestTools.deleteRecursive(this.folder);
		}
	}

	private static HttpResponse<String> call(final String method, final String path, final String token)
			throws Exception {
		return call(method, URI.create(ConfigBaseVariable.getLocalAddress() + path), token);
	}

	private static HttpResponse<String> call(final String method, final URI address, final String token)
			throws Exception {
		final HttpRequest.Builder builder = HttpRequest.newBuilder(address).method(method,
				HttpRequest.BodyPublishers.noBody());
		if (token != null) {
			builder.header("Authorization", "Bearer " + token);
		}
		return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static JsonNode awaitIdle() throws Exception {
		final long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
		while (true) {
			final JsonNode state = MAPPER.readTree(call(GET, ROUTE, Common.ADMIN_TOKEN).body());
			if (!state.get("running").asBoolean()) {
				return state;
			}
			Assertions.assertTrue(System.nanoTime() < limit, "the backup did not finish");
			Thread.sleep(5);
		}
	}

	private static Set<String> keysOf(final JsonNode node) {
		final Set<String> out = new HashSet<>();
		node.fieldNames().forEachRemaining(out::add);
		return out;
	}

	/** A token as karso signs it: the codes as integers under the application, USER included, the ages as rights. */
	private static String tokenWithAdminRole(final PartRight admin) {
		final Map<String, Integer> applicationRoles = new HashMap<>();
		applicationRoles.put("ADMIN", admin.getValue());
		applicationRoles.put("USER", PartRight.READ_WRITE.getValue());
		final Map<String, Object> roles = new HashMap<>();
		roles.put(TestAuthenticationFilter.APPLICATION_NAME, applicationRoles);
		final Map<String, Object> rights = new HashMap<>();
		rights.put(TestAuthenticationFilter.APPLICATION_NAME, Map.of("AGE_18", PartRight.READ_WRITE.getValue()));
		return JWTWrapper.generateJWToken(Common.ADMIN_ID, "karso_login", "Karso",
				TestAuthenticationFilter.APPLICATION_NAME, roles, rights, 60);
	}

	private static void assertAccess(final PartRight admin, final int get, final int post) throws Exception {
		final String token = tokenWithAdminRole(admin);
		Assertions.assertEquals(get, call(GET, ROUTE, token).statusCode(), "GET with ADMIN " + admin);
		Assertions.assertEquals(post, call(POST, ROUTE, token).statusCode(), "POST with ADMIN " + admin);
		awaitIdle();
	}

	@Test
	public void testAdminInReadOnlySeesButDoesNotStart() throws Exception {
		assertAccess(PartRight.READ, 200, 403);
		Assertions.assertEquals(0, MAPPER.readTree(call(GET, ROUTE, Common.ADMIN_TOKEN).body()).get("archives").size());
	}

	@Test
	public void testAdminInReadWriteSeesAndStarts() throws Exception {
		assertAccess(PartRight.READ_WRITE, 200, 202);
	}

	@Test
	public void testAdminInWriteOnlyStartsButDoesNotSee() throws Exception {
		assertAccess(PartRight.WRITE, 403, 202);
	}

	@Test
	public void testAdminWithoutRightGetsNothing() throws Exception {
		assertAccess(PartRight.NONE, 403, 403);
	}

	/** {@code @RolesAllowed} alone checks nothing: an application that forgets the filter must fail closed. */
	@Test
	public void testWithoutAuthenticationFilterEveryRouteIsRefused() throws Exception {
		final AtomicInteger stores = new AtomicInteger();
		final BackupService service = BackupServiceBridge
				.on(new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED) {
					@Override
					public Date store(final String sequence) {
						stores.incrementAndGet();
						return new Date();
					}
				});
		BackupResource.setService(service);
		final ResourceConfig rc = new ResourceConfig();
		GenericCatcher.addAll(rc);
		rc.register(BackupResource.class);
		ContextGenericTools.addJsr310(rc);
		rc.register(JacksonFeature.class);
		final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://127.0.0.1:0/"), rc);
		try {
			final int port = server.getListeners().iterator().next().getPort();
			final URI address = URI.create("http://127.0.0.1:" + port + "/" + ROUTE);
			for (final String method : List.of(GET, POST)) {
				Assertions.assertEquals(403, call(method, address, null).statusCode(), method);
				Assertions.assertEquals(403, call(method, address, Common.ADMIN_TOKEN).statusCode(), method);
			}
		} finally {
			server.shutdownNow();
		}
		Assertions.assertEquals(0, stores.get());
		Assertions.assertNull(service.getState().lastAttempt());
	}

	/** Run an action and return what the resource logged, at INFO and above. */
	private static List<ILoggingEvent> logsOf(final Executable action) throws Throwable {
		final Logger logger = (Logger) LoggerFactory.getLogger(BackupResource.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			action.execute();
		} finally {
			logger.detachAppender(appender);
		}
		return appender.list;
	}

	private static void assertTraced(final List<ILoggingEvent> logs, final Level level, final String result) {
		Assertions.assertEquals(1, logs.size(), logs.toString());
		final String line = logs.get(0).getFormattedMessage();
		Assertions.assertEquals(level, logs.get(0).getLevel(), line);
		Assertions.assertTrue(line.contains(Common.ADMIN_ID.toHexString()), line);
		Assertions.assertTrue(line.contains(result), line);
		Assertions.assertFalse(line.contains(Common.ADMIN_TOKEN), line);
		Assertions.assertFalse(line.contains("Bearer"), line);
	}

	@Test
	public void testAStartThatRunsIsTracedWithItsAuthor() throws Throwable {
		assertTraced(logsOf(() -> Assertions.assertEquals(202, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode())),
				Level.INFO, "STARTED");
		awaitIdle();
	}

	@Test
	public void testAStartRefusedBecauseOneRunsIsTraced() throws Throwable {
		final BlockedEngine engine = new BlockedEngine(this.folder);
		BackupResource.setService(BackupServiceBridge.on(engine));
		try {
			Assertions.assertEquals(202, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode());
			assertTraced(logsOf(() -> Assertions.assertEquals(409, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode())),
					Level.WARN, "ALREADY_RUNNING");
		} finally {
			engine.release.countDown();
		}
		awaitIdle();
	}

	@Test
	public void testAStartRefusedForLackOfSpaceIsTraced() throws Throwable {
		BackupResource.setService(BackupServiceBridge
				.on(new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED), () -> 0));
		assertTraced(logsOf(() -> Assertions.assertEquals(507, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode())),
				Level.WARN, "INSUFFICIENT_SPACE");
	}

	@Test
	public void testAStartThatCanNotBeCheckedIsTraced() throws Throwable {
		Files.delete(this.folder);
		assertTraced(logsOf(() -> Assertions.assertEquals(500, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode())),
				Level.WARN, "FAILED");
	}

	/** The nightly task does not pass through the resource: a line here always names a person. */
	@Test
	public void testTheNightlyTaskIsNotTracedAsAManualStart() throws Throwable {
		final BackupService service = BackupServiceBridge.on(this.folder, BASE_NAME, List.of(), List.of());
		BackupResource.setService(service);
		Assertions.assertEquals(List.of(), logsOf(service::startAsync));
		awaitIdle();
	}

	@Test
	public void testWithoutTokenIsRefused() throws Exception {
		Assertions.assertEquals(401, call(GET, ROUTE, null).statusCode());
		Assertions.assertEquals(401, call(POST, ROUTE, null).statusCode());
	}

	@Test
	public void testATokenInTheUriIsRefused() throws Exception {
		final String path = ROUTE + "?Authorization=Bearer%20" + Common.ADMIN_TOKEN;
		Assertions.assertEquals(401, call(GET, path, null).statusCode());
		Assertions.assertEquals(401, call(POST, path, null).statusCode());
	}

	@Test
	public void testUserRoleIsRefused() throws Exception {
		Assertions.assertEquals(403, call(GET, ROUTE, Common.USER_TOKEN).statusCode());
		Assertions.assertEquals(403, call(POST, ROUTE, Common.USER_TOKEN).statusCode());
		// The refused POST started nothing.
		final JsonNode state = awaitIdle();
		Assertions.assertTrue(state.get("lastAttempt").isNull());
		Assertions.assertEquals(0, state.get("archives").size());
	}

	@Test
	public void testAdminStartsABackupAndFollowsIt() throws Exception {
		final HttpResponse<String> list = call(GET, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(200, list.statusCode());
		final HttpResponse<String> start = call(POST, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(202, start.statusCode());
		// rest-tools.ts parses a JSON Content-Type: an empty body under it would fail the generated client.
		Assertions.assertEquals("", start.body());
		Assertions.assertTrue(start.headers().firstValue("Content-Type").isEmpty(), start.headers().toString());
		final JsonNode state = awaitIdle();
		Assertions.assertTrue(state.get("lastAttempt").get("success").asBoolean());
		Assertions.assertEquals(1, state.get("archives").size());
		final JsonNode archive = state.get("archives").get(0);
		Assertions.assertEquals(state.get("lastAttempt").get("date").asText(), archive.get("date").asText());
		Assertions.assertEquals("FULL", archive.get("type").asText());
		Assertions.assertTrue(archive.get("hasChecksum").asBoolean());
		Assertions.assertTrue(archive.get("size").asLong() > 0);
	}

	@Test
	public void testAStartDuringABackupAnswers409() throws Exception {
		final BlockedEngine engine = new BlockedEngine(this.folder);
		BackupResource.setService(BackupServiceBridge.on(engine));
		try {
			Assertions.assertEquals(202, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode());
			final HttpResponse<String> second = call(POST, ROUTE, Common.ADMIN_TOKEN);
			Assertions.assertEquals(409, second.statusCode());
			Assertions.assertFalse(second.body().contains(this.folder.toString()), second.body());
			final JsonNode running = MAPPER.readTree(call(GET, ROUTE, Common.ADMIN_TOKEN).body());
			Assertions.assertTrue(running.get("running").asBoolean());
		} finally {
			engine.release.countDown();
		}
		Assertions.assertTrue(awaitIdle().get("lastAttempt").get("success").asBoolean());
	}

	/** Distinct from the 409: waiting does not help, the administrator has to free some space. */
	@Test
	public void testAStartWithoutEnoughSpaceAnswers507() throws Exception {
		BackupResource.setService(BackupServiceBridge
				.on(new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED), () -> 0));
		final HttpResponse<String> answer = call(POST, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(507, answer.statusCode(), answer.body());
		// The body the generated client recognizes as an error, without any number nor path.
		final JsonNode error = MAPPER.readTree(answer.body());
		Assertions.assertEquals(507, error.get("status").asInt());
		Assertions.assertEquals("Not enough free space for a backup", error.get("message").asText());
		Assertions.assertFalse(answer.body().contains(this.folder.toString()), answer.body());
		Assertions.assertFalse(awaitIdle().get("lastAttempt").get("success").asBoolean());
		Assertions.assertEquals(401, call(POST, ROUTE, null).statusCode());
		Assertions.assertEquals(403, call(POST, ROUTE, Common.USER_TOKEN).statusCode());
	}

	@Test
	public void testAStartOnAFolderThatDisappearedAnswersAGenericError() throws Exception {
		Files.delete(this.folder);
		final HttpResponse<String> answer = call(POST, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(500, answer.statusCode());
		Assertions.assertTrue(answer.body().contains(ExceptionCatcher.GENERIC_MESSAGE), answer.body());
		Assertions.assertFalse(answer.body().contains(this.folder.toString()), answer.body());
	}

	@Test
	public void testTheAnswerHoldsNoFileNameNorPath() throws Exception {
		Assertions.assertEquals(202, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode());
		awaitIdle();
		final String body = call(GET, ROUTE, Common.ADMIN_TOKEN).body();
		Assertions.assertFalse(body.contains(BASE_NAME), body);
		Assertions.assertFalse(body.contains(".tar"), body);
		Assertions.assertFalse(body.contains(".sha256"), body);
		Assertions.assertFalse(body.contains("_full"), body);
		Assertions.assertFalse(body.contains(this.folder.toString()), body);
		Assertions.assertFalse(body.contains("/"), body);
		final JsonNode state = MAPPER.readTree(body);
		Assertions.assertEquals(Set.of("running", "lastAttempt", "archives"), keysOf(state));
		Assertions.assertEquals(Set.of("date", "success"), keysOf(state.get("lastAttempt")));
		Assertions.assertEquals(Set.of("date", "size", "type", "hasChecksum"), keysOf(state.get("archives").get(0)));
		// An ISO date, not a number of seconds.
		Assertions.assertTrue(state.get("archives").get(0).get("date").asText().endsWith("Z"), body);
	}

	@Test
	public void testAFailedBackupSaysNothingOfItsCause() throws Exception {
		final String cause = this.folder + "/secret: disk full";
		BackupResource.setService(
				BackupServiceBridge.on(new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED) {
					@Override
					public Date store(final String sequence) throws IOException {
						throw new IOException(cause);
					}
				}));
		Assertions.assertEquals(202, call(POST, ROUTE, Common.ADMIN_TOKEN).statusCode());
		final JsonNode state = awaitIdle();
		Assertions.assertFalse(state.get("lastAttempt").get("success").asBoolean());
		Assertions.assertEquals(Set.of("date", "success"), keysOf(state.get("lastAttempt")));
		final String body = call(GET, ROUTE, Common.ADMIN_TOKEN).body();
		Assertions.assertFalse(body.contains(this.folder.toString()), body);
		Assertions.assertFalse(body.contains("disk full"), body);
		Assertions.assertFalse(body.contains("Exception"), body);
	}

	/** A volume that is no longer mounted must not look like a server without any backup. */
	@Test
	public void testAFolderThatDisappearedAnswersAGenericError() throws Exception {
		Files.delete(this.folder);
		final HttpResponse<String> answer = call(GET, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(500, answer.statusCode());
		Assertions.assertTrue(answer.body().contains(ExceptionCatcher.GENERIC_MESSAGE), answer.body());
		Assertions.assertFalse(answer.body().contains(this.folder.toString()), answer.body());
	}

	@Test
	public void testAnUnreadableFolderAnswersAGenericError() throws Exception {
		Files.setPosixFilePermissions(this.folder, PosixFilePermissions.fromString("---------"));
		Assumptions.assumeFalse(Files.isReadable(this.folder), "root reads everything");
		final HttpResponse<String> answer = call(GET, ROUTE, Common.ADMIN_TOKEN);
		Assertions.assertEquals(500, answer.statusCode());
		Assertions.assertTrue(answer.body().contains(ExceptionCatcher.GENERIC_MESSAGE), answer.body());
		Assertions.assertFalse(answer.body().contains(this.folder.toString()), answer.body());
	}

	@Test
	public void testAnApplicationWithoutServiceGetsACleanError() throws Exception {
		BackupResource.setService(null);
		for (final String method : List.of(GET, POST)) {
			final HttpResponse<String> answer = call(method, ROUTE, Common.ADMIN_TOKEN);
			Assertions.assertEquals(503, answer.statusCode(), method);
			Assertions.assertTrue(answer.body().contains("Backups are not configured"), answer.body());
			// The roles come first: nobody learns whether backups are configured.
			Assertions.assertEquals(401, call(method, ROUTE, null).statusCode());
			Assertions.assertEquals(403, call(method, ROUTE, Common.USER_TOKEN).statusCode());
		}
	}

	@Test
	public void testNoRouteServesDeletesOrRestoresAnArchive() throws Exception {
		Assertions.assertEquals(405, call("DELETE", ROUTE, Common.ADMIN_TOKEN).statusCode());
		Assertions.assertEquals(405, call("PUT", ROUTE, Common.ADMIN_TOKEN).statusCode());
		Assertions.assertEquals(404, call(GET, ROUTE + "/any.tar.gz", Common.ADMIN_TOKEN).statusCode());
		Assertions.assertEquals(404, call(POST, ROUTE + "/restore", Common.ADMIN_TOKEN).statusCode());
	}

	@Test
	public void testRoutesAreTwoAdminRoutesWithoutParameter() {
		final Map<String, String> routes = new HashMap<>();
		for (final Method method : BackupResource.class.getDeclaredMethods()) {
			final boolean isRoute = method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class);
			if (!isRoute) {
				Assertions.assertTrue(
						method.getName().equals("setService") || !Modifier.isPublic(method.getModifiers()),
						method.toString());
				continue;
			}
			final String[] roles = method.getAnnotation(RolesAllowed.class).value();
			Assertions.assertEquals(1, roles.length, method.toString());
			routes.put(method.isAnnotationPresent(GET.class) ? GET : POST, roles[0]);
			// The security context, injected by the server: nothing a client gives.
			Assertions.assertEquals(1, method.getParameterCount(), method.toString());
			Assertions.assertEquals(SecurityContext.class, method.getParameterTypes()[0], method.toString());
			Assertions.assertTrue(method.getParameters()[0].isAnnotationPresent(Context.class), method.toString());
			Assertions.assertFalse(method.isAnnotationPresent(jakarta.ws.rs.Path.class), method.toString());
			Assertions.assertFalse(method.isAnnotationPresent(PermitTokenInURI.class), method.toString());
		}
		Assertions.assertEquals(Map.of(GET, "ADMIN:r", POST, "ADMIN:w"), routes);
	}

	@Test
	public void testTheOpenApiHasNoParameter() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(BackupResource.class));
		final String generated = OpenApiGenerateApi.generateJson(api, "backups", "1");
		Assertions.assertFalse(generated.contains("SecurityContext"), generated);
		final JsonNode paths = MAPPER.readTree(generated).get("paths");
		Assertions.assertEquals(1, paths.size(), generated);
		final JsonNode route = paths.elements().next();
		Assertions.assertEquals(Set.of("get", "post"), keysOf(route), generated);
		for (final JsonNode operation : route) {
			Assertions.assertFalse(operation.has("requestBody"), generated);
			Assertions.assertTrue(!operation.has("parameters") || operation.get("parameters").isEmpty(), generated);
		}
	}

	@Test
	public void testTheApiIsGeneratedInTypeScript() throws Exception {
		final Path out = this.folder.resolve("generated");
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(BackupResource.class));
		TsGenerateApi.generateApi(api, out);
		final List<String> sources = new ArrayList<>();
		try (Stream<Path> files = Files.walk(out)) {
			for (final Path file : files.filter(Files::isRegularFile).toList()) {
				if (file.getFileName().toString().equals("rest-tools.ts")) {
					continue;
				}
				sources.add(Files.readString(file));
			}
		}
		final String generated = String.join("\n", sources);
		for (final String expected : List.of("Promise<BackupState>", "Promise<void>", "ZodBackupAttempt",
				"ZodBackupArchive", "ZodBackupArchiveType")) {
			Assertions.assertTrue(generated.contains(expected), expected + " in:\n" + generated);
		}
		// Nothing a client could pass: neither a path parameter nor an argument besides the configuration.
		Assertions.assertEquals(2, generated.split("endPoint: \"/backups/\"", -1).length - 1, generated);
		Assertions.assertFalse(generated.contains("params:"), generated);
		// The security context the server injects is not an argument of the client.
		for (final String function : List.of("getState", "start")) {
			Assertions.assertTrue(
					generated.replaceAll("\\s+", " ")
							.contains("export function " + function + "({ restConfig, }: { restConfig: RESTConfig, })"),
					generated);
		}
		Assertions.assertFalse(generated.contains("queries:"), generated);
	}
}
