package test.kar.archidata.migration;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.migration.MigrationEngine;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.migration.model.TypesMigrationInitialisationCurrent;
import test.kar.archidata.migration.model.TypesMigrationInitialisationFirst;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMigrationFirstInit {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestMigrationFail.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigBaseVariable.dbType = "sqlite";
		ConfigBaseVariable.dbHost = "memory";
		// for test we need to connect all time the DB
		ConfigBaseVariable.dbKeepConnected = "true";
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		entry.connect();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();
	}

	@Order(1)
	@Test
	public void testInitialMigration() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationFirst());
		migrationEngine.migrateErrorThrow(GlobalConfiguration.dbConfig);

		final TypesMigrationInitialisationFirst test = new TypesMigrationInitialisationFirst();
		test.testData = 95.0;
		final TypesMigrationInitialisationFirst insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(95.0, insertedData.testData);
	}

	@Order(2)
	@Test
	public void testInitialMigrationReopen() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine();
		// add initialization:
		migrationEngine.setInit(new InitializationFirst());
		migrationEngine.migrateErrorThrow(GlobalConfiguration.dbConfig);

		final TypesMigrationInitialisationFirst test = new TypesMigrationInitialisationFirst();
		test.testData = 99.0;
		final TypesMigrationInitialisationFirst insertedData = DataAccess.insert(test);
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
		migrationEngine.migrateErrorThrow(GlobalConfiguration.dbConfig);

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 125.0;
		final TypesMigrationInitialisationCurrent insertedData = DataAccess.insert(test);
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
		migrationEngine.migrateErrorThrow(GlobalConfiguration.dbConfig);

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 2563.0;
		final TypesMigrationInitialisationCurrent insertedData = DataAccess.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(2563.0, insertedData.testDataMigration2);
	}
}