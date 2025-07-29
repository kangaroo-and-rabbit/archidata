
package test.atriasoft.archidata.apiExtern;

import java.util.TimeZone;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebLauncherTest extends WebLauncher {
	final private static Logger LOGGER = LoggerFactory.getLogger(WebLauncherTest.class);

	public WebLauncherTest() {
		LOGGER.debug("Configure REST system");
		// for local test:
		ConfigBaseVariable.apiAdress = "http://127.0.0.1:12345/test/api/";
		// Enable the test mode permit to access to the test token (never use it in
		// production).
		ConfigBaseVariable.testMode = "true";
		// ConfigBaseVariable.dbPort = "3306";

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
}
