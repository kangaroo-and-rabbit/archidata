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
import test.atriasoft.archidata.apiExtern.model.SimpleArchiveTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestAPI {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestAPI.class);
	public final static String ENDPOINT_NAME = "TestResource";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;

	private static Long idTest = 0L;

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

		final SimpleArchiveTable data = new SimpleArchiveTable();
		data.name = "Test name";

		final SimpleArchiveTable inserted = api.request(TestAPI.ENDPOINT_NAME).showIOStrean().formatBody().post()
				.bodyJson(data).fetch(SimpleArchiveTable.class);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);
		Assertions.assertNotNull(inserted.name);
		Assertions.assertEquals(data.name, inserted.name);

		TestAPI.idTest = inserted.id;
		final SimpleArchiveTable retrieve = api.request(TestAPI.ENDPOINT_NAME, Long.toString(TestAPI.idTest)).get()
				.fetch(SimpleArchiveTable.class);
		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(TestAPI.idTest, retrieve.id);
		Assertions.assertNotNull(retrieve.name);
		Assertions.assertEquals(data.name, retrieve.name);
	}

	@Order(2)
	@Test
	public void archiveValue() throws Exception {
		final SimpleArchiveTable archivedData = api.request(TestAPI.ENDPOINT_NAME, Long.toString(TestAPI.idTest))
				.archive().fetch(SimpleArchiveTable.class);
		Assertions.assertNotNull(archivedData);
		Assertions.assertEquals(TestAPI.idTest, archivedData.id);
		Assertions.assertNotNull(archivedData.name);
		Assertions.assertNotNull(archivedData.archive);
	}

	@Order(2)
	@Test
	public void restoreValue() throws Exception {
		final SimpleArchiveTable archivedData = api.request(TestAPI.ENDPOINT_NAME, Long.toString(TestAPI.idTest))
				.restore().fetch(SimpleArchiveTable.class);
		Assertions.assertNotNull(archivedData);
		Assertions.assertEquals(TestAPI.idTest, archivedData.id);
		Assertions.assertNotNull(archivedData.name);
		Assertions.assertNull(archivedData.archive);
	}
}
