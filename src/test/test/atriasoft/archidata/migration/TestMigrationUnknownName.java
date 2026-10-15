package test.atriasoft.archidata.migration;

import java.io.IOException;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.migration.MigrationException;
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
public class TestMigrationUnknownName {

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
	public void testInitialize() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		migrationEngine.setInit(new InitializationFirst());
		migrationEngine.add(new Migration1());
		// Fresh database: the init runs and a placeholder records "Migration1" as the last one.
		migrationEngine.migrateErrorThrow(new DbConfig());
	}

	@Order(2)
	@Test
	public void testLastMigrationRemovedIsRejected() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		migrationEngine.setInit(new InitializationCurrent());
		// Migration1 has been renamed or removed: the recorded name is unknown.
		migrationEngine.add(new Migration2());
		final MigrationException ex = Assertions.assertThrows(MigrationException.class,
				() -> migrationEngine.migrateErrorThrow(new DbConfig()));
		Assertions.assertTrue(ex.getMessage().contains(new Migration1().getName()), ex.getMessage());
	}

	@Order(3)
	@Test
	public void testRevertIsNotSupported() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> migrationEngine.revertSingle(ConfigureDb.da, new Migration1(), 0, 1));
	}

}
