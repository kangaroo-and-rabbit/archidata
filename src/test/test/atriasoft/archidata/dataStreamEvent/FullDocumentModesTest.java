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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.changestream.FullDocument;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataStreamEvent.model.TestChangeStreamEntity;

/**
 * Tests for FullDocument DEFAULT mode behavior and automatic mode computation.
 */
public class FullDocumentModesTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(FullDocumentModesTest.class);

	private static ChangeNotificationManager manager;
	private final List<ChangeEvent> capturedEvents = new ArrayList<>();

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		manager = ChangeNotificationManager.getInstance();
		// Start with DEFAULT mode for these specific tests
		manager.start(ConfigureDb.da.getInterface().getDatabase(),
				com.mongodb.client.model.changestream.FullDocument.DEFAULT);
		LOGGER.info("Change Notification Manager started with DEFAULT mode");
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
	@Order(10)
	public void testDefaultMode() throws Exception {
		LOGGER.info("=== TEST: DEFAULT Mode ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").register();
		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "user", 50);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			final long insertCount = this.capturedEvents.stream().filter(ChangeEvent::isInsert).count();
			Assertions.assertTrue(insertCount >= 1, "Should have at least 1 INSERT event");
			final ChangeEvent insertEvent = this.capturedEvents.stream().filter(ChangeEvent::isInsert).findFirst()
					.orElseThrow();
			Assertions.assertTrue(insertEvent.hasFullDocument(), "DEFAULT mode should have full document on INSERT");
		}

		this.capturedEvents.clear();
		inserted.name = "Alice Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1);
			final ChangeEvent updateEvent = this.capturedEvents.get(0);
			Assertions.assertNotNull(updateEvent.getUpdateDescription(),
					"UPDATE should have update description even in DEFAULT mode");
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);
		LOGGER.info("=== DEFAULT Mode test completed ===");
	}

	@Test
	@Order(20)
	public void testDefaultModeUpdateBehavior() throws Exception {
		LOGGER.info("=== TEST: DEFAULT Mode UPDATE Behavior ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").register();
		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Bob", "admin", 75);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.get(0).hasFullDocument(),
					"DEFAULT mode should have full document on INSERT");
		}

		this.capturedEvents.clear();
		inserted.name = "Bob Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1);
			final ChangeEvent updateEvent = this.capturedEvents.stream().filter(ChangeEvent::isUpdate).findFirst()
					.orElseThrow();
			// With DEFAULT mode, UPDATE events do NOT have full documents
			Assertions.assertFalse(updateEvent.hasFullDocument(),
					"DEFAULT mode should NOT have full document on UPDATE");
			Assertions.assertNotNull(updateEvent.getUpdateDescription(),
					"UPDATE should have update description with changed fields");
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);
		LOGGER.info("=== DEFAULT Mode UPDATE Behavior test completed ===");
	}

	@org.junit.jupiter.api.Disabled("Flaky when run with other tests due to worker restart timing - tested in ChangeNotificationIntegrationTest instead")
	@Test
	@Order(30)
	public void testAutomaticModeComputation() throws Exception {
		LOGGER.info("=== TEST: Automatic Mode Computation ===");

		manager.clearAllListeners();
		// Additional delay to ensure previous test cleanup is complete
		Thread.sleep(500);

		final List<ChangeEvent> listener1Events = new ArrayList<>();
		final List<ChangeEvent> listener2Events = new ArrayList<>();

		manager.createListenerBuilder(event -> {
			synchronized (listener1Events) {
				listener1Events.add(event);
			}
		}, "TestChangeStreamEntity").register();

		manager.createListenerBuilder(event -> {
			synchronized (listener2Events) {
				listener2Events.add(event);
			}
		}, "TestChangeStreamEntity").register();

		final var effectiveMode = manager.computeEffectiveMode("TestChangeStreamEntity");
		Assertions.assertEquals(FullDocument.DEFAULT, effectiveMode,
				"Effective mode should be DEFAULT when multiple listeners with DEFAULT mode are registered");

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);
		// Additional delay to ensure the worker is fully ready after mode change
		Thread.sleep(500);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Charlie", "user", 60);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForCondition(() -> {
			synchronized (listener1Events) {
				synchronized (listener2Events) {
					return listener1Events.size() >= 1 && listener2Events.size() >= 1;
				}
			}
		}, 5000, "Both listeners should receive INSERT event");

		inserted.name = "Charlie Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (listener1Events) {
				synchronized (listener2Events) {
					return listener1Events.size() >= 2 && listener2Events.size() >= 2;
				}
			}
		}, 5000, "Both listeners should receive UPDATE event");

		synchronized (listener1Events) {
			synchronized (listener2Events) {
				Assertions.assertTrue(listener1Events.size() >= 2, "Listener 1 should receive events");
				Assertions.assertTrue(listener2Events.size() >= 2, "Listener 2 should receive events");

				final java.util.Optional<ChangeEvent> updateEventOpt = listener1Events.stream()
						.filter(ChangeEvent::isUpdate).findFirst();
				Assertions.assertTrue(updateEventOpt.isPresent(), "Should have at least one UPDATE event");
				final ChangeEvent updateEvent = updateEventOpt.get();
				Assertions.assertFalse(updateEvent.hasFullDocument(),
						"DEFAULT mode listeners do not get full document on UPDATE operations");
			}
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);
		LOGGER.info("=== Automatic Mode Computation test completed ===");
	}
}
