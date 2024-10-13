package test.kar.archidata.apiExtern;

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
import test.kar.archidata.apiExtern.model.SimpleArchiveTable;

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

		final SimpleArchiveTable inserted = api.post(SimpleArchiveTable.class, TestAPI.ENDPOINT_NAME, data);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);
		Assertions.assertNotNull(inserted.name);
		Assertions.assertEquals(data.name, inserted.name);

		TestAPI.idTest = inserted.id;
		final SimpleArchiveTable retrieve = api.get(SimpleArchiveTable.class,
				TestAPI.ENDPOINT_NAME + "/" + TestAPI.idTest);
		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(TestAPI.idTest, retrieve.id);
		Assertions.assertNotNull(retrieve.name);
		Assertions.assertEquals(data.name, retrieve.name);
	}

	@Order(2)
	@Test
	public void archiveValue() throws Exception {
		final SimpleArchiveTable archivedData = api.archive(SimpleArchiveTable.class,
				TestAPI.ENDPOINT_NAME + "/" + TestAPI.idTest);
		Assertions.assertNotNull(archivedData);
		Assertions.assertEquals(TestAPI.idTest, archivedData.id);
		Assertions.assertNotNull(archivedData.name);
		Assertions.assertNotNull(archivedData.archive);
	}

	@Order(2)
	@Test
	public void restoreValue() throws Exception {
		final SimpleArchiveTable archivedData = api.restore(SimpleArchiveTable.class,
				TestAPI.ENDPOINT_NAME + "/" + TestAPI.idTest);
		Assertions.assertNotNull(archivedData);
		Assertions.assertEquals(TestAPI.idTest, archivedData.id);
		Assertions.assertNotNull(archivedData.name);
		Assertions.assertNull(archivedData.archive);
	}
}
