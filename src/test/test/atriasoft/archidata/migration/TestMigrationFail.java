package test.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.migration.MigrationException;
import org.atriasoft.archidata.migration.model.Migration;
import org.atriasoft.archidata.migration.model.MigrationMessage;
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
import test.atriasoft.archidata.migration.model.TypesMigrationInitialisationFirst;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMigrationFail {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestMigrationFirstInit.class);

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
	public void testInitializeTable() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationFirst());
		migrationEngine.migrateErrorThrow(new DbConfig());

		final TypesMigrationInitialisationFirst test = new TypesMigrationInitialisationFirst();
		test.testData = 95.0;
		final TypesMigrationInitialisationFirst insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(95.0, insertedData.testData);
	}

	@Order(2)
	@Test
	public void testUpdateTwoMigration() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		migrationEngine.add(new Migration1());
		migrationEngine.add(new MigrationFail());
		Assertions.assertThrows(MigrationException.class, () -> {
			migrationEngine.migrateErrorThrow(new DbConfig());
		});
	}

	@Order(3)
	@Test
	public void testFailedMigrationLogsArePersisted() throws Exception {
		final List<Migration> elements = ConfigureDb.da.gets(Migration.class);
		final Migration failed = elements.stream().filter(elem -> "Fail migration Test".equals(elem.name)).findFirst()
				.orElse(null);
		Assertions.assertNotNull(failed);
		Assertions.assertFalse(failed.terminated);
		Assertions.assertEquals(1, failed.stepId);
		Assertions.assertNotNull(failed.logs);
		Assertions.assertTrue(
				failed.logs.stream().map((final MigrationMessage msg) -> msg.message)
						.anyMatch(msg -> msg != null && msg.startsWith("DB request ERROR: FAIL migration")),
				"intermediate logs must be persisted, got: " + failed.logs.size() + " message(s)");
		Assertions.assertTrue(failed.logs.stream().map((final MigrationMessage msg) -> msg.message)
				.anyMatch("Fail in the migration engine..."::equals));
		final Migration done = elements.stream().filter(elem -> new Migration1().getName().equals(elem.name))
				.findFirst().orElse(null);
		Assertions.assertNotNull(done);
		Assertions.assertTrue(
				done.logs.stream().map((final MigrationMessage msg) -> msg.message).anyMatch("==> DONE"::equals));
	}

}
