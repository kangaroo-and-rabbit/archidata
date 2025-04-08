package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
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
import test.atriasoft.archidata.dataAccess.model.SimpleTableSoftDelete;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSimpleTableSoftDelete {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestSimpleTableSoftDelete.class);
	private static final String DATA_INJECTED = "kjhlkjhlkjghlmkkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFL";
	private static final String DATA_INJECTED_2 = "qsdfqsdfqsdfsqdf";
	private static Long idOfTheObject = null;
	private static Timestamp startAction = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		// Clear the static test:
		idOfTheObject = null;
		startAction = null;

		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testTableInsertAndRetrieve() throws Exception {
		TestSimpleTableSoftDelete.startAction = Timestamp.from(Instant.now());
		final List<String> sqlCommand = DataFactory.createTable(SimpleTableSoftDelete.class);
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
		final SimpleTableSoftDelete test = new SimpleTableSoftDelete();
		test.data = TestSimpleTableSoftDelete.DATA_INJECTED;
		final SimpleTableSoftDelete insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED, retrieve.data);
		Assertions.assertNull(retrieve.createdAt);
		Assertions.assertNull(retrieve.updatedAt);
		Assertions.assertNull(retrieve.deleted);
		TestSimpleTableSoftDelete.idOfTheObject = retrieve.id;
	}

	@Order(2)
	@Test
	public void testReadAllValuesUnreadable() throws Exception {
		// check the full values
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, QueryOptions.READ_ALL_COLOMN);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED, retrieve.data);
		Assertions.assertNotNull(retrieve.createdAt);
		LOGGER.info("start @ {} create @ {}", retrieve.createdAt.toInstant(),
				TestSimpleTableSoftDelete.startAction.toInstant());
		// Gros travail sur les timestamp a faire pour que ce soit correct ...
		// Assertions.assertTrue(retrieve.createdAt.after(this.startAction));
		Assertions.assertNotNull(retrieve.updatedAt);
		// Assertions.assertTrue(retrieve.updatedAt.after(this.startAction));
		Assertions.assertEquals(retrieve.createdAt, retrieve.updatedAt);
		Assertions.assertNotNull(retrieve.deleted);
		Assertions.assertEquals(false, retrieve.deleted);
	}

	@Order(3)
	@Test
	public void testUpdateData() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		// Delete the entry:
		final SimpleTableSoftDelete test = new SimpleTableSoftDelete();
		test.data = TestSimpleTableSoftDelete.DATA_INJECTED_2;
		ConfigureDb.da.update(test, TestSimpleTableSoftDelete.idOfTheObject, List.of("data"));
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, QueryOptions.ACCESS_DELETED_ITEMS,
				QueryOptions.READ_ALL_COLOMN);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.createdAt);
		Assertions.assertNotNull(retrieve.updatedAt);
		LOGGER.info("created @ {} updated @ {}", retrieve.createdAt, retrieve.updatedAt);
		Assertions.assertTrue(retrieve.updatedAt.after(retrieve.createdAt));
		Assertions.assertNotNull(retrieve.deleted);
		Assertions.assertEquals(false, retrieve.deleted);
	}

	@Order(4)
	@Test
	public void testSoftDeleteTheObject() throws Exception {
		if ("sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
			Thread.sleep(Duration.ofMillis(1100));
		} else {
			Thread.sleep(Duration.ofMillis(15));
		}
		// Delete the entry:
		ConfigureDb.da.delete(SimpleTableSoftDelete.class, TestSimpleTableSoftDelete.idOfTheObject);
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject);
		Assertions.assertNull(retrieve);
	}

	@Order(5)
	@Test
	public void testReadDeletedObject() throws Exception {

		// check if we set get deleted element
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, QueryOptions.ACCESS_DELETED_ITEMS);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNull(retrieve.createdAt);
		Assertions.assertNull(retrieve.updatedAt);
		Assertions.assertNull(retrieve.deleted);

	}

	@Order(6)
	@Test
	public void testReadAllValuesUnreadableOfDeletedObject() throws Exception {
		// check if we set get deleted element with all data
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.get(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, QueryOptions.ACCESS_DELETED_ITEMS,
				QueryOptions.READ_ALL_COLOMN);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.createdAt);
		Assertions.assertNotNull(retrieve.updatedAt);
		LOGGER.info("created @ {} updated @ {}", retrieve.createdAt, retrieve.updatedAt);
		Assertions.assertTrue(retrieve.updatedAt.after(retrieve.createdAt));
		Assertions.assertNotNull(retrieve.deleted);
		Assertions.assertEquals(true, retrieve.deleted);

	}
}
