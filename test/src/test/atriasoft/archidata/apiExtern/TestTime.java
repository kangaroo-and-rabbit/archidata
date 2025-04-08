package test.atriasoft.archidata.apiExtern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.atriasoft.archidata.exception.RESTErrorResponseException;
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
import test.atriasoft.archidata.apiExtern.model.DataForJSR310;
import test.atriasoft.archidata.apiExtern.model.DataForJSR310String;

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

		final DataForJSR310 inserted = api.request(TestTime.ENDPOINT_NAME).post().bodyJson(data)
				.fetch(DataForJSR310.class);
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
		String received = api.request(TestTime.ENDPOINT_NAME + "/serialize").post().bodyAsJson(data)
				.fetch(String.class);
		Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
		data = """
				{
					"date": "2025-04-04T15:15:07.123Z"
				}
				""";
		received = api.request(TestTime.ENDPOINT_NAME + "/serialize").post().bodyAsJson(data).fetch(String.class);
		Assertions.assertEquals("Fri Apr 04 15:15:07 UTC 2025", received);
		data = """
				{
					"date": "2025-04-04T15:15:07.123+05:00"
				}
				""";
		received = api.request(TestTime.ENDPOINT_NAME + "/serialize").post().bodyAsJson(data).fetch(String.class);
		Assertions.assertEquals("Fri Apr 04 10:15:07 UTC 2025", received);
	}

	@Order(3)
	@Test
	public void unserializeValue() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		DataForJSR310String received = api.request(TestTime.ENDPOINT_NAME + "/unserialize").post().bodyString(data)
				.fetch(DataForJSR310String.class);
		Assertions.assertEquals("2025-04-04T15:15:07.123Z", received.date);

		data = "2025-04-04T15:15:07.123";
		received = api.request(TestTime.ENDPOINT_NAME + "/unserialize").post().bodyString(data)
				.fetch(DataForJSR310String.class);
		Assertions.assertEquals("2025-04-04T15:15:07.123Z", received.date);

		data = "2025-04-04T15:15:07.123+05:00";
		received = api.request(TestTime.ENDPOINT_NAME + "/unserialize").post().bodyString(data)
				.fetch(DataForJSR310String.class);
		Assertions.assertEquals("2025-04-04T10:15:07.123Z", received.date);
	}

	@Order(50)
	@Test
	public void jakartaInputDate() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		String received = api.request(TestTime.ENDPOINT_NAME + "/inputDate").get().queryParam("date", data)
				.fetch(String.class);
		Assertions.assertEquals("2025-04-04T15:15:07.123000000Z", received);

		data = "2025-04-04T15:15:07.123";
		received = api.request(TestTime.ENDPOINT_NAME + "/inputDate").get().queryParam("date", data)
				.fetch(String.class);
		Assertions.assertEquals("2025-04-04T15:15:07.123000000Z", received);

		data = "2025-04-04T15:15:07.123+05:00";
		received = api.request(TestTime.ENDPOINT_NAME + "/inputDate").get().queryParam("date", data)
				.fetch(String.class);
		Assertions.assertEquals("2025-04-04T10:15:07.123000000Z", received);
	}

	@Order(51)
	@Test
	public void jakartaInputOffsetDateTime() throws Exception {
		String data = "2025-04-04T15:15:07.123Z";
		String received = api.request(TestTime.ENDPOINT_NAME + "/inputOffsetDateTime").get().queryParam("date", data)
				.fetch(String.class);
		Assertions.assertEquals("2025-04-04T15:15:07.123000000Z", received);

		// check with offset:
		data = "2025-04-04T15:15:07.123+05:00";
		received = api.request(TestTime.ENDPOINT_NAME + "/inputOffsetDateTime").get().queryParam("date", data)
				.fetch(String.class);
		Assertions.assertEquals("2025-04-04T10:15:07.123000000Z", received);

		// Check parsing fail
		final String dataFail = "2025-04-04T15:15:07.123";
		final RESTErrorResponseException ex = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestTime.ENDPOINT_NAME + "/inputOffsetDateTime").get().queryParam("date", dataFail)
						.fetch(String.class));
		Assertions.assertEquals("Error on query input='date'", ex.name);
		Assertions.assertEquals("Input parsing fail", ex.message);
		Assertions.assertEquals(400, ex.status);
		Assertions.assertNotNull(ex.inputError);
		Assertions.assertEquals(1, ex.inputError.size());
		Assertions.assertEquals("date", ex.inputError.get(0).path);
		Assertions.assertEquals("Invalid date format. Please use ISO8601", ex.inputError.get(0).message);
	}
}
