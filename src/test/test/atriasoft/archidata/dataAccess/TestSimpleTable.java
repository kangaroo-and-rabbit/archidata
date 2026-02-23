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
import test.atriasoft.archidata.dataAccess.model.SimpleTable;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSimpleTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSimpleTable.class);
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
		Assertions.assertNotNull(insertedData.getId());
		Assertions.assertTrue(insertedData.getId() >= 0);

		// Try to retrieve all the data:
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, insertedData.getId());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(insertedData.getId(), retrieve.getId());
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED, retrieve.data);
		Assertions.assertNull(retrieve.getCreatedAt());
		Assertions.assertNull(retrieve.getUpdatedAt());
		TestSimpleTable.idOfTheObject = retrieve.getId();
	}

	@Order(2)
	@Test
	public void testReadAllValuesUnreadable() throws Exception {
		// check the full values
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				new ReadAllColumn());

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTable.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED, retrieve.data);
		Assertions.assertNotNull(retrieve.getCreatedAt());
		LOGGER.info("start @ {} create @ {}", retrieve.getCreatedAt().toInstant(), TestSimpleTable.startAction.toInstant());
		// Gros travail sur les timestamp a faire pour que ce soit correct ...
		// Assertions.assertTrue(retrieve.getCreatedAt().after(this.startAction));
		Assertions.assertNotNull(retrieve.getUpdatedAt());
		// Assertions.assertTrue(retrieve.getUpdatedAt().after(this.startAction));
		// Check timestamps are equal within 1 second tolerance (to handle precision differences)
		final long timeDiff = Math.abs(retrieve.getCreatedAt().getTime() - retrieve.getUpdatedAt().getTime());
		Assertions.assertTrue(timeDiff < 1000, "createdAt and updatedAt should be equal (within 1s tolerance)");
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
				new ReadAllColumn());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.getId());
		Assertions.assertEquals(TestSimpleTable.idOfTheObject, retrieve.getId());
		Assertions.assertEquals(TestSimpleTable.DATA_INJECTED_2, retrieve.data);
		Assertions.assertNotNull(retrieve.getCreatedAt());
		Assertions.assertNotNull(retrieve.getUpdatedAt());
		LOGGER.info("created @ {} updated @ {}", retrieve.getCreatedAt(), retrieve.getUpdatedAt());
		Assertions.assertTrue(retrieve.getUpdatedAt().after(retrieve.getCreatedAt()));
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
				new AccessDeletedItems());
		Assertions.assertNull(retrieve);

	}

	@Order(6)
	@Test
	public void testReadAllValuesUnreadableOfDeletedObject() throws Exception {
		// check if we set get deleted element with all data
		final SimpleTable retrieve = ConfigureDb.da.getById(SimpleTable.class, TestSimpleTable.idOfTheObject,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(retrieve);

	}
}
