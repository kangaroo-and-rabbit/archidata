package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;
import java.time.Duration;

import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.model.GenericData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestTimestamps {

	public static class Model extends GenericData {
		public String data;
	}

	private static Long idOfTheObject = null;

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
	void testInsertSetsTimestamps() throws Exception {
		final Model test = new Model();
		test.data = "timestamp_test";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted.id;

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertNotNull(retrieved.createdAt);
		Assertions.assertNotNull(retrieved.updatedAt);
		Assertions.assertEquals(retrieved.createdAt, retrieved.updatedAt);
	}

	@Order(2)
	@Test
	void testTimestampsNotReadByDefault() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertNull(retrieved.createdAt);
		Assertions.assertNull(retrieved.updatedAt);
	}

	@Order(3)
	@Test
	void testUpdateChangesUpdatedAt() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		final Model update = new Model();
		update.data = "timestamp_updated";
		ConfigureDb.da.updateById(update, idOfTheObject, new FilterValue("data"));

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("timestamp_updated", retrieved.data);
		Assertions.assertNotNull(retrieved.createdAt);
		Assertions.assertNotNull(retrieved.updatedAt);
		Assertions.assertTrue(retrieved.updatedAt.after(retrieved.createdAt));
	}
}
