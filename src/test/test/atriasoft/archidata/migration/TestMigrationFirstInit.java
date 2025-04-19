package test.atriasoft.archidata.migration;

import java.io.IOException;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.migration.MigrationEngine;
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
import test.atriasoft.archidata.migration.model.TypesMigrationInitialisationFirst;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMigrationFirstInit {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestMigrationFail.class);

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
	public void testInitialMigrationReopen() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationFirst());
		migrationEngine.migrateErrorThrow(new DbConfig());

		final TypesMigrationInitialisationFirst test = new TypesMigrationInitialisationFirst();
		test.testData = 99.0;
		final TypesMigrationInitialisationFirst insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(99.0, insertedData.testData);
	}

	@Order(3)
	@Test
	public void testUpdateTwoMigration() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		migrationEngine.migrateErrorThrow(new DbConfig());

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 125.0;
		final TypesMigrationInitialisationCurrent insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(125.0, insertedData.testDataMigration2);
	}

	@Order(4)
	@Test
	public void testUpdateTwoMigrationReopen() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		migrationEngine.migrateErrorThrow(new DbConfig());

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 2563.0;
		final TypesMigrationInitialisationCurrent insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(2563.0, insertedData.testDataMigration2);
	}
}
