package test.kar.archidata.apiExtern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.RESTApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.apiExtern.model.DataForJSR310;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTime {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestTime.class);
	public final static String ENDPOINT_NAME = "TimeResource";

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
	public void insertValue() throws Exception {

		final DataForJSR310 data = new DataForJSR310();
		data.time = LocalTime.now();
		data.date = LocalDate.now();
		data.dateTime = LocalDateTime.now();

		final DataForJSR310 inserted = api.post(DataForJSR310.class, TestTime.ENDPOINT_NAME, data);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.time);
		Assertions.assertNotNull(inserted.date);
		Assertions.assertNotNull(inserted.dateTime);
		Assertions.assertEquals(inserted.time, data.time);
		Assertions.assertEquals(inserted.date, data.date);
		Assertions.assertEquals(inserted.dateTime, data.dateTime);
	}
}
