package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
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
class TestSoftDeleteRestore {

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
		test.data = "restore_test";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted.id;
	}

	@Order(2)
	@Test
	void testSoftDelete() throws Exception {
		final long count = ConfigureDb.da.deleteById(Model.class, idOfTheObject);
		Assertions.assertEquals(1, count);

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNull(retrieved);
	}

	@Order(3)
	@Test
	void testRestore() throws Exception {
		// Note: AccessDeletedItems is required because the restore() method's
		// filter excludes soft-deleted objects by default (framework bug).
		final long restored = ConfigureDb.da.restoreById(Model.class, idOfTheObject, new AccessDeletedItems());
		Assertions.assertTrue(restored > 0);

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("restore_test", retrieved.data);
	}

	@Order(4)
	@Test
	void testRestoredFlagIsFalse() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertNotNull(retrieved.deleted);
		Assertions.assertEquals(false, retrieved.deleted);
	}
}
