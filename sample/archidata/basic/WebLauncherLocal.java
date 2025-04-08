
package sample.archidata.basic;

import java.util.List;

import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.dataAccess.DataFactoryTsApi;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLauncherLocal extends WebLauncher {
	private final static Logger LOGGER = LoggerFactory.getLogger(WebLauncherLocal.class);

	private WebLauncherLocal() {}

	public static void main(final String[] args) throws Exception {
		final WebLauncherLocal launcher = new WebLauncherLocal();
		launcher.process();
		LOGGER.info("end-configure the server & wait finish process:");
		Thread.currentThread().join();
		LOGGER.info("STOP the REST server:");
	}

	@Override
	public void process() throws InterruptedException {
		if (true) {
			// for local test:
			ConfigBaseVariable.apiAddress = "http://0.0.0.0:9000/sample/api/";
			ConfigBaseVariable.dbPort = "3906";
		}
		try {
			super.migrateDB();
		} catch (final Exception e) {
			e.printStackTrace();
			while (true) {
				LOGGER.error("============================================================================");
				LOGGER.error("== Migration fail ==> waiting intervention of administrator...");
				LOGGER.error("============================================================================");
				Thread.sleep(60 * 60 * 1000);
			}
		}
		super.process();
	}
}
