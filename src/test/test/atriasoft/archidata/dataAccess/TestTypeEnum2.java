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
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;
import test.atriasoft.archidata.dataAccess.model.TypesEnum2;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTypeEnum2 {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypeEnum2.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(2)
	@Test
	public void testEnum() throws Exception {

		final TypesEnum2 test = new TypesEnum2();
		test.data = Enum2ForTest.ENUM_VALUE_4;
		final TypesEnum2 insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		TypesEnum2 retrieve = ConfigureDb.da.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(insertedData.data, retrieve.data);

		// Update data to null
		retrieve.data = null;
		long ret = ConfigureDb.da.update(retrieve, retrieve.id);
		Assertions.assertEquals(1L, ret);

		// get new data
		retrieve = ConfigureDb.da.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNull(retrieve.data);

		// Remove the data
		ret = ConfigureDb.da.delete(TypesEnum2.class, insertedData.id);
		Assertions.assertEquals(1L, ret);

		// Get the removed data:
		retrieve = ConfigureDb.da.get(TypesEnum2.class, insertedData.id);
		Assertions.assertNull(retrieve);
	}

	@Order(3)
	@Test
	public void testNull() throws Exception {

		final TypesEnum2 test = new TypesEnum2();
		test.data = null;
		final TypesEnum2 insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final TypesEnum2 retrieve = ConfigureDb.da.get(TypesEnum2.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNull(retrieve.data);

		ConfigureDb.da.delete(TypesEnum2.class, insertedData.id);
	}
}
