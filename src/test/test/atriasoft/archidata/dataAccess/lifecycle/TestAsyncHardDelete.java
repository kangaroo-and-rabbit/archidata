package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.ForceHardDelete;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryOr;
import org.atriasoft.archidata.model.OIDGenericDataSoftAsyncHardDelete;
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

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestAsyncHardDelete {

	public static class Model extends OIDGenericDataSoftAsyncHardDelete {
		public String data;
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
		final Model test = new Model();
		test.data = "async_hard_delete_test";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.getOid());
		idOfTheObject = inserted.getOid();

		final Model test2 = new Model();
		test2.data = "force_hard_delete_test";
		final Model inserted2 = ConfigureDb.da.insert(test2);
		Assertions.assertNotNull(inserted2);
		idOfTheObject2 = inserted2.getOid();
	}

	@Order(2)
	@Test
	void testRetrieveAfterInsert() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("async_hard_delete_test", retrieved.data);
		Assertions.assertNotNull(retrieved.getDeleted());
		Assertions.assertEquals(false, retrieved.getDeleted());
		Assertions.assertNull(retrieved.getHardDeleted());
	}

	@Order(3)
	@Test
	void testDeleteHardSetsAsyncFlag() throws Exception {
		// deleteHardById without ForceHardDelete should only set hardDeleted=true and deleted=true
		final long count = ConfigureDb.da.deleteHardById(Model.class, idOfTheObject);
		Assertions.assertEquals(1, count);

		// The object should not be visible in normal queries (deleted=true)
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNull(retrieved);

		// But it should still exist in DB with hardDeleted=true when accessing deleted items
		final Model retrievedWithDeleted = ConfigureDb.da.getById(Model.class, idOfTheObject, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertNotNull(retrievedWithDeleted);
		Assertions.assertEquals("async_hard_delete_test", retrievedWithDeleted.data);
		Assertions.assertEquals(true, retrievedWithDeleted.getDeleted());
		Assertions.assertEquals(true, retrievedWithDeleted.getHardDeleted());
	}

	@Order(4)
	@Test
	void testForceHardDeleteRemovesPhysically() throws Exception {
		// With ForceHardDelete option, the object should be physically removed
		final long count = ConfigureDb.da.deleteHardById(Model.class, idOfTheObject2, new ForceHardDelete());
		Assertions.assertEquals(1, count);

		// The object should be completely gone, even with AccessDeletedItems
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject2, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertNull(retrieved);
	}

	@Order(5)
	@Test
	void testForceHardDeleteOnAsyncDeletedObject() throws Exception {
		// The first object was async-hard-deleted in step 3, now force-delete it
		final long count = ConfigureDb.da.deleteHardById(Model.class, idOfTheObject, new ForceHardDelete());
		Assertions.assertEquals(1, count);

		// Should be completely gone now
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertNull(retrieved);
	}

	@Order(6)
	@Test
	void testDeleteHardBulkWithCondition() throws Exception {
		// Insert multiple objects
		final Model a = new Model();
		a.data = "bulk_a";
		ConfigureDb.da.insert(a);

		final Model b = new Model();
		b.data = "bulk_b";
		ConfigureDb.da.insert(b);

		// deleteHard with a condition matching both, without ForceHardDelete => async-delete
		final Condition bulkCondition = new Condition(new QueryOr(
				List.of(new QueryCondition("data", "=", "bulk_a"), new QueryCondition("data", "=", "bulk_b"))));
		final long count = ConfigureDb.da.deleteHard(Model.class, bulkCondition);
		Assertions.assertEquals(2, count);

		// Normal query returns nothing
		final List<Model> normalList = ConfigureDb.da.gets(Model.class, bulkCondition);
		Assertions.assertEquals(0, normalList.size());

		// But both still exist with hardDeleted=true
		final List<Model> deletedList = ConfigureDb.da.gets(Model.class, bulkCondition, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(2, deletedList.size());
		for (final Model m : deletedList) {
			Assertions.assertEquals(true, m.getDeleted());
			Assertions.assertEquals(true, m.getHardDeleted());
		}

		// Force hard delete to clean up
		final long forceCount = ConfigureDb.da.deleteHard(Model.class, bulkCondition, new ForceHardDelete());
		Assertions.assertEquals(2, forceCount);

		final List<Model> afterForce = ConfigureDb.da.gets(Model.class, bulkCondition, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(0, afterForce.size());
	}
}
