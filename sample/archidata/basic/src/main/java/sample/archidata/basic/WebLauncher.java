package sample.archidata.basic;

import java.net.URI;
import java.util.TimeZone;
import java.util.logging.LogManager;

import org.atriasoft.archidata.UpdateJwtPublicKey;
import org.atriasoft.archidata.catcher.GenericCatcher;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.filter.CORSFilter;
import org.atriasoft.archidata.filter.OptionFilter;
import org.atriasoft.archidata.index.IndexEngine;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import jakarta.ws.rs.core.UriBuilder;
import sample.archidata.basic.api.TaskResource;
import sample.archidata.basic.filter.SampleAuthenticationFilter;
import sample.archidata.basic.migration.Initialization;
import sample.archidata.basic.model.Task;

/**
 * Production entry point.
 *
 * <p>All the configuration comes from the environment, read by {@link ConfigBaseVariable}:
 * {@code DB_HOST}, {@code DB_PORT}, {@code DB_USER}, {@code DB_PASSWORD} (mandatory),
 * {@code DB_DATABASE}, {@code API_ADDRESS} (address the server listens on),
 * {@code SSO_ADDRESS} (where the JWT public key is fetched)...
 *
 * <p>Start-up order: migrations, then indexes, then the HTTP server.
 */
public class WebLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	/** Every entity the application stores: the index engine works on this list. */
	public static final Class<?>[] ENTITIES = { Task.class };

	protected HttpServer server = null;
	protected UpdateJwtPublicKey keyUpdater = null;

	public WebLauncher() {
		// Dates are stored and exchanged in UTC.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		// Default database name when DB_DATABASE is not set.
		if (ConfigBaseVariable.getDBName() == null) {
			ConfigBaseVariable.setBdDatabase("sample_basic");
		}
	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri(ConfigBaseVariable.getLocalAddress()).build();
	}

	/** Bring the database to the current version of the model. */
	public void migrateDB() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// Run only on an empty database.
		migrationEngine.setInit(new Initialization());
		// Later migrations are appended here, in order, and never removed:
		// migrationEngine.add(new Migration20261101());
		// new DbConfig() reads the DB_* configuration from ConfigBaseVariable.
		// On failure, migrateWaitAdmin() logs and blocks forever (waiting for an administrator)
		// instead of starting on a half-migrated database; migrateErrorThrow() throws instead.
		migrationEngine.migrateWaitAdmin(new DbConfig());
	}

	/** Create, update or drop the MongoDB indexes so that they match the @Index / @Indexed declarations. */
	public void synchronizeIndexes() throws Exception {
		final IndexEngine indexes = new IndexEngine();
		indexes.addClass(ENTITIES);
		indexes.synchronize();
	}

	/** Configure Jersey and start the Grizzly HTTP server (non-blocking). */
	public void process() throws Exception {
		final ResourceConfig rc = new ResourceConfig();
		// Answer the CORS pre-flight OPTIONS requests and add the CORS headers.
		rc.register(OptionFilter.class);
		rc.register(CORSFilter.class);
		// JWT check + @PermitAll / @RolesAllowed enforcement. Without it, the role annotations are ignored.
		rc.register(SampleAuthenticationFilter.class);
		// Exception -> JSON error body with the right HTTP status (400 on validation errors, 404...).
		GenericCatcher.addAll(rc);
		// Application resources.
		rc.register(TaskResource.class);
		// Jackson configured by archidata (java.time, ObjectId...) AND the interceptor that applies
		// @ValidGroup: without this call @ValidGroup is silently ignored.
		ContextGenericTools.addJsr310(rc);
		rc.register(JacksonFeature.class);
		// Bean Validation of the @Valid parameters.
		rc.register(ValidationFeature.class);

		this.server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc);
		final HttpServer serverLink = this.server;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Stopping server...");
			serverLink.shutdownNow();
		}, "shutdownHook"));

		// Fetch (and refresh) the JWT public key from SSO_ADDRESS. Without SSO_ADDRESS the thread
		// stops at once and the key must be provided otherwise (see WebLauncherLocal).
		this.keyUpdater = new UpdateJwtPublicKey();
		this.keyUpdater.start();

		this.server.start();
		LOGGER.info("Server started at {}", getBaseURI());
	}

	/** Stop the HTTP server (used by tests and the local launcher). */
	public void stop() {
		if (this.keyUpdater != null) {
			this.keyUpdater.kill();
			this.keyUpdater = null;
		}
		if (this.server != null) {
			this.server.shutdownNow();
			this.server = null;
		}
	}

	/** Route java.util.logging (used by Jersey and Grizzly) to SLF4J. */
	protected static void installLogBridge() {
		LogManager.getLogManager().reset();
		SLF4JBridgeHandler.install();
	}

	public static void main(final String[] args) throws Exception {
		installLogBridge();
		final WebLauncher launcher = new WebLauncher();
		launcher.migrateDB();
		launcher.synchronizeIndexes();
		launcher.process();
		Thread.currentThread().join();
	}
}
