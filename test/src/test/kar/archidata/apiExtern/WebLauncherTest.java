
package test.kar.archidata.apiExtern;

import java.io.IOException;

import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;

public class WebLauncherTest extends WebLauncher {
	final private static Logger LOGGER = LoggerFactory.getLogger(WebLauncherTest.class);

	public WebLauncherTest() {
		LOGGER.debug("Configure REST system");
		// for local test:
		ConfigBaseVariable.apiAdress = "http://127.0.0.1:12345/test/api/";
		// Enable the test mode permit to access to the test token (never use it in production).
		ConfigBaseVariable.testMode = "true";
		// ConfigBaseVariable.dbPort = "3306";
		try {
			ConfigureDb.configure();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}