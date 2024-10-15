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
import test.kar.archidata.dataAccess.model.Enum1ForTest;
import test.kar.archidata.dataAccess.model.TypesEnum1;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTypeEnum1 {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypeEnum1.class);

	private DataAccess da = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	public TestTypeEnum1() {
		this.da = DataAccess.createInterface();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypesEnum1.class);
		if (this.da instanceof final DataAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testEnum() throws Exception {

		final TypesEnum1 test = new TypesEnum1();
		test.data = Enum1ForTest.ENUM_VALUE_3;
		final TypesEnum1 insertedData = this.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesEnum1 retrieve = this.da.get(TypesEnum1.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(insertedData.data, retrieve.data);

		this.da.delete(TypesEnum1.class, insertedData.id);
	}
}