package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
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
public class TestSimpleTable {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestSimpleTable.class);
	private static final String DATA_INJECTED = "kjhlkjhlkjghlmkkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFL";
	private static final String DATA_INJECTED_2 = "dsqfsdfqsdfsqdf";
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
		TestSimpleTable.startAction = Timestamp.from(Instant.now());
		final SimpleTable test = new SimpleTable();
		test.data = TestSimpleTable.DATA_INJECTED;
		final SimpleTable insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		// Try to retrieve all the data:
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED, retrieve.data);
		Assertions.assertNull(retrieve.createdAt);
		Assertions.assertNull(retrieve.updatedAt);
		TestSimpleTable.idOfTheObject = retrieve.id;
	}

	@Order(2)
	@Test
	public void testReadAllValuesUnreadable() throws Exception {
		// check the full values
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				QueryOptions.READ_ALL_COLOMN);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTable.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED, retrieve.data);
		Assertions.assertNotNull(retrieve.createdAt);
		LOGGER.info("start @ {} create @ {}", retrieve.createdAt.toInstant(), TestSimpleTable.startAction.toInstant());
		// Gros travail sur les timestamp a faire pour que ce soit correct ...
		// Assertions.assertTrue(retrieve.createdAt.after(this.startAction));
		Assertions.assertNotNull(retrieve.updatedAt);
		// Assertions.assertTrue(retrieve.updatedAt.after(this.startAction));
		Assertions.assertEquals(retrieve.createdAt, retrieve.updatedAt);
	}

	@Order(3)
	@Test
	public void testUpdateData() throws Exception {
		Thread.sleep(Duration.ofMillis(15));
		// Delete the entry:
		final SimpleTable test = new SimpleTable();
		test.data = TestSimpleTable.DATA_INJECTED_2;
		ConfigureDb.da.updateById(test, TestSimpleTable.idOfTheObject, new FilterValue("data"));
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				QueryOptions.READ_ALL_COLOMN);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(TestSimpleTable.idOfTheObject, retrieve.id);
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.createdAt);
		Assertions.assertNotNull(retrieve.updatedAt);
		LOGGER.info("created @ {} updated @ {}", retrieve.createdAt, retrieve.updatedAt);
		Assertions.assertTrue(retrieve.updatedAt.after(retrieve.createdAt));
	}

	@Order(4)
	@Test
	public void testDeleteTheObject() throws Exception {
		// Delete the entry:
		ConfigureDb.da.deleteById(SimpleTable.class, TestSimpleTable.idOfTheObject);
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject);
		Assertions.assertNull(retrieve);
	}

	@Order(5)
	@Test
	public void testReadDeletedObject() throws Exception {

		// check if we set get deleted element
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				QueryOptions.ACCESS_DELETED_ITEMS);
		Assertions.assertNull(retrieve);

	}

	@Order(6)
	@Test
	public void testReadAllValuesUnreadableOfDeletedObject() throws Exception {
		// check if we set get deleted element with all data
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				QueryOptions.ACCESS_DELETED_ITEMS, QueryOptions.READ_ALL_COLOMN);
		Assertions.assertNull(retrieve);

	}
}
