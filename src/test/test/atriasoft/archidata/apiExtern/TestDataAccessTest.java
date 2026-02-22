package test.atriasoft.archidata.apiExtern;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.RESTApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestDataAccessTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestAPI.class);
	public static final String ENDPOINT_NAME = "DataAccessTestResource";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		LOGGER.info("configure server ...");
		webInterface = new WebLauncherTest();
		LOGGER.info("Clean previous table");

		LOGGER.info("Start REST (BEGIN)");
		webInterface.process();
		LOGGER.info("Start REST (DONE)");
		api = new RESTApi(ConfigBaseVariable.apiAdress);
		api.setToken(Common.ADMIN_TOKEN);
		api.showIOStrean();
	}

	@AfterAll
	public static void stopWebServer() throws Exception {
		LOGGER.info("Kill the web server");
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void connected() throws Exception {
		final String result = api.request(ENDPOINT_NAME, "getAlreadyConnected").formatBody().get().fetch(String.class);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("OK", result);
	}

	@Order(2)
	@Test
	public void notConnected() throws Exception {
		final String result = api.request(ENDPOINT_NAME, "getNoConnection").formatBody().get().fetch(String.class);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("OK", result);
	}

}
