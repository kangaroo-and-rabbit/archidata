package test.atriasoft.archidata.tools;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.atriasoft.archidata.annotation.DataAsyncHardDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.model.OIDGenericDataSoftAsyncHardDelete;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.atriasoft.archidata.tools.CleanupTools;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestCleanupTools {

	// Model with @DataDeleted only (soft delete)
	public static class SoftDeleteModel extends OIDGenericDataSoftDelete {
		public String data;
	}

	// Model with @DataDeleted + @DataAsyncHardDeleted
	public static class AsyncHardDeleteModel extends OIDGenericDataSoftAsyncHardDelete {
		public String data;
	}

	// Model without any delete field (no @DataDeleted, no @DataAsyncHardDeleted)
	public static class PlainModel {
		@Id
		public ObjectId _id = null;
		public String data;
	}

	// Model with @DataDeleted but no @UpdateTimestamp
	public static class NoTimestampModel {
		@Id
		public ObjectId _id = null;
		@DataNotRead
		@org.atriasoft.archidata.annotation.DataDeleted
		public Boolean deleted = null;
		public String data;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	// ===== Soft delete cleanup tests =====

	@Order(1)
	@Test
	void testCleanupSoftDeleted_removesOldRecords() throws Exception {
		// Insert and soft-delete a record
		final SoftDeleteModel obj = new SoftDeleteModel();
		obj.data = "soft_old";
		final SoftDeleteModel inserted = ConfigureDb.da.insert(obj);
		ConfigureDb.da.deleteById(SoftDeleteModel.class, inserted.getOid());

		// Cleanup with threshold in the future => should remove the record
		final Date futureThreshold = Date.from(Instant.now().plus(Duration.ofHours(1)));
		final long count = CleanupTools.cleanupSoftDeletedRecords(SoftDeleteModel.class, futureThreshold);
		Assertions.assertEquals(1, count);

		// Record should be physically gone
		final SoftDeleteModel retrieved = ConfigureDb.da.getById(SoftDeleteModel.class, inserted.getOid(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(retrieved);
	}

	@Order(2)
	@Test
	void testCleanupSoftDeleted_keepsRecentRecords() throws Exception {
		// Insert and soft-delete a record
		final SoftDeleteModel obj = new SoftDeleteModel();
		obj.data = "soft_recent";
		final SoftDeleteModel inserted = ConfigureDb.da.insert(obj);
		ConfigureDb.da.deleteById(SoftDeleteModel.class, inserted.getOid());

		// Cleanup with threshold in the past => should NOT remove the record
		final Date pastThreshold = Date.from(Instant.now().minus(Duration.ofHours(1)));
		final long count = CleanupTools.cleanupSoftDeletedRecords(SoftDeleteModel.class, pastThreshold);
		Assertions.assertEquals(0, count);

		// Record should still exist
		final SoftDeleteModel retrieved = ConfigureDb.da.getById(SoftDeleteModel.class, inserted.getOid(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(true, retrieved.getDeleted());

		// Cleanup for next tests
		ConfigureDb.da.deleteHardById(SoftDeleteModel.class, inserted.getOid());
	}

	// ===== Async hard delete cleanup tests =====

	@Order(3)
	@Test
	void testCleanupAsyncHardDeleted_removesOldRecords() throws Exception {
		// Insert and async-hard-delete a record
		final AsyncHardDeleteModel obj = new AsyncHardDeleteModel();
		obj.data = "async_old";
		final AsyncHardDeleteModel inserted = ConfigureDb.da.insert(obj);
		ConfigureDb.da.deleteHardById(AsyncHardDeleteModel.class, inserted.getOid());

		// Verify it's async-hard-deleted (not physically removed)
		final AsyncHardDeleteModel before = ConfigureDb.da.getById(AsyncHardDeleteModel.class, inserted.getOid(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(before);
		Assertions.assertEquals(true, before.getHardDeleted());
		Assertions.assertEquals(true, before.getDeleted());

		// Cleanup with threshold in the future => should physically remove
		final Date futureThreshold = Date.from(Instant.now().plus(Duration.ofHours(1)));
		final long count = CleanupTools.cleanupAsyncHardDeletedRecords(AsyncHardDeleteModel.class, futureThreshold);
		Assertions.assertEquals(1, count);

		// Record should be physically gone
		final AsyncHardDeleteModel after = ConfigureDb.da.getById(AsyncHardDeleteModel.class, inserted.getOid(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(after);
	}

	@Order(4)
	@Test
	void testCleanupAsyncHardDeleted_keepsRecentRecords() throws Exception {
		// Insert and async-hard-delete a record
		final AsyncHardDeleteModel obj = new AsyncHardDeleteModel();
		obj.data = "async_recent";
		final AsyncHardDeleteModel inserted = ConfigureDb.da.insert(obj);
		ConfigureDb.da.deleteHardById(AsyncHardDeleteModel.class, inserted.getOid());

		// Cleanup with threshold in the past => should NOT remove
		final Date pastThreshold = Date.from(Instant.now().minus(Duration.ofHours(1)));
		final long count = CleanupTools.cleanupAsyncHardDeletedRecords(AsyncHardDeleteModel.class, pastThreshold);
		Assertions.assertEquals(0, count);

		// Record should still exist
		final AsyncHardDeleteModel retrieved = ConfigureDb.da.getById(AsyncHardDeleteModel.class, inserted.getOid(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(true, retrieved.getHardDeleted());

		// Cleanup for next tests
		ConfigureDb.da.deleteHardById(AsyncHardDeleteModel.class, inserted.getOid(),
				new org.atriasoft.archidata.dataAccess.options.ForceHardDelete());
	}

	// ===== Error cases =====

	@Order(5)
	@Test
	void testCleanupSoftDeleted_failsWithoutDeletedField() {
		final Date threshold = Date.from(Instant.now());
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> CleanupTools.cleanupSoftDeletedRecords(PlainModel.class, threshold));
		Assertions.assertTrue(ex.getMessage().contains("has no @DataDeleted field"),
				"Expected message about missing @DataDeleted, got: " + ex.getMessage());
	}

	@Order(6)
	@Test
	void testCleanupAsyncHardDeleted_failsWithoutAsyncHardDeletedField() {
		final Date threshold = Date.from(Instant.now());
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> CleanupTools.cleanupAsyncHardDeletedRecords(SoftDeleteModel.class, threshold));
		Assertions.assertTrue(ex.getMessage().contains("has no @DataAsyncHardDeleted field"),
				"Expected message about missing @DataAsyncHardDeleted, got: " + ex.getMessage());
	}

	@Order(7)
	@Test
	void testCleanupSoftDeleted_failsWithoutUpdateTimestamp() {
		final Date threshold = Date.from(Instant.now());
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> CleanupTools.cleanupSoftDeletedRecords(NoTimestampModel.class, threshold));
		Assertions.assertTrue(ex.getMessage().contains("has no @UpdateTimestamp field"),
				"Expected message about missing @UpdateTimestamp, got: " + ex.getMessage());
	}
}
