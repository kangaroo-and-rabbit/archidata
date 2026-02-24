package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSimpleTableSoftDelete.class);
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
		final SimpleTableSoftDelete test = new SimpleTableSoftDelete();
		test.data = TestSimpleTableSoftDelete.DATA_INJECTED;
		final SimpleTableSoftDelete insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.getId());
		Assertions.assertTrue(insertedData.getId() >= 0);

		// Try to retrieve all the data:
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				insertedData.getId());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(insertedData.getId(), retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED, retrieve.data);
		Assertions.assertNull(retrieve.getCreatedAt());
		Assertions.assertNull(retrieve.getUpdatedAt());
		Assertions.assertNull(retrieve.getDeleted());
		TestSimpleTableSoftDelete.idOfTheObject = retrieve.getId();
	}

	@Order(2)
	@Test
	public void testReadAllValuesUnreadable() throws Exception {
		// check the full values
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED, retrieve.data);
		Assertions.assertNotNull(retrieve.getCreatedAt());
		LOGGER.info("start @ {} create @ {}", retrieve.getCreatedAt().toInstant(),
				TestSimpleTableSoftDelete.startAction.toInstant());
		// Gros travail sur les timestamp a faire pour que ce soit correct ...
		// Assertions.assertTrue(retrieve.getCreatedAt().after(this.startAction));
		Assertions.assertNotNull(retrieve.getUpdatedAt());
		// Assertions.assertTrue(retrieve.getUpdatedAt().after(this.startAction));
		Assertions.assertEquals(retrieve.getCreatedAt(), retrieve.getUpdatedAt());
		Assertions.assertNotNull(retrieve.getDeleted());
		Assertions.assertEquals(false, retrieve.getDeleted());
	}

	@Order(3)
	@Test
	public void testUpdateData() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		// Delete the entry:
		final SimpleTableSoftDelete test = new SimpleTableSoftDelete();
		test.data = TestSimpleTableSoftDelete.DATA_INJECTED_2;
		ConfigureDb.da.updateById(test, TestSimpleTableSoftDelete.idOfTheObject, new FilterValue("data"));
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.getCreatedAt());
		Assertions.assertNotNull(retrieve.getUpdatedAt());
		LOGGER.info("created @ {} updated @ {}", retrieve.getCreatedAt(), retrieve.getUpdatedAt());
		Assertions.assertTrue(retrieve.getUpdatedAt().after(retrieve.getCreatedAt()));
		Assertions.assertNotNull(retrieve.getDeleted());
		Assertions.assertEquals(false, retrieve.getDeleted());
	}

	@Order(4)
	@Test
	public void testSoftDeleteTheObject() throws Exception {
		Thread.sleep(Duration.ofMillis(15));
		// Delete the entry:
		ConfigureDb.da.deleteById(SimpleTableSoftDelete.class, TestSimpleTableSoftDelete.idOfTheObject);
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject);
		Assertions.assertNull(retrieve);
	}

	@Order(5)
	@Test
	public void testReadDeletedObject() throws Exception {

		// check if we set get deleted element
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, new AccessDeletedItems());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNull(retrieve.getCreatedAt());
		Assertions.assertNull(retrieve.getUpdatedAt());
		Assertions.assertNull(retrieve.getDeleted());

	}

	@Order(6)
	@Test
	public void testReadAllValuesUnreadableOfDeletedObject() throws Exception {
		// check if we set get deleted element with all data
		final SimpleTableSoftDelete retrieve = ConfigureDb.da.getById(SimpleTableSoftDelete.class,
				TestSimpleTableSoftDelete.idOfTheObject, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTableSoftDelete.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.getCreatedAt());
		Assertions.assertNotNull(retrieve.getUpdatedAt());
		LOGGER.info("created @ {} updated @ {}", retrieve.getCreatedAt(), retrieve.getUpdatedAt());
		Assertions.assertTrue(retrieve.getUpdatedAt().after(retrieve.getCreatedAt()));
		Assertions.assertNotNull(retrieve.getDeleted());
		Assertions.assertEquals(true, retrieve.getDeleted());

	}
}
