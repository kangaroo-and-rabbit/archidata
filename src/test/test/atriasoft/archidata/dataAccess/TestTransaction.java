package test.atriasoft.archidata.dataAccess;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.TransactionContext;
import org.atriasoft.archidata.exception.DataAccessException;
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

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.SimpleTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestTransaction {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTransaction.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testTransactionCommit() throws Exception {
		final SimpleTable test = new SimpleTable();
		test.data = "transaction-commit-test";

		ConfigureDb.da.startTransaction();
		final SimpleTable inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.getId());
		ConfigureDb.da.commitTransaction();

		// After commit, data should be visible
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, inserted.getId());
		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals("transaction-commit-test", retrieve.data);
	}

	@Order(2)
	@Test
	public void testTransactionAbort() throws Exception {
		final SimpleTable test = new SimpleTable();
		test.data = "transaction-abort-test";

		ConfigureDb.da.startTransaction();
		final SimpleTable inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.getId());
		final Long insertedId = inserted.getId();
		ConfigureDb.da.abortTransaction();

		// After abort, data should NOT be visible
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, insertedId);
		Assertions.assertNull(retrieve);
	}

	@Order(3)
	@Test
	public void testTransactionContextAutoAbort() throws Exception {
		final SimpleTable test = new SimpleTable();
		test.data = "transaction-auto-abort-test";
		Long insertedId = null;

		try (TransactionContext tx = new TransactionContext(ConfigureDb.da)) {
			final SimpleTable inserted = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(inserted);
			insertedId = inserted.getId();
			// Do NOT call tx.commit() — auto-abort on close
		}

		// After auto-abort, data should NOT be visible
		Assertions.assertNotNull(insertedId);
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, insertedId);
		Assertions.assertNull(retrieve);
	}

	@Order(4)
	@Test
	public void testTransactionContextCommit() throws Exception {
		final SimpleTable test = new SimpleTable();
		test.data = "transaction-context-commit-test";

		try (TransactionContext tx = new TransactionContext(ConfigureDb.da)) {
			final SimpleTable inserted = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(inserted);
			tx.commit();
		}

		// After commit via TransactionContext, data should be visible
		// We verify by querying — since we don't have the ID outside, we use gets
		final java.util.List<SimpleTable> results = ConfigureDb.da.gets(SimpleTable.class,
				new org.atriasoft.archidata.dataAccess.options.Condition(
						new org.atriasoft.archidata.dataAccess.QueryCondition("data", "=",
								"transaction-context-commit-test")));
		Assertions.assertFalse(results.isEmpty());
		Assertions.assertEquals("transaction-context-commit-test", results.get(0).data);
	}

	@Order(5)
	@Test
	public void testTransactionUpdateCommit() throws Exception {
		// First insert without transaction
		final SimpleTable test = new SimpleTable();
		test.data = "before-update";
		final SimpleTable inserted = ConfigureDb.da.insert(test);

		// Update within a transaction
		final SimpleTable updateData = new SimpleTable();
		updateData.data = "after-update";

		try (TransactionContext tx = new TransactionContext(ConfigureDb.da)) {
			ConfigureDb.da.updateById(updateData, inserted.getId(),
					new org.atriasoft.archidata.dataAccess.options.FilterValue("data"));
			tx.commit();
		}

		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, inserted.getId());
		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals("after-update", retrieve.data);
	}

	@Order(6)
	@Test
	public void testTransactionDeleteCommit() throws Exception {
		// First insert without transaction
		final SimpleTable test = new SimpleTable();
		test.data = "to-be-deleted";
		final SimpleTable inserted = ConfigureDb.da.insert(test);

		// Delete within a transaction
		try (TransactionContext tx = new TransactionContext(ConfigureDb.da)) {
			ConfigureDb.da.deleteHardById(SimpleTable.class, inserted.getId());
			tx.commit();
		}

		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, inserted.getId());
		Assertions.assertNull(retrieve);
	}

	@Order(7)
	@Test
	public void testDoubleStartTransactionThrows() throws Exception {
		ConfigureDb.da.startTransaction();
		try {
			Assertions.assertThrows(DataAccessException.class, () -> {
				ConfigureDb.da.startTransaction();
			});
		} finally {
			ConfigureDb.da.abortTransaction();
		}
	}

	@Order(8)
	@Test
	public void testCommitWithoutTransactionThrows() {
		Assertions.assertThrows(DataAccessException.class, () -> {
			ConfigureDb.da.commitTransaction();
		});
	}

	@Order(9)
	@Test
	public void testAbortWithoutTransactionThrows() {
		Assertions.assertThrows(DataAccessException.class, () -> {
			ConfigureDb.da.abortTransaction();
		});
	}

	@Order(10)
	@Test
	public void testIsTransactionActive() throws Exception {
		Assertions.assertFalse(ConfigureDb.da.isTransactionActive());

		ConfigureDb.da.startTransaction();
		Assertions.assertTrue(ConfigureDb.da.isTransactionActive());

		ConfigureDb.da.commitTransaction();
		Assertions.assertFalse(ConfigureDb.da.isTransactionActive());
	}
}
