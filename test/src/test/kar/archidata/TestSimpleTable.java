package test.kar.archidata;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.sqlWrapper.QuerryOptions;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.model.SimpleTable;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSimpleTable {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestSimpleTable.class);
	private static final String DATA_INJECTED = "kjhlkjhlkjghlmkkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLkjhlkjhlkjghlmkqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFLqsmlfuqùaẑjfQZLSKNEFDÙQMSLDKFJQÙLSNEKRFÙZQOSEdinkqùsldkfnqÙSDKFQJÙMSDKFL";
	private static final String DATA_INJECTED_2 = "dsqfsdfqsdfsqdf";
	private static Long idOfTheObject = null;
	private static Timestamp startAction = null;
	
	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigBaseVariable.dbType = "sqlite";
		ConfigBaseVariable.dbHost = "memory";
		// for test we need to connect all time the DB
		ConfigBaseVariable.dbKeepConnected = "true";
		
		// Clear the static test:
		idOfTheObject = null;
		startAction = null;
		
		// Connect the dataBase...
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
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
	public void testTableInsertAndRetrieve() throws Exception {
		TestSimpleTable.startAction = Timestamp.from(Instant.now());
		List<String> sqlCommand = SqlWrapper.createTable(SimpleTable.class);
		for (String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			SqlWrapper.executeSimpleQuerry(elem, false);
		}
		SimpleTable test = new SimpleTable();
		test.data = TestSimpleTable.DATA_INJECTED;
		SimpleTable insertedData = SqlWrapper.insert(test);
		
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		
		// Try to retrieve all the data:
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, insertedData.id);
		
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
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, TestSimpleTable.idOfTheObject, new QuerryOptions(QuerryOptions.SQL_NOT_READ_DISABLE, true));
		
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
		if ("sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
			Thread.sleep(Duration.ofMillis(1100));
		} else {
			Thread.sleep(Duration.ofMillis(15));
		}
		
		// Delete the entry:
		SimpleTable test = new SimpleTable();
		test.data = TestSimpleTable.DATA_INJECTED_2;
		SqlWrapper.update(test, TestSimpleTable.idOfTheObject, List.of("data"));
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, TestSimpleTable.idOfTheObject, new QuerryOptions(QuerryOptions.SQL_NOT_READ_DISABLE, true));
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
		SqlWrapper.delete(SimpleTable.class, TestSimpleTable.idOfTheObject);
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, TestSimpleTable.idOfTheObject);
		Assertions.assertNull(retrieve);
	}
	
	@Order(5)
	@Test
	public void testReadDeletedObject() throws Exception {
		
		// check if we set get deleted element
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, TestSimpleTable.idOfTheObject, new QuerryOptions(QuerryOptions.SQL_DELETED_DISABLE, true));
		Assertions.assertNull(retrieve);
		
	}
	
	@Order(6)
	@Test
	public void testReadAllValuesUnreadableOfDeletedObject() throws Exception {
		// check if we set get deleted element with all data
		SimpleTable retrieve = SqlWrapper.get(SimpleTable.class, TestSimpleTable.idOfTheObject, new QuerryOptions(QuerryOptions.SQL_DELETED_DISABLE, true, QuerryOptions.SQL_NOT_READ_DISABLE, true));
		Assertions.assertNull(retrieve);
		
	}
}
