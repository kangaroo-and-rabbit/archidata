package test.atriasoft.archidata.dataStreamEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataStreamEvent.ChangeEvent;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.changestream.FullDocument;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.SimpleTable;
import test.atriasoft.archidata.dataStreamEvent.model.TestChangeStreamEntity;

/**
 * Tests for multi-collection observation and global listeners.
 */
public class MultiCollectionTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiCollectionTest.class);

	private static ChangeNotificationManager manager;

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		manager = ChangeNotificationManager.getInstance();
		manager.start(ConfigureDb.da.getInterface().getDatabase(),
				com.mongodb.client.model.changestream.FullDocument.UPDATE_LOOKUP);
		LOGGER.info("Change Notification Manager started with UPDATE_LOOKUP mode");
	}

	@AfterAll
	public static void cleanupDatabase() throws IOException {
		if (manager != null && manager.isRunning()) {
			manager.stop();
		}
		ConfigureDb.clear();
	}

	@AfterEach
	public void cleanupAfterTest() {
		manager.clearAllListeners();
	}

	@Test
	public void testMultiCollectionObservation() throws Exception {
		LOGGER.info("=== TEST: Multi-Collection Observation ===");

		final List<ChangeEvent> collection1Events = new ArrayList<>();
		final List<ChangeEvent> collection2Events = new ArrayList<>();
		final List<ChangeEvent> globalEvents = new ArrayList<>();

		manager.createListenerBuilder(event -> {
			synchronized (collection1Events) {
				collection1Events.add(event);
			}
		}, "TestChangeStreamEntity").register();

		manager.createListenerBuilder(event -> {
			synchronized (collection2Events) {
				collection2Events.add(event);
			}
		}, "SimpleTable").register();

		manager.createListenerBuilder(event -> {
			synchronized (globalEvents) {
				globalEvents.add(event);
			}
		}).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);
		TestHelper.waitForStreamInitialization(manager, "SimpleTable", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "user", 25);
		ConfigureDb.da.insert(entity);

		final SimpleTable simple = new SimpleTable();
		simple.data = "test_data";
		ConfigureDb.da.insert(simple);

		TestHelper.waitForEvents(collection1Events, 1, 5000);
		TestHelper.waitForEvents(collection2Events, 1, 5000);
		TestHelper.waitForEvents(globalEvents, 2, 5000);

		synchronized (collection1Events) {
			Assertions.assertTrue(collection1Events.size() >= 1,
					"TestChangeStreamEntity listener should receive events");
			Assertions.assertEquals("TestChangeStreamEntity", collection1Events.get(0).getCollectionName());
		}

		synchronized (collection2Events) {
			Assertions.assertTrue(collection2Events.size() >= 1, "SimpleTable listener should receive events");
			Assertions.assertEquals("SimpleTable", collection2Events.get(0).getCollectionName());
		}

		synchronized (globalEvents) {
			Assertions.assertTrue(globalEvents.size() >= 2,
					"Global listener should receive events from both collections");
			final long testChangeStreamCount = globalEvents.stream()
					.filter(e -> "TestChangeStreamEntity".equals(e.getCollectionName())).count();
			final long simpleTableCount = globalEvents.stream().filter(e -> "SimpleTable".equals(e.getCollectionName()))
					.count();
			Assertions.assertTrue(testChangeStreamCount >= 1,
					"Global listener should receive TestChangeStreamEntity events");
			Assertions.assertTrue(simpleTableCount >= 1, "Global listener should receive SimpleTable events");
		}

		LOGGER.info("=== Multi-Collection Observation test completed ===");
	}
}
