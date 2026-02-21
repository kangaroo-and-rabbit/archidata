package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;
import java.time.Duration;

import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.model.GenericDataSoftDelete;
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
class TestHardDeleteSoftDeletedObject {

	public static class Model extends GenericDataSoftDelete {
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
	void testInsert() throws Exception {
		final Model test = new Model();
		test.data = "soft_delete_timestamps";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted.id;
	}

	@Order(2)
	@Test
	void testTimestampsOnInsert() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertNotNull(retrieved.createdAt);
		Assertions.assertNotNull(retrieved.updatedAt);
		Assertions.assertEquals(retrieved.createdAt, retrieved.updatedAt);
		Assertions.assertNotNull(retrieved.deleted);
		Assertions.assertEquals(false, retrieved.deleted);
	}

	@Order(3)
	@Test
	void testUpdateBeforeSoftDelete() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		final Model update = new Model();
		update.data = "updated_before_delete";
		ConfigureDb.da.updateById(update, idOfTheObject, new FilterValue("data"));

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("updated_before_delete", retrieved.data);
		Assertions.assertTrue(retrieved.updatedAt.after(retrieved.createdAt));
		Assertions.assertEquals(false, retrieved.deleted);
	}

	@Order(4)
	@Test
	void testSoftDeleteUpdatesTimestamp() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		final long count = ConfigureDb.da.deleteById(Model.class, idOfTheObject);
		Assertions.assertEquals(1, count);

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("updated_before_delete", retrieved.data);
		Assertions.assertNotNull(retrieved.createdAt);
		Assertions.assertNotNull(retrieved.updatedAt);
		Assertions.assertTrue(retrieved.updatedAt.after(retrieved.createdAt));
		Assertions.assertEquals(true, retrieved.deleted);
	}
}
