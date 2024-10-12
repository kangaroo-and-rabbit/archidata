package test.kar.archidata;

import java.io.IOException;
import java.util.ArrayList;
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

import test.kar.archidata.model.SerializeListAsJson;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestListJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestListJson.class);

	private DataAccess da = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	public TestListJson() {
		this.da = DataAccess.createInterface();
	}

	@Order(1)
	@Test
	public void testTableInsertAndRetrieve() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(SerializeListAsJson.class);
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
		final SerializeListAsJson test = new SerializeListAsJson();
		test.data = new ArrayList<>();
		test.data.add(5);
		test.data.add(2);
		test.data.add(8);
		test.data.add(6);
		test.data.add(51);

		final SerializeListAsJson insertedData = this.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertEquals(5, insertedData.data.size());
		Assertions.assertEquals(test.data.get(0), insertedData.data.get(0));
		Assertions.assertEquals(test.data.get(1), insertedData.data.get(1));
		Assertions.assertEquals(test.data.get(2), insertedData.data.get(2));
		Assertions.assertEquals(test.data.get(3), insertedData.data.get(3));
		Assertions.assertEquals(test.data.get(4), insertedData.data.get(4));

		// Try to retrieve all the data:
		final SerializeListAsJson retrieve = this.da.get(SerializeListAsJson.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertTrue(retrieve.id >= 0);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(5, retrieve.data.size());
		Assertions.assertEquals(test.data.get(0), retrieve.data.get(0));
		Assertions.assertEquals(test.data.get(1), retrieve.data.get(1));
		Assertions.assertEquals(test.data.get(2), retrieve.data.get(2));
		Assertions.assertEquals(test.data.get(3), retrieve.data.get(3));
		Assertions.assertEquals(test.data.get(4), retrieve.data.get(4));
	}

}
