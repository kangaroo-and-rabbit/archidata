package test.kar.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.SerializeAsJson;
import test.kar.archidata.dataAccess.model.SimpleTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestJson.class);

	private DataAccess da = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	public TestJson() {
		this.da = DataAccess.createInterface();
	}

	@Order(1)
	@Test
	public void testTableFactory() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(SerializeAsJson.class);
		if (this.da instanceof final DataAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testIO() throws Exception {
		final SerializeAsJson test = new SerializeAsJson();
		test.data = new SimpleTable();
		test.data.data = "plopppopql";

		final SerializeAsJson insertedData = this.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertNotNull(insertedData.data.data);
		Assertions.assertEquals(test.data.data, insertedData.data.data);

		// Try to retrieve all the data:
		final SerializeAsJson retrieve = this.da.get(SerializeAsJson.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertTrue(retrieve.id >= 0);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertNotNull(retrieve.data.data);
		Assertions.assertEquals(test.data.data, retrieve.data.data);
	}

}
