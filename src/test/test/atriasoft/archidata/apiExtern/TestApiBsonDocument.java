package test.atriasoft.archidata.apiExtern;

import java.util.List;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.RESTApi;
import org.bson.Document;
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
import test.atriasoft.archidata.dataAccess.model.DataWithBsonDocument;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestApiBsonDocument {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestApiBsonDocument.class);
	private static final String ENDPOINT_NAME = "DocumentResource";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		LOGGER.info("configure server ...");
		webInterface = new WebLauncherTest();
		LOGGER.info("Start REST (BEGIN)");
		webInterface.process();
		LOGGER.info("Start REST (DONE)");
		api = new RESTApi(ConfigBaseVariable.getlocalAddress());
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
	public void testRoundTripSimpleDocument() throws Exception {
		final DataWithBsonDocument data = new DataWithBsonDocument();
		data.documentData = new Document().append("name", "test-value").append("count", 42).append("active", true);

		final DataWithBsonDocument inserted = api.request(ENDPOINT_NAME).post().bodyJson(data)
				.fetch(DataWithBsonDocument.class);

		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.getOid());
		Assertions.assertNotNull(inserted.documentData);
		Assertions.assertEquals("test-value", inserted.documentData.getString("name"));
		Assertions.assertEquals(42, inserted.documentData.getInteger("count"));
		Assertions.assertEquals(true, inserted.documentData.getBoolean("active"));

		// GET it back
		final DataWithBsonDocument retrieve = api.request(ENDPOINT_NAME, inserted.getOid().toHexString()).get()
				.fetch(DataWithBsonDocument.class);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.documentData);
		Assertions.assertEquals("test-value", retrieve.documentData.getString("name"));
		Assertions.assertEquals(42, retrieve.documentData.getInteger("count"));
		Assertions.assertEquals(true, retrieve.documentData.getBoolean("active"));
	}

	@Order(2)
	@Test
	public void testRoundTripNestedDocument() throws Exception {
		final DataWithBsonDocument data = new DataWithBsonDocument();
		data.documentData = new Document().append("level1", "value1").append("nested",
				new Document().append("level2", "value2").append("number", 99));

		final DataWithBsonDocument inserted = api.request(ENDPOINT_NAME).post().bodyJson(data)
				.fetch(DataWithBsonDocument.class);

		Assertions.assertNotNull(inserted);

		final DataWithBsonDocument retrieve = api.request(ENDPOINT_NAME, inserted.getOid().toHexString()).get()
				.fetch(DataWithBsonDocument.class);

		Assertions.assertNotNull(retrieve.documentData);
		Assertions.assertEquals("value1", retrieve.documentData.getString("level1"));

		final Document nested = (Document) retrieve.documentData.get("nested");
		Assertions.assertNotNull(nested);
		Assertions.assertEquals("value2", nested.getString("level2"));
		Assertions.assertEquals(99, nested.getInteger("number"));
	}

	@Order(3)
	@Test
	public void testRoundTripDocumentWithList() throws Exception {
		final DataWithBsonDocument data = new DataWithBsonDocument();
		data.documentData = new Document().append("tags", List.of("alpha", "beta", "gamma"));

		final DataWithBsonDocument inserted = api.request(ENDPOINT_NAME).post().bodyJson(data)
				.fetch(DataWithBsonDocument.class);

		final DataWithBsonDocument retrieve = api.request(ENDPOINT_NAME, inserted.getOid().toHexString()).get()
				.fetch(DataWithBsonDocument.class);

		Assertions.assertNotNull(retrieve.documentData);
		@SuppressWarnings("unchecked")
		final List<String> tags = (List<String>) retrieve.documentData.get("tags");
		Assertions.assertEquals(3, tags.size());
		Assertions.assertEquals("alpha", tags.get(0));
		Assertions.assertEquals("beta", tags.get(1));
		Assertions.assertEquals("gamma", tags.get(2));
	}
}
