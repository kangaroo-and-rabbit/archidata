package test.atriasoft.archidata.dataAccess;

import java.io.IOException;

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
import test.atriasoft.archidata.dataAccess.model.SerializeAsJson;
import test.atriasoft.archidata.dataAccess.model.SimpleTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestJson.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testTableFactory() throws Exception {}

	@Order(2)
	@Test
	public void testIO() throws Exception {
		final SerializeAsJson test = new SerializeAsJson();
		test.data = new SimpleTable();
		test.data.data = "plopppopql";

		final SerializeAsJson insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertNotNull(insertedData.data.data);
		Assertions.assertEquals(test.data.data, insertedData.data.data);

		// Try to retrieve all the data:
		final SerializeAsJson retrieve = ConfigureDb.da.get(SerializeAsJson.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertTrue(retrieve.id >= 0);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertNotNull(retrieve.data.data);
		Assertions.assertEquals(test.data.data, retrieve.data.data);
	}

}
