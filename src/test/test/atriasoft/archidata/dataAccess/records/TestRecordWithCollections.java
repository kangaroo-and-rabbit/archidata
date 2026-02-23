package test.atriasoft.archidata.dataAccess.records;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
class TestRecordWithCollections {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestRecordWithCollections.class);

	public record RecWithColl(
			String name,
			List<Integer> values,
			Map<String, String> labels) {}

	public static class Model {
		@Id
		public ObjectId _id = null;

		public RecWithColl data;
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
		test.data = new RecWithColl("collections-test", List.of(1, 2, 3, 4, 5),
				Map.of("key1", "value1", "key2", "value2"));

		final Model insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData._id);
		Assertions.assertNotNull(insertedData._id);

		final Model retrieve = ConfigureDb.da.getById(Model.class, insertedData._id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve._id);
		Assertions.assertEquals(insertedData._id, retrieve._id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals("collections-test", retrieve.data.name());
		Assertions.assertNotNull(retrieve.data.values());
		Assertions.assertEquals(List.of(1, 2, 3, 4, 5), retrieve.data.values());
		Assertions.assertNotNull(retrieve.data.labels());
		Assertions.assertEquals("value1", retrieve.data.labels().get("key1"));
		Assertions.assertEquals("value2", retrieve.data.labels().get("key2"));
		Assertions.assertEquals(2, retrieve.data.labels().size());

		idOfTheObject = retrieve._id;
	}
}
