package test.atriasoft.archidata.migration;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.migration.model.Migration;
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
import test.atriasoft.archidata.migration.model.TypesMigrationInitialisationCurrent;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMigrationFirstInitWithMigration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestMigrationFirstInitWithMigration.class);

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
	public void testInitialMigration() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		// add migration for old version
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		Assertions.assertDoesNotThrow(() -> migrationEngine.migrateErrorThrow(new DbConfig()));

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 95.0;
		final TypesMigrationInitialisationCurrent insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(95.0, insertedData.testDataMigration2);

		final List<Migration> elements = ConfigureDb.da.gets(Migration.class);
		LOGGER.info("List of migrations:");
		for (final Migration elem : elements) {
			LOGGER.info("    - {} => {}", elem.id, elem.name);
		}
	}

	@Order(2)
	@Test
	public void testInitialMigrationReopen() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		// add migration for old version
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		migrationEngine.migrateErrorThrow(new DbConfig());

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 99.0;
		final TypesMigrationInitialisationCurrent insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(99.0, insertedData.testDataMigration2);
	}
}
