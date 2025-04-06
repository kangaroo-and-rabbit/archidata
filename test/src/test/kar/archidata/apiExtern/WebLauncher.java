package test.kar.archidata.apiExtern;

import java.net.URI;
import java.util.Iterator;
import java.util.TimeZone;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.kar.archidata.UpdateJwtPublicKey;
import org.kar.archidata.api.DataResource;
import org.kar.archidata.api.ProxyResource;
import org.kar.archidata.catcher.GenericCatcher;
import org.kar.archidata.converter.Jakarta.DateParamConverter;
import org.kar.archidata.converter.Jakarta.OffsetDateTimeParamConverter;
import org.kar.archidata.db.DbConfig;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.filter.CORSFilter;
import org.kar.archidata.filter.OptionFilter;
import org.kar.archidata.migration.MigrationEngine;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.ContextGenericTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
import test.kar.archidata.apiExtern.resource.TestResource;
import test.kar.archidata.apiExtern.resource.TimeResource;

public class WebLauncher {
	final static Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	protected UpdateJwtPublicKey keyUpdater = null;
	protected HttpServer server = null;

	public WebLauncher() {
		// Set default timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	private static URI getBaseURI() {
		return UriBuilder.fromUri(ConfigBaseVariable.getlocalAddress()).build();
	}

	public void migrateDB() throws Exception {
		WebLauncher.LOGGER.info("Create migration engine");
		final MigrationEngine migrationEngine = new MigrationEngine();
		WebLauncher.LOGGER.info("Add initialization");
		//migrationEngine.setInit(new Initialization());
		WebLauncher.LOGGER.info("Add migration since last version");
		//migrationEngine.add(new Migration20231126());
		WebLauncher.LOGGER.info("Migrate the DB [START]");
		migrationEngine.migrateWaitAdmin(new DbConfig());
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
		WebLauncher.LOGGER.trace("Available Image Readers:");
		final Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(aaa);
		while (readers.hasNext()) {
			final ImageReader reader = readers.next();
			WebLauncher.LOGGER.trace("Reader: " + reader.getOriginatingProvider().getDescription(null));
			WebLauncher.LOGGER.trace("Reader CN: " + reader.getOriginatingProvider().getPluginClassName());
			// ImageIO.deregisterServiceProvider(reader.getOriginatingProvider());
		}

		// List available Image Writers
		WebLauncher.LOGGER.trace("\nAvailable Image Writers:");
		final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(aaa);
		while (writers.hasNext()) {
			final ImageWriter writer = writers.next();
			WebLauncher.LOGGER.trace("Writer: " + writer.getOriginatingProvider().getDescription(null));
			WebLauncher.LOGGER.trace("Writer CN: " + writer.getOriginatingProvider().getPluginClassName());
		}
	}

	public void process() throws InterruptedException, DataAccessException {

		ImageIO.scanForPlugins();
		plop("jpeg");
		plop("png");
		plop("webmp");
		plop("webp");
		// ===================================================================
		// Configure resources
		// ===================================================================
		final ResourceConfig rc = new ResourceConfig();

		// Add permissive date converter for jakarta
		rc.register(DateParamConverter.class);
		rc.register(OffsetDateTimeParamConverter.class);

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
		rc.register(TimeResource.class);
		rc.register(DataResource.class);
		rc.register(ProxyResource.class);

		ContextGenericTools.addJsr310(rc);

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
		LOGGER.info(" ==> {}", new DbConfig());
		LOGGER.info("OAuth service {}", getBaseURI());
		this.server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc);
		final HttpServer serverLink = this.server;
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				LOGGER.warn("Stopping server..");
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
