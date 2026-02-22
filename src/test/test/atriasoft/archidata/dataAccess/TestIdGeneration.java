package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.UUID;

import org.atriasoft.archidata.dataAccess.options.DirectPrimaryKey;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.bson.types.ObjectId;
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

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestIdGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestIdGeneration.class);

	// --- Models ---

	public static class ModelLong {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public String data;
	}

	public static class ModelOID {
		@Id
		public ObjectId _id = null;
		public String data;
	}

	public static class ModelUUID {
		@Id
		public UUID _id = null;
		public String data;
	}

	// --- State ---

	private static Long longId1 = null;
	private static Long longId2 = null;
	private static Long longId3 = null;
	private static ObjectId oidId1 = null;
	private static ObjectId oidId2 = null;
	private static ObjectId oidId3 = null;
	private static UUID uuidId1 = null;
	private static UUID uuidId2 = null;
	private static UUID uuidId3 = null;

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	// ========================================================================
	// Long ID tests
	// ========================================================================

	@Order(1)
	@Test
	void testLongAutoGeneration() throws Exception {
		final ModelLong test = new ModelLong();
		test.data = "long_auto_1";
		final ModelLong inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);
		Assertions.assertTrue(inserted.id >= 0);
		LOGGER.info("Long auto-generated ID: {}", inserted.id);
		longId1 = inserted.id;
	}

	@Order(2)
	@Test
	void testLongUniqueness() throws Exception {
		final ModelLong test2 = new ModelLong();
		test2.data = "long_auto_2";
		final ModelLong inserted2 = ConfigureDb.da.insert(test2);
		Assertions.assertNotNull(inserted2);
		Assertions.assertNotNull(inserted2.id);
		longId2 = inserted2.id;

		final ModelLong test3 = new ModelLong();
		test3.data = "long_auto_3";
		final ModelLong inserted3 = ConfigureDb.da.insert(test3);
		Assertions.assertNotNull(inserted3);
		Assertions.assertNotNull(inserted3.id);
		longId3 = inserted3.id;

		Assertions.assertNotEquals(longId1, longId2);
		Assertions.assertNotEquals(longId2, longId3);
		Assertions.assertNotEquals(longId1, longId3);
	}

	@Order(3)
	@Test
	void testLongSequential() throws Exception {
		LOGGER.info("Long IDs: {} < {} < {}", longId1, longId2, longId3);
		Assertions.assertTrue(longId1 < longId2, "Long IDs should be sequential: id1 < id2");
		Assertions.assertTrue(longId2 < longId3, "Long IDs should be sequential: id2 < id3");
	}

	@Order(4)
	@Test
	void testLongCrudCycle() throws Exception {
		// Read
		final ModelLong retrieved = ConfigureDb.da.getById(ModelLong.class, longId1);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(longId1, retrieved.id);
		Assertions.assertEquals("long_auto_1", retrieved.data);

		// Update
		final ModelLong updateData = new ModelLong();
		updateData.data = "long_auto_1_updated";
		ConfigureDb.da.updateById(updateData, longId1, new FilterValue("data"));
		final ModelLong afterUpdate = ConfigureDb.da.getById(ModelLong.class, longId1);
		Assertions.assertNotNull(afterUpdate);
		Assertions.assertEquals("long_auto_1_updated", afterUpdate.data);

		// Delete
		ConfigureDb.da.deleteById(ModelLong.class, longId1);
		final ModelLong afterDelete = ConfigureDb.da.getById(ModelLong.class, longId1);
		Assertions.assertNull(afterDelete);
	}

	@Order(5)
	@Test
	void testLongDirectPrimaryKey() throws Exception {
		final ModelLong test = new ModelLong();
		test.id = 999999L;
		test.data = "long_direct_pk";
		final ModelLong inserted = ConfigureDb.da.insert(test, new DirectPrimaryKey());
		Assertions.assertNotNull(inserted);
		Assertions.assertEquals(999999L, inserted.id);

		final ModelLong retrieved = ConfigureDb.da.getById(ModelLong.class, 999999L);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("long_direct_pk", retrieved.data);

		ConfigureDb.da.deleteById(ModelLong.class, 999999L);
	}

	@Order(6)
	@Test
	void testLongPresetIdFails() throws Exception {
		final ModelLong test = new ModelLong();
		test.id = 888888L;
		test.data = "should_fail";
		Assertions.assertThrows(Exception.class, () -> {
			ConfigureDb.da.insert(test);
		});
	}

	// ========================================================================
	// ObjectId tests
	// ========================================================================

	@Order(7)
	@Test
	void testOidAutoGeneration() throws Exception {
		final ModelOID test = new ModelOID();
		test.data = "oid_auto_1";
		final ModelOID inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted._id);
		LOGGER.info("ObjectId auto-generated: {}", inserted._id);
		oidId1 = inserted._id;
	}

	@Order(8)
	@Test
	void testOidUniqueness() throws Exception {
		final ModelOID test2 = new ModelOID();
		test2.data = "oid_auto_2";
		final ModelOID inserted2 = ConfigureDb.da.insert(test2);
		Assertions.assertNotNull(inserted2);
		Assertions.assertNotNull(inserted2._id);
		oidId2 = inserted2._id;

		final ModelOID test3 = new ModelOID();
		test3.data = "oid_auto_3";
		final ModelOID inserted3 = ConfigureDb.da.insert(test3);
		Assertions.assertNotNull(inserted3);
		Assertions.assertNotNull(inserted3._id);
		oidId3 = inserted3._id;

		Assertions.assertNotEquals(oidId1, oidId2);
		Assertions.assertNotEquals(oidId2, oidId3);
		Assertions.assertNotEquals(oidId1, oidId3);
	}

	@Order(9)
	@Test
	void testOidCrudCycle() throws Exception {
		// Read
		final ModelOID retrieved = ConfigureDb.da.getById(ModelOID.class, oidId1);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(oidId1, retrieved._id);
		Assertions.assertEquals("oid_auto_1", retrieved.data);

		// Update
		final ModelOID updateData = new ModelOID();
		updateData.data = "oid_auto_1_updated";
		ConfigureDb.da.updateById(updateData, oidId1, new FilterValue("data"));
		final ModelOID afterUpdate = ConfigureDb.da.getById(ModelOID.class, oidId1);
		Assertions.assertNotNull(afterUpdate);
		Assertions.assertEquals("oid_auto_1_updated", afterUpdate.data);

		// Delete
		ConfigureDb.da.deleteById(ModelOID.class, oidId1);
		final ModelOID afterDelete = ConfigureDb.da.getById(ModelOID.class, oidId1);
		Assertions.assertNull(afterDelete);
	}

	@Order(10)
	@Test
	void testOidDirectPrimaryKey() throws Exception {
		final ObjectId customOid = new ObjectId();
		final ModelOID test = new ModelOID();
		test._id = customOid;
		test.data = "oid_direct_pk";
		final ModelOID inserted = ConfigureDb.da.insert(test, new DirectPrimaryKey());
		Assertions.assertNotNull(inserted);
		Assertions.assertEquals(customOid, inserted._id);

		final ModelOID retrieved = ConfigureDb.da.getById(ModelOID.class, customOid);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("oid_direct_pk", retrieved.data);

		ConfigureDb.da.deleteById(ModelOID.class, customOid);
	}

	@Order(11)
	@Test
	void testOidPresetIdFails() throws Exception {
		final ModelOID test = new ModelOID();
		test._id = new ObjectId();
		test.data = "should_fail";
		Assertions.assertThrows(Exception.class, () -> {
			ConfigureDb.da.insert(test);
		});
	}

	// ========================================================================
	// UUID tests
	// ========================================================================

	@Order(12)
	@Test
	void testUuidAutoGeneration() throws Exception {
		final ModelUUID test = new ModelUUID();
		test.data = "uuid_auto_1";
		final ModelUUID inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted._id);
		LOGGER.info("UUID auto-generated: {}", inserted._id);
		uuidId1 = inserted._id;
	}

	@Order(13)
	@Test
	void testUuidUniqueness() throws Exception {
		final ModelUUID test2 = new ModelUUID();
		test2.data = "uuid_auto_2";
		final ModelUUID inserted2 = ConfigureDb.da.insert(test2);
		Assertions.assertNotNull(inserted2);
		Assertions.assertNotNull(inserted2._id);
		uuidId2 = inserted2._id;

		final ModelUUID test3 = new ModelUUID();
		test3.data = "uuid_auto_3";
		final ModelUUID inserted3 = ConfigureDb.da.insert(test3);
		Assertions.assertNotNull(inserted3);
		Assertions.assertNotNull(inserted3._id);
		uuidId3 = inserted3._id;

		Assertions.assertNotEquals(uuidId1, uuidId2);
		Assertions.assertNotEquals(uuidId2, uuidId3);
		Assertions.assertNotEquals(uuidId1, uuidId3);
	}

	@Order(14)
	@Test
	void testUuidCrudCycle() throws Exception {
		// Read
		final ModelUUID retrieved = ConfigureDb.da.getById(ModelUUID.class, uuidId1);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(uuidId1, retrieved._id);
		Assertions.assertEquals("uuid_auto_1", retrieved.data);

		// Update
		final ModelUUID updateData = new ModelUUID();
		updateData.data = "uuid_auto_1_updated";
		ConfigureDb.da.updateById(updateData, uuidId1, new FilterValue("data"));
		final ModelUUID afterUpdate = ConfigureDb.da.getById(ModelUUID.class, uuidId1);
		Assertions.assertNotNull(afterUpdate);
		Assertions.assertEquals("uuid_auto_1_updated", afterUpdate.data);

		// Delete
		ConfigureDb.da.deleteById(ModelUUID.class, uuidId1);
		final ModelUUID afterDelete = ConfigureDb.da.getById(ModelUUID.class, uuidId1);
		Assertions.assertNull(afterDelete);
	}

	@Order(15)
	@Test
	void testUuidDirectPrimaryKey() throws Exception {
		final UUID customUuid = UUID.randomUUID();
		final ModelUUID test = new ModelUUID();
		test._id = customUuid;
		test.data = "uuid_direct_pk";
		final ModelUUID inserted = ConfigureDb.da.insert(test, new DirectPrimaryKey());
		Assertions.assertNotNull(inserted);
		Assertions.assertEquals(customUuid, inserted._id);

		final ModelUUID retrieved = ConfigureDb.da.getById(ModelUUID.class, customUuid);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("uuid_direct_pk", retrieved.data);

		ConfigureDb.da.deleteById(ModelUUID.class, customUuid);
	}

	@Order(16)
	@Test
	void testUuidPresetIdFails() throws Exception {
		final ModelUUID test = new ModelUUID();
		test._id = UUID.randomUUID();
		test.data = "should_fail";
		Assertions.assertThrows(Exception.class, () -> {
			ConfigureDb.da.insert(test);
		});
	}
}
