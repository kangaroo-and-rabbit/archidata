package test.atriasoft.archidata.hybernateValidator;

import java.net.URI;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

import org.atriasoft.archidata.UpdateJwtPublicKey;
import org.atriasoft.archidata.catcher.GenericCatcher;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.filter.CORSFilter;
import org.atriasoft.archidata.filter.OptionFilter;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.validation.ValidationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.UriBuilder;
import test.atriasoft.archidata.hybernateValidator.resource.TestResourceValidator;

public class WebLauncher {
	final static Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	protected UpdateJwtPublicKey keyUpdater = null;
	protected HttpServer server = null;

	public WebLauncher() {}

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

		// add multipart models ..
		rc.register(MultiPartFeature.class);
		// global authentication system
		rc.register(OptionFilter.class);
		// remove cors ==> all time called by an other system...
		rc.register(CORSFilter.class);
		// register exception catcher
		GenericCatcher.addAll(rc);
		// add default resource:
		rc.register(TestResourceValidator.class);
		// enable jersey specific validations (@Valid
		rc.register(ValidationFeature.class);

		ContextGenericTools.addJsr310(rc);

		// add jackson to be discover when we are ins standalone server
		rc.register(JacksonFeature.class);

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
			e.printStackTrace();
		}
	}
}
