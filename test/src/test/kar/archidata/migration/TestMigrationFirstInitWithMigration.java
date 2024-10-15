package test.kar.archidata.migration;

import java.io.IOException;
import java.util.List;

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
import org.kar.archidata.migration.MigrationEngine;
import org.kar.archidata.migration.model.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.migration.model.TypesMigrationInitialisationCurrent;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestMigrationFirstInitWithMigration {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestMigrationFirstInitWithMigration.class);

	private DataAccess da = null;

	public TestMigrationFirstInitWithMigration() {
		this.da = DataAccess.createInterface();
	}

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
		final MigrationEngine migrationEngine = new MigrationEngine(this.da);
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		// add migration for old version
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		Assertions.assertDoesNotThrow(() -> migrationEngine.migrateErrorThrow(GlobalConfiguration.getDbconfig()));

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 95.0;
		final TypesMigrationInitialisationCurrent insertedData = this.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(95.0, insertedData.testDataMigration2);

		final List<Migration> elements = this.da.gets(Migration.class);
		LOGGER.info("List of migrations:");
		for (final Migration elem : elements) {
			LOGGER.info("    - {} => {}", elem.id, elem.name);
		}
	}

	@Order(2)
	@Test
	public void testInitialMigrationReopen() throws Exception {
		final MigrationEngine migrationEngine = new MigrationEngine(this.da);
		// add initialization:
		migrationEngine.setInit(new InitializationCurrent());
		// add migration for old version
		migrationEngine.add(new Migration1());
		migrationEngine.add(new Migration2());
		migrationEngine.migrateErrorThrow(GlobalConfiguration.getDbconfig());

		final TypesMigrationInitialisationCurrent test = new TypesMigrationInitialisationCurrent();
		test.testDataMigration2 = 99.0;
		final TypesMigrationInitialisationCurrent insertedData = this.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertEquals(99.0, insertedData.testDataMigration2);
	}
}
