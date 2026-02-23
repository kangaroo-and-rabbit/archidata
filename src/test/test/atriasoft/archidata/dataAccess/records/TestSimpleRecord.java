package test.atriasoft.archidata.dataAccess.records;

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
import org.bson.types.ObjectId;

import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestSimpleRecord {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSimpleRecord.class);

	public record SimpleRec(
			String name,
			int value) {}

	public static class Model {
		@Id
		public ObjectId _id = null;

		public SimpleRec data;
	}

	private static ObjectId idOfTheObject = null;

	@BeforeAll
	static void setup() throws Exception {
		idOfTheObject = null;
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testInsertAndRetrieve() throws Exception {
		final Model test = new Model();
		test.data = new SimpleRec("hello", 42);

		final Model insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData._id);
		Assertions.assertNotNull(insertedData._id);

		final Model retrieve = ConfigureDb.da.getById(Model.class, insertedData._id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve._id);
		Assertions.assertEquals(insertedData._id, retrieve._id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals("hello", retrieve.data.name());
		Assertions.assertEquals(42, retrieve.data.value());

		idOfTheObject = retrieve._id;
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model updateData = new Model();
		updateData.data = new SimpleRec("updated", 99);

		ConfigureDb.da.updateById(updateData, idOfTheObject);

		final Model retrieve = ConfigureDb.da.getById(Model.class, idOfTheObject);

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(idOfTheObject, retrieve._id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals("updated", retrieve.data.name());
		Assertions.assertEquals(99, retrieve.data.value());
	}
}
