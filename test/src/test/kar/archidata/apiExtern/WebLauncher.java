package test.kar.archidata.apiExtern;

import java.net.URI;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.UpdateJwtPublicKey;
import org.kar.archidata.api.DataResource;
import org.kar.archidata.api.ProxyResource;
import org.kar.archidata.catcher.GenericCatcher;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.filter.CORSFilter;
import org.kar.archidata.filter.OptionFilter;
import org.kar.archidata.migration.MigrationEngine;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
import test.kar.archidata.apiExtern.resource.TestResource;

public class WebLauncher {
	final static Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	protected UpdateJwtPublicKey keyUpdater = null;
	protected HttpServer server = null;

	private final DataAccess da;

	public WebLauncher() {
		this.da = DataAccess.createInterface();
	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri(ConfigBaseVariable.getlocalAddress()).build();
	}

	public void migrateDB() throws Exception {
		WebLauncher.LOGGER.info("Create migration engine");
		final MigrationEngine migrationEngine = new MigrationEngine(this.da);
		WebLauncher.LOGGER.info("Add initialization");
		//migrationEngine.setInit(new Initialization());
		WebLauncher.LOGGER.info("Add migration since last version");
		//migrationEngine.add(new Migration20231126());
		WebLauncher.LOGGER.info("Migrate the DB [START]");
		migrationEngine.migrateWaitAdmin(GlobalConfiguration.getDbconfig());
		WebLauncher.LOGGER.info("Migrate the DB [STOP]");
	}

	public static void main(final String[] args) throws Exception {
		WebLauncher.LOGGER.info("[START] application wake UP");
		final WebLauncher launcher = new WebLauncher();
		launcher.migrateDB();

		launcher.process();
		WebLauncher.LOGGER.info("end-configure the server & wait finish process:");
		Thread.currentThread().join();
		WebLauncher.LOGGER.info("STOP Key updater");
		launcher.stopOther();
		WebLauncher.LOGGER.info("STOP the REST server:");
	}

	public void plop(final String aaa) {
		// List available Image Readers
		System.out.println("Available Image Readers:");
		final Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(aaa);
		while (readers.hasNext()) {
			final ImageReader reader = readers.next();
			System.out.println("Reader: " + reader.getOriginatingProvider().getDescription(null));
			System.out.println("Reader CN: " + reader.getOriginatingProvider().getPluginClassName());
			// ImageIO.deregisterServiceProvider(reader.getOriginatingProvider());
		}

		// List available Image Writers
		System.out.println("\nAvailable Image Writers:");
		final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(aaa);
		while (writers.hasNext()) {
			final ImageWriter writer = writers.next();
			System.out.println("Writer: " + writer.getOriginatingProvider().getDescription(null));
			System.out.println("Writer CN: " + writer.getOriginatingProvider().getPluginClassName());
		}
	}

	public void process() throws InterruptedException {

		ImageIO.scanForPlugins();
		plop("jpeg");
		plop("png");
		plop("webmp");
		plop("webp");
		// ===================================================================
		// Configure resources
		// ===================================================================
		final ResourceConfig rc = new ResourceConfig();

		// add multipart models ..
		rc.register(MultiPartFeature.class);
		// global authentication system
		rc.register(OptionFilter.class);
		// remove cors ==> all time called by an other system...
		rc.register(CORSFilter.class);
		// global authentication system
		rc.register(TestAuthenticationFilter.class);
		// register exception catcher
		GenericCatcher.addAll(rc);
		// add default resource:
		rc.register(TestResource.class);
		rc.register(DataResource.class);
		rc.register(ProxyResource.class);

		// add jackson to be discover when we are ins standalone server
		rc.register(JacksonFeature.class);
		// enable this to show low level request
		// rc.property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, Level.WARNING.getName());

		// System.out.println("Connect on the BDD:");
		// System.out.println(" getDBHost: '" + ConfigVariable.getDBHost() + "'");
		// System.out.println(" getDBPort: '" + ConfigVariable.getDBPort() + "'");
		// System.out.println(" getDBLogin: '" + ConfigVariable.getDBLogin() + "'");
		// System.out.println(" getDBPassword: '" + ConfigVariable.getDBPassword() + "'");
		// System.out.println(" getDBName: '" + ConfigVariable.getDBName() + "'");
		System.out.println(" ==> " + GlobalConfiguration.getDbconfig());
		System.out.println("OAuth service " + getBaseURI());
		this.server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc);
		final HttpServer serverLink = this.server;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Stopping server..");
				serverLink.shutdownNow();
			}
		}, "shutdownHook"));

		// ===================================================================
		// start periodic update of the token ...
		// ===================================================================
		this.keyUpdater = new UpdateJwtPublicKey();
		this.keyUpdater.start();

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

	public void stop() {
		if (this.server != null) {
			this.server.shutdownNow();
			this.server = null;
		}
	}

	public void stopOther() {
		this.keyUpdater.kill();
		try {
			this.keyUpdater.join(4000, 0);
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
