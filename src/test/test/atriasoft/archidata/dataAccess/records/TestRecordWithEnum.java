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
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestRecordWithEnum {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestRecordWithEnum.class);

	public record RecEnum(
			Enum2ForTest status,
			String label) {}

	public static class Model {
		@Id
		public ObjectId _id = null;

		public RecEnum data;
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
		test.data = new RecEnum(Enum2ForTest.ENUM_VALUE_3, "test-label");

		final Model insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData._id);
		Assertions.assertNotNull(insertedData._id);

		final Model retrieve = ConfigureDb.da.getById(Model.class, insertedData._id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve._id);
		Assertions.assertEquals(insertedData._id, retrieve._id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(Enum2ForTest.ENUM_VALUE_3, retrieve.data.status());
		Assertions.assertEquals("test-label", retrieve.data.label());

		idOfTheObject = retrieve._id;
	}
}
