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
import test.atriasoft.archidata.dataStreamEvent.model.TestChangeStreamEntity;

/**
 * Tests for basic CRUD operations (INSERT, UPDATE, DELETE) with change stream events.
 */
public class BasicCRUDOperationsTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(BasicCRUDOperationsTest.class);

	private static ChangeNotificationManager manager;
	private final List<ChangeEvent> capturedEvents = new ArrayList<>();

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
		this.capturedEvents.clear();
		manager.clearAllListeners();
	}

	private void captureEvent(final ChangeEvent event) {
		synchronized (this.capturedEvents) {
			this.capturedEvents.add(event);
			LOGGER.info("Captured event: {}", event);
		}
	}

	@Test
	public void testInsertOperation() throws Exception {
		LOGGER.info("=== TEST: INSERT Operation ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "admin", 100);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1, "Should have at least 1 INSERT event");
			final ChangeEvent event = this.capturedEvents.stream().filter(ChangeEvent::isInsert).findFirst()
					.orElseThrow();
			Assertions.assertTrue(event.isInsert(), "Should be INSERT operation");
			Assertions.assertEquals("TestChangeStreamEntity", event.getCollectionName());
			Assertions.assertNotNull(event.getOid());
			Assertions.assertTrue(event.hasFullDocument(), "INSERT should have full document");
			Assertions.assertEquals("Alice", event.getFullDocument().getString("name"));
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);
		LOGGER.info("=== INSERT Operation test completed ===");
	}

	@Test
	public void testUpdateOperation() throws Exception {
		LOGGER.info("=== TEST: UPDATE Operation ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Bob", "user", 50);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		this.capturedEvents.clear();

		inserted.name = "Bob Updated";
		inserted.value = 200;
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1, "Should have at least 1 UPDATE event");
			final ChangeEvent event = this.capturedEvents.stream().filter(ChangeEvent::isUpdate).findFirst()
					.orElseThrow();
			Assertions.assertTrue(event.isUpdate(), "Should be UPDATE operation");
			Assertions.assertNotNull(event.getUpdateDescription(), "UPDATE should have update description");
			// With UPDATE_LOOKUP mode, UPDATE events DO have full documents
			Assertions.assertTrue(event.hasFullDocument(), "UPDATE_LOOKUP mode provides full document on UPDATE");
			Assertions.assertEquals("Bob Updated", event.getFullDocument().getString("name"));
			Assertions.assertTrue(event.getUpdatedFields().contains("name"), "Should track 'name' field update");
			Assertions.assertTrue(event.getUpdatedFields().contains("value"), "Should track 'value' field update");
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);
		LOGGER.info("=== UPDATE Operation test completed ===");
	}

	@Test
	public void testDeleteOperation() throws Exception {
		LOGGER.info("=== TEST: DELETE Operation ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Charlie", "admin", 75);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		this.capturedEvents.clear();

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1, "Should have at least 1 DELETE event");
			final ChangeEvent event = this.capturedEvents.stream().filter(ChangeEvent::isDelete).findFirst()
					.orElseThrow();
			Assertions.assertTrue(event.isDelete(), "Should be DELETE operation");
			Assertions.assertEquals("TestChangeStreamEntity", event.getCollectionName());
			Assertions.assertNotNull(event.getOid());
			Assertions.assertFalse(event.hasFullDocument(), "DELETE should not have full document");
		}

		LOGGER.info("=== DELETE Operation test completed ===");
	}
}
