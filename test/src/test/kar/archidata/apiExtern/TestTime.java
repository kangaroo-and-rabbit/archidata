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
import test.kar.archidata.apiExtern.model.DataForJSR310String;

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
		data.localTime = LocalTime.now();
		data.localDate = LocalDate.now();
		data.localDateTime = LocalDateTime.now();

		final DataForJSR310 inserted = api.post(DataForJSR310.class, TestTime.ENDPOINT_NAME, data);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.localTime);
		Assertions.assertNotNull(inserted.localDate);
		Assertions.assertNotNull(inserted.localDateTime);
		Assertions.assertEquals(inserted.localTime, data.localTime);
		Assertions.assertEquals(inserted.localDate, data.localDate);
		Assertions.assertEquals(inserted.localDateTime, data.localDateTime);
	}

	@Order(2)
	@Test
	public void serializeValue() throws Exception {
		String data = """
				{
					"date": "2025-04-04T15:15:07.123"
				}
				""";
		String received = api.postJson(String.class, TestTime.ENDPOINT_NAME + "/serialize", data);
		LOGGER.info("received: '{}'", received);
		Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
		data = """
				{
					"date": "2025-04-04T15:15:07.123Z"
				}
				""";
		received = api.postJson(String.class, TestTime.ENDPOINT_NAME + "/serialize", data);
		LOGGER.info("received: '{}'", received);
		Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
		data = """
				{
					"date": "2025-04-04T15:15:07.123+05:00"
				}
				""";
		received = api.postJson(String.class, TestTime.ENDPOINT_NAME + "/serialize", data);
		LOGGER.info("received: '{}'", received);
		Assertions.assertEquals("Fri Apr 04 10:15:07 UTC 2025", received);

		Assertions.assertNotNull(received);
	}

	@Order(3)
	@Test
	public void unserializeValue() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		DataForJSR310String received = api.postJson(DataForJSR310String.class, TestTime.ENDPOINT_NAME + "/unserialize",
				data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received.date);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123";
		received = api.postJson(DataForJSR310String.class, TestTime.ENDPOINT_NAME + "/unserialize", data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received.date);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123+05:00";
		received = api.postJson(DataForJSR310String.class, TestTime.ENDPOINT_NAME + "/unserialize", data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received.date);
		LOGGER.info("----------------------------------------------------");
		//Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
	}

	@Order(50)
	@Test
	public void jakartaInputDate() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		String received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputDate");
		//String received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputDate?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123";
		received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputDate?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123+05:00";
		received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputDate?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		//Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
	}

	@Order(51)
	@Test
	public void jakartaInputOffsetDateTime() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		String received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputOffsetDateTime?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123";
		received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputOffsetDateTime?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		data = "2025-04-04T15:15:07.123+05:00";
		received = api.get(String.class, TestTime.ENDPOINT_NAME + "/inputOffsetDateTime?date=" + data);
		LOGGER.info("send    : '{}'", data);
		LOGGER.info("received: '{}'", received);
		LOGGER.info("----------------------------------------------------");
		//Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
	}
}
