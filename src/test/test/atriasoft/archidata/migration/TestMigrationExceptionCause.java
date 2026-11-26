package test.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.migration.MigrationException;
import org.atriasoft.archidata.migration.MigrationInterface;
import org.atriasoft.archidata.migration.model.Migration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.InternalServerErrorException;
import test.atriasoft.archidata.ConfigureDb;

/** The {@link MigrationException} of a failed migration keeps the exception that caused it. */
public class TestMigrationExceptionCause {
	/** What a step of the test does. */
	private interface Apply {
		boolean run(DBAccessMongo da) throws Exception;
	}

	private static MigrationInterface migration(final Apply apply, final Exception numberOfStepFailure) {
		return new MigrationInterface() {
			@Override
			public String getName() {
				return "cause test";
			}

			@Override
			public boolean applyMigration(final DBAccessMongo entry, final Migration migration) throws Exception {
				return apply.run(entry);
			}

			@Override
			public int getNumberOfStep() throws Exception {
				if (numberOfStepFailure != null) {
					throw numberOfStepFailure;
				}
				return 1;
			}
		};
	}

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private static MigrationException failure(final MigrationInterface elem, final boolean closeBefore)
			throws Exception {
		try (DBAccessMongo da = DBAccessMongo.createInterface()) {
			if (closeBefore) {
				// the connection is gone: the first write of the migration fails
				da.close();
			}
			return Assertions.assertThrows(MigrationException.class,
					() -> new MigrationEngine().migrateSingle(da, elem, 0, 1));
		}
	}

	@Test
	public void testConnectionFailureIsTheCause() throws Exception {
		final DbConfig noDatabaseName = new DbConfig("localhost", 27017, "user", "pass", null, false, List.of());
		final MigrationException ex = Assertions.assertThrows(MigrationException.class,
				() -> new MigrationEngine().migrateErrorThrow(noDatabaseName));
		Assertions.assertTrue(ex.getMessage().startsWith("Internal server error during migration: "), ex.getMessage());
		Assertions.assertInstanceOf(InternalServerErrorException.class, ex.getCause());
	}

	@Test
	public void testHistoryReadFailureIsTheCause() throws Exception {
		try (DBAccessMongo da = DBAccessMongo.createInterface()) {
			da.close();
			final MigrationException ex = Assertions.assertThrows(MigrationException.class,
					() -> new MigrationEngine().getCurrentVersion(da));
			Assertions.assertEquals("Can not retreive Migration model", ex.getMessage());
			Assertions.assertNotNull(ex.getCause());
		}
	}

	@Test
	public void testNumberOfStepFailureIsTheCause() throws Exception {
		final IllegalStateException failure = new IllegalStateException("no step");
		final MigrationException ex = failure(migration(da -> true, failure), false);
		Assertions.assertTrue(ex.getMessage().startsWith("Fail to get number of migration step"), ex.getMessage());
		Assertions.assertSame(failure, ex.getCause());
	}

	@Test
	public void testLogInsertionFailureIsTheCause() throws Exception {
		final MigrationException ex = failure(migration(da -> true, null), true);
		Assertions.assertTrue(ex.getMessage().startsWith("Fail to insert migration Log"), ex.getMessage());
		Assertions.assertNotNull(ex.getCause());
	}

	@Test
	public void testStepFailureIsTheCause() throws Exception {
		final IOException failure = new IOException("step failed");
		final MigrationException ex = failure(migration(da -> {
			throw failure;
		}, null), false);
		Assertions.assertTrue(ex.getMessage().startsWith("Migration fail: 'cause test' defect @"), ex.getMessage());
		Assertions.assertSame(failure, ex.getCause());
	}

	@Test
	public void testTerminatedUpdateFailureIsTheCause() throws Exception {
		final MigrationException ex = failure(migration(da -> {
			// the connection is lost after the step: the update of the log fails
			da.close();
			return true;
		}, null), false);
		Assertions.assertTrue(ex.getMessage().startsWith("Fail to update migration Log"), ex.getMessage());
		Assertions.assertNotNull(ex.getCause());
	}

	@Test
	public void testFailedStepLogUpdateFailureIsTheCause() throws Exception {
		final MigrationException ex = failure(migration(da -> {
			da.close();
			return false;
		}, null), false);
		Assertions.assertTrue(ex.getMessage().startsWith("Fail to update migration Log"), ex.getMessage());
		Assertions.assertNotNull(ex.getCause());
	}
}
