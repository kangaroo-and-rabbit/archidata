package sample.archidata.basic;

import java.net.URI;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.atriasoft.archidata.GlobalConfiguration;
import org.atriasoft.archidata.UpdateJwtPublicKey;
import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.FailExceptionCatcher;
import org.atriasoft.archidata.catcher.InputExceptionCatcher;
import org.atriasoft.archidata.catcher.SystemExceptionCatcher;
import org.atriasoft.archidata.db.DBConfig;
import org.atriasoft.archidata.filter.CORSFilter;
import org.atriasoft.archidata.filter.OptionFilter;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import sample.archidata.basic.api.Front;
import sample.archidata.basic.api.HealthCheck;
import sample.archidata.basic.api.MediaResource;
import sample.archidata.basic.api.SeasonResource;
import sample.archidata.basic.api.SeriesResource;
import sample.archidata.basic.api.TypeResource;
import sample.archidata.basic.api.UserMediaAdvancementResource;
import sample.archidata.basic.api.UserResource;
import sample.archidata.basic.filter.KarideoAuthenticationFilter;
import sample.archidata.basic.migration.Initialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;

public class WebLauncher {
	final static Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	public static DBConfig dbConfig;
	protected UpdateJwtPublicKey keyUpdater = null;
	protected HttpServer server = null;

	public WebLauncher() {
		ConfigBaseVariable.bdDatabase = "sample_archidata_basic";
	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri(ConfigBaseVariable.getlocalAddress()).build();
	}

	public void migrateDB() throws Exception {
		WebLauncher.LOGGER.info("Create migration engine");
		final MigrationEngine migrationEngine = new MigrationEngine();
		WebLauncher.LOGGER.info("Add initialization");
		migrationEngine.setInit(new Initialization());
		//WebLauncher.LOGGER.info("Add migration since last version");
		//migrationEngine.add(new Migration20230810());
		WebLauncher.LOGGER.info("Migrate the DB [START]");
		migrationEngine.migrateWaitAdmin(GlobalConfiguration.dbConfig);
		WebLauncher.LOGGER.info("Migrate the DB [STOP]");
	}

	public static void main(final String[] args) throws Exception {
		WebLauncher.LOGGER.info("[START] application wake UP");
		final WebLauncher launcher = new WebLauncher();
		launcher.migrateDB();
		launcher.process();
		WebLauncher.LOGGER.info("end-configure the server & wait finish process:");
		Thread.currentThread().join();
		WebLauncher.LOGGER.info("STOP the REST server");
	}

	public void process() throws InterruptedException {

		// ===================================================================
		// Configure resources
		// ===================================================================
		final ResourceConfig rc = new ResourceConfig();

		// Permit to accept OPTION request 
		rc.register(OptionFilter.class);
		// remove cors ==> not obviously needed ...
		rc.register(CORSFilter.class);
		// register exception catcher (this permit to format return error with a normalized JSON)
		rc.register(InputExceptionCatcher.class);
		rc.register(SystemExceptionCatcher.class);
		rc.register(FailExceptionCatcher.class);
		rc.register(ExceptionCatcher.class);

		// add default resource:
		rc.register(MyModelResource.class);


		// add jackson to be discover when we are in stand-alone server
		rc.register(JacksonFeature.class);

		this.server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc);
		final HttpServer serverLink = this.server;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				WebLauncher.LOGGER.info("Stopping server..");
				serverLink.shutdownNow();
			}
		}, "shutdownHook"));


		// ===================================================================
		// run JERSEY
		// ===================================================================
		try {
			this.server.start();
			LOGGER.info("Jersey app started at {}", getBaseURI());
		} catch (final Exception e) {
			LOGGER.error("There was an error while starting Grizzly HTTP server.");
			e.printStackTrace();
		}
	}
	// This is used for TEST (See it later)
	public void stop() {
		if (this.server != null) {
			this.server.shutdownNow();
			this.server = null;
		}
	}
}
