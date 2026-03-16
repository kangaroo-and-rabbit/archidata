package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;

import org.atriasoft.archidata.annotation.DataAsyncHardDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ForceHardDelete;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;
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

/**
 * Tests for objects with only @DataAsyncHardDeleted (no @DataDeleted soft delete flag).
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestAsyncHardDeleteOnly {

	public static class ModelHardOnly extends OIDGenericData {
		public String data;

		@DataNotRead
		@DataAsyncHardDeleted
		public Boolean hardDeleted = null;

		public Boolean getHardDeleted() {
			return this.hardDeleted;
		}

		public void setHardDeleted(final Boolean hardDeleted) {
			this.hardDeleted = hardDeleted;
		}
	}

	private static ObjectId idOfTheObject = null;
	private static ObjectId idOfTheObject2 = null;

	@BeforeAll
	static void setup() throws Exception {
		idOfTheObject = null;
		idOfTheObject2 = null;
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testInsert() throws Exception {
		final ModelHardOnly test = new ModelHardOnly();
		test.data = "hard_only_test";
		final ModelHardOnly inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.getOid());
		idOfTheObject = inserted.getOid();

		final ModelHardOnly test2 = new ModelHardOnly();
		test2.data = "hard_only_force";
		final ModelHardOnly inserted2 = ConfigureDb.da.insert(test2);
		Assertions.assertNotNull(inserted2);
		idOfTheObject2 = inserted2.getOid();
	}

	@Order(2)
	@Test
	void testRetrieveAfterInsert() throws Exception {
		final ModelHardOnly retrieved = ConfigureDb.da.getById(ModelHardOnly.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("hard_only_test", retrieved.data);
		Assertions.assertNull(retrieved.getHardDeleted());
	}

	@Order(3)
	@Test
	void testDeleteHardSetsAsyncFlagOnly() throws Exception {
		// deleteHardById without ForceHardDelete should set hardDeleted=true
		final long count = ConfigureDb.da.deleteHardById(ModelHardOnly.class, idOfTheObject);
		Assertions.assertEquals(1, count);

		// Object still exists in DB with hardDeleted=true (no soft delete to filter it out,
		// so it remains visible in normal queries since there's no @DataDeleted)
		final ModelHardOnly retrieved = ConfigureDb.da.getById(ModelHardOnly.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("hard_only_test", retrieved.data);
		Assertions.assertEquals(true, retrieved.getHardDeleted());
	}

	@Order(4)
	@Test
	void testForceHardDeleteRemovesPhysically() throws Exception {
		// With ForceHardDelete, the object should be physically removed
		final long count = ConfigureDb.da.deleteHardById(ModelHardOnly.class, idOfTheObject2, new ForceHardDelete());
		Assertions.assertEquals(1, count);

		// The object should be completely gone
		final ModelHardOnly retrieved = ConfigureDb.da.getById(ModelHardOnly.class, idOfTheObject2,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(retrieved);
	}

	@Order(5)
	@Test
	void testForceHardDeleteOnAsyncDeletedObject() throws Exception {
		// The first object was async-hard-deleted in step 3, now force-delete it
		final long count = ConfigureDb.da.deleteHardById(ModelHardOnly.class, idOfTheObject, new ForceHardDelete());
		Assertions.assertEquals(1, count);

		// Should be completely gone now
		final ModelHardOnly retrieved = ConfigureDb.da.getById(ModelHardOnly.class, idOfTheObject,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(retrieved);
	}
}
