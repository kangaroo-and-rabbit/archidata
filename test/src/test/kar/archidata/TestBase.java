package test.kar.archidata;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.util.RESTApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBase {
	final static Logger logger = LoggerFactory.getLogger(TestBase.class);

	static RESTApi api = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		logger.info("Create DB");
		final String dbName = "sdfsdfsdfsfsdfsfsfsfsdfsdfsd";
		boolean data = SqlWrapper.isDBExist(dbName);
		logger.error("exist: {}", data);
		data = SqlWrapper.createDB(dbName);
		logger.error("create: {}", data);
		data = SqlWrapper.isDBExist(dbName);
		logger.error("exist: {}", data);
	}
	
	@AfterAll
	public static void stopWebServer() throws InterruptedException {
		logger.info("Kill the web server");
		// TODO: do it better...
	}

	@Order(1)
	@Test
	public void checkSimpleTestError() throws Exception {
		Assertions.assertEquals("lkjlkjlkjlk", "alive and kicking");
	}
}
