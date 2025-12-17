package test.atriasoft.archidata.dataStreamEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.atriasoft.archidata.dataStreamEvent.ChangeEvent;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationListener;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.SimpleTable;
import test.atriasoft.archidata.dataStreamEvent.model.TestChangeStreamEntity;

/**
 * Comprehensive integration tests for the MongoDB Change Notification System.
 * Tests cover all aspects: CRUD operations, filtering modes, field-based filtering,
 * auto-disconnect, multi-collection observation, and field-specific inspection.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ChangeNotificationIntegrationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeNotificationIntegrationTest.class);

	private static ChangeNotificationManager manager;
	private final List<ChangeEvent> capturedEvents = new ArrayList<>();
	private CountDownLatch eventLatch;

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		manager = ChangeNotificationManager.getInstance();
		// Start with UPDATE_LOOKUP mode for tests that need full documents
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

	/**
	 * Helper method to wait for expected number of events
	 */
	private boolean waitForEvents(final int expectedCount, final int timeoutSeconds) throws InterruptedException {
		this.eventLatch = new CountDownLatch(expectedCount);
		return this.eventLatch.await(timeoutSeconds, TimeUnit.SECONDS);
	}

	/**
	 * Listener that captures events in a thread-safe list
	 */
	private void captureEvent(final ChangeEvent event) {
		synchronized (this.capturedEvents) {
			this.capturedEvents.add(event);
			LOGGER.info("Captured event: {}", event);
		}
		if (this.eventLatch != null) {
			this.eventLatch.countDown();
		}
	}

	// ============================================================================
	// SECTION 1: Basic CRUD Operations with Event Capture
	// ============================================================================

	@Order(1)
	@Test
	public void testBasicCRUDOperations() throws Exception {
		LOGGER.info("=== TEST: Basic CRUD Operations ===");

		// Register listener for the test entity collection
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.register();

		// Wait for the change stream to initialize
		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		// INSERT operation
		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "admin", 100);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);

		// Wait for INSERT event
		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should have 1 INSERT event");
			final ChangeEvent insertEvent = this.capturedEvents.get(0);
			Assertions.assertTrue(insertEvent.isInsert(), "Should be INSERT operation");
			Assertions.assertEquals("TestChangeStreamEntity", insertEvent.getCollectionName());
			Assertions.assertNotNull(insertEvent.getOid());
			Assertions.assertTrue(insertEvent.hasFullDocument(), "INSERT should have full document");
			Assertions.assertEquals("Alice", insertEvent.getFullDocument().getString("name"));
		}

		// Clear events for next operation
		this.capturedEvents.clear();

		// UPDATE operation
		inserted.name = "Alice Updated";
		inserted.value = 200;
		ConfigureDb.da.updateById(inserted, inserted.id);

		// Wait for UPDATE event
		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should have 1 UPDATE event");
			final ChangeEvent updateEvent = this.capturedEvents.get(0);
			Assertions.assertTrue(updateEvent.isUpdate(), "Should be UPDATE operation");
			Assertions.assertEquals("TestChangeStreamEntity", updateEvent.getCollectionName());
			Assertions.assertNotNull(updateEvent.getUpdateDescription(), "UPDATE should have update description");
			Assertions.assertTrue(updateEvent.hasFullDocument(), "UPDATE_LOOKUP should provide full document");
			Assertions.assertEquals("Alice Updated", updateEvent.getFullDocument().getString("name"));
			Assertions.assertTrue(updateEvent.getUpdatedFields().contains("name"), "Should track 'name' field update");
			Assertions.assertTrue(updateEvent.getUpdatedFields().contains("value"),
					"Should track 'value' field update");
		}

		// Clear events for next operation
		this.capturedEvents.clear();

		// DELETE operation
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		// Wait for DELETE event
		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should have 1 DELETE event");
			final ChangeEvent deleteEvent = this.capturedEvents.get(0);
			Assertions.assertTrue(deleteEvent.isDelete(), "Should be DELETE operation");
			Assertions.assertEquals("TestChangeStreamEntity", deleteEvent.getCollectionName());
			Assertions.assertNotNull(deleteEvent.getOid());
			Assertions.assertFalse(deleteEvent.hasFullDocument(), "DELETE should not have full document");
		}

		LOGGER.info("=== CRUD Operations test completed successfully ===");
	}

	// ============================================================================
	// SECTION 2: Global FullDocument Mode Testing
	// ============================================================================

	@Order(2)
	@Test
	public void testGlobalFullDocumentMode() throws Exception {
		LOGGER.info("=== TEST: Global FullDocument Mode (UPDATE_LOOKUP) ===");

		// Verify that the global mode (UPDATE_LOOKUP) is applied to all operations
		this.capturedEvents.clear();
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity")
				// Mode should match the global mode set at startup
				.withMode(FullDocument.UPDATE_LOOKUP).register();
		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity1 = new TestChangeStreamEntity("Bob", "user", 50);
		final TestChangeStreamEntity inserted1 = ConfigureDb.da.insert(entity1);

		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			// May receive DELETE from previous test + INSERT, so filter for INSERT only
			final long insertCount = this.capturedEvents.stream().filter(ChangeEvent::isInsert).count();
			Assertions.assertTrue(insertCount >= 1, "Should have at least 1 INSERT event");
			final ChangeEvent insertEvent = this.capturedEvents.stream().filter(ChangeEvent::isInsert).findFirst()
					.orElseThrow();
			Assertions.assertTrue(insertEvent.hasFullDocument(),
					"UPDATE_LOOKUP mode should have full document on INSERT");
			Assertions.assertEquals("Bob", insertEvent.getFullDocument().getString("name"));
		}

		this.capturedEvents.clear();
		inserted1.name = "Bob Updated";
		ConfigureDb.da.updateById(inserted1, inserted1.id);

		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size());
			// UPDATE_LOOKUP mode should have full document on UPDATE
			final ChangeEvent updateEvent = this.capturedEvents.get(0);
			Assertions.assertNotNull(updateEvent.getUpdateDescription(), "UPDATE should have update description");
			Assertions.assertTrue(updateEvent.hasFullDocument(),
					"UPDATE_LOOKUP mode should have full document on UPDATE");
			Assertions.assertEquals("Bob Updated", updateEvent.getFullDocument().getString("name"));
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted1.id);

		// Clear for next test
		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Test 2: UPDATE_LOOKUP mode (always has full document)
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.register();
		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity2 = new TestChangeStreamEntity("Charlie", "admin", 75);
		final TestChangeStreamEntity inserted2 = ConfigureDb.da.insert(entity2);

		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.get(0).hasFullDocument(),
					"UPDATE_LOOKUP should have full document on INSERT");
		}

		this.capturedEvents.clear();
		inserted2.name = "Charlie Updated";
		ConfigureDb.da.updateById(inserted2, inserted2.id);

		waitForEvents(1, 5);
		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size());
			Assertions.assertTrue(this.capturedEvents.get(0).hasFullDocument(),
					"UPDATE_LOOKUP should have full document on UPDATE");
			Assertions.assertEquals("Charlie Updated", this.capturedEvents.get(0).getFullDocument().getString("name"));
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted2.id);

		LOGGER.info("=== FullDocument Modes test completed successfully ===");
	}

	@Order(3)
	@Test
	public void testGlobalModeEnforcement() throws Exception {
		LOGGER.info("=== TEST: Global Mode Enforcement ===");

		manager.clearAllListeners();

		// Clear any events that might be pending
		this.capturedEvents.clear();

		// Register first listener with the global mode (UPDATE_LOOKUP)
		final List<ChangeEvent> listener1Events = new ArrayList<>();

		manager.createListenerBuilder(event -> {
			LOGGER.debug("Listener1 received: {}", event.getOperationType());
			synchronized (listener1Events) {
				listener1Events.add(event);
			}
		}, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP).register();

		// The effective mode should be the global mode (UPDATE_LOOKUP)
		final var effectiveMode = manager.computeEffectiveMode("TestChangeStreamEntity");
		Assertions.assertEquals(FullDocument.UPDATE_LOOKUP, effectiveMode,
				"Effective mode should be the global mode set at startup");

		// Register a second listener with the SAME mode - should work
		final List<ChangeEvent> listener2Events = new ArrayList<>();

		manager.createListenerBuilder(event -> {
			LOGGER.debug("Listener2 received: {}", event.getOperationType());
			synchronized (listener2Events) {
				listener2Events.add(event);
			}
		}, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		// Perform operations to verify both listeners work
		final TestChangeStreamEntity entity = new TestChangeStreamEntity("David", "user", 60);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForCondition(() -> {
			synchronized (listener1Events) {
				synchronized (listener2Events) {
					return !listener1Events.isEmpty() && !listener2Events.isEmpty();
				}
			}
		}, 5000, "Both listeners should receive INSERT event");

		synchronized (listener1Events) {
			synchronized (listener2Events) {
				Assertions.assertEquals(1, listener1Events.size(), "Listener 1 should receive 1 event");
				Assertions.assertEquals(1, listener2Events.size(), "Listener 2 should receive 1 event");
				Assertions.assertTrue(listener1Events.get(0).isInsert());
				Assertions.assertTrue(listener2Events.get(0).isInsert());
			}
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== Fixed Mode Enforcement test completed successfully ===");
	}

	// ============================================================================
	// SECTION 3: Field-Based Filtering
	// ============================================================================

	@Order(4)
	@Test
	public void testFieldBasedFiltering() throws Exception {
		LOGGER.info("=== TEST: Field-Based Filtering ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Register listener that only receives events where role == "admin"
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.filterField("role", "admin").register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		// Insert admin user - should be captured
		final TestChangeStreamEntity admin = new TestChangeStreamEntity("AdminUser", "admin", 100);
		final TestChangeStreamEntity insertedAdmin = ConfigureDb.da.insert(admin);

		// Insert regular user - should NOT be captured
		final TestChangeStreamEntity user = new TestChangeStreamEntity("RegularUser", "user", 50);
		final TestChangeStreamEntity insertedUser = ConfigureDb.da.insert(user);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			// Filter for INSERT events with admin role
			final long adminInsertCount = this.capturedEvents.stream().filter(
					e -> e.isInsert() && e.hasFullDocument() && "admin".equals(e.getFullDocument().getString("role")))
					.count();
			Assertions.assertTrue(adminInsertCount >= 1, "Should capture at least 1 admin INSERT event");
			final ChangeEvent adminEvent = this.capturedEvents.stream().filter(
					e -> e.isInsert() && e.hasFullDocument() && "admin".equals(e.getFullDocument().getString("role")))
					.findFirst().orElseThrow();
			Assertions.assertEquals("AdminUser", adminEvent.getFullDocument().getString("name"));
			Assertions.assertEquals("admin", adminEvent.getFullDocument().getString("role"));
		}

		this.capturedEvents.clear();

		// Update admin - should be captured
		insertedAdmin.value = 150;
		ConfigureDb.da.updateById(insertedAdmin, insertedAdmin.id);

		// Update user - should NOT be captured
		insertedUser.value = 75;
		ConfigureDb.da.updateById(insertedUser, insertedUser.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should only capture admin update");
			Assertions.assertTrue(this.capturedEvents.get(0).isUpdate());
		}

		// Cleanup
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedAdmin.id);
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedUser.id);

		LOGGER.info("=== Field-Based Filtering test completed successfully ===");
	}

	@Order(5)
	@Test
	public void testMultipleFieldChanges() throws Exception {
		LOGGER.info("=== TEST: Multiple Field Changes ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Filter on specific field value
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.filter(event -> event.getFullDocument() != null
						&& event.getFullDocument().getInteger("value", 0) > 100)
				.register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Eve", "user", 50);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture initial insert with value=50");

		this.capturedEvents.clear();

		// Update value to 150 - should be captured
		inserted.value = 150;
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture update with value=150");
			Assertions.assertEquals(150, this.capturedEvents.get(0).getFullDocument().getInteger("value"));
		}

		this.capturedEvents.clear();

		// Update value to 80 - should NOT be captured
		inserted.value = 80;
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture update with value=80");

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== Multiple Field Changes test completed successfully ===");
	}

	// ============================================================================
	// SECTION 4: Auto-Disconnect Verification
	// ============================================================================

	@Order(6)
	@Test
	public void testAutoDisconnect() throws Exception {
		LOGGER.info("=== TEST: Auto-Disconnect ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Register a listener
		final ChangeNotificationListener listener = this::captureEvent;
		manager.createListenerBuilder(listener, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		// Insert and verify event is captured
		final TestChangeStreamEntity entity1 = new TestChangeStreamEntity("Frank", "user", 30);
		ConfigureDb.da.insert(entity1);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1, "Listener should receive event");
		}

		this.capturedEvents.clear();

		// Unregister the listener
		manager.unregisterListener(listener);

		// Verify the change stream worker is still running (no auto-disconnect)
		Assertions.assertTrue(manager.isWatching("TestChangeStreamEntity"),
				"Collection should still be watched after listener removal");

		// Insert another entity
		final TestChangeStreamEntity entity2 = new TestChangeStreamEntity("Grace", "admin", 40);
		ConfigureDb.da.insert(entity2);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Removed listener should not receive events");

		// Verify collection is still being monitored
		Assertions.assertTrue(manager.isWatching("TestChangeStreamEntity"),
				"Collection monitoring should persist even with no listeners");

		LOGGER.info("=== Auto-Disconnect test completed successfully ===");
	}

	// ============================================================================
	// SECTION 5: Multi-Collection Observation
	// ============================================================================

	@Order(7)
	@Test
	public void testMultiCollectionObservation() throws Exception {
		LOGGER.info("=== TEST: Multi-Collection Observation ===");

		manager.clearAllListeners();

		final List<ChangeEvent> collection1Events = new ArrayList<>();
		final List<ChangeEvent> collection2Events = new ArrayList<>();
		final List<ChangeEvent> globalEvents = new ArrayList<>();

		// Register listener for TestChangeStreamEntity
		manager.createListenerBuilder(event -> {
			LOGGER.info("Retrieve (1) data oid={}", event.getOid());
			synchronized (collection1Events) {
				collection1Events.add(event);
			}
		}, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP).register();

		// Register listener for SimpleTable
		manager.createListenerBuilder(event -> {
			LOGGER.info("Retrieve (2) data oid={}", event.getOid());
			synchronized (collection2Events) {
				collection2Events.add(event);
			}
		}, "SimpleTable").withMode(FullDocument.UPDATE_LOOKUP).register();

		// Register global listener (receives events from all collections)
		manager.createListenerBuilder(event -> {
			LOGGER.info("Retrieve (3) data oid={}", event.getOid());
			synchronized (globalEvents) {
				globalEvents.add(event);
			}
		}).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);
		TestHelper.waitForStreamInitialization(manager, "SimpleTable", 2000);

		// Insert into TestChangeStreamEntity
		final TestChangeStreamEntity entity1 = new TestChangeStreamEntity("Henry", "user", 25);
		ConfigureDb.da.insert(entity1);

		// Insert into SimpleTable
		final SimpleTable simple = new SimpleTable();
		simple.data = "test_data";
		ConfigureDb.da.insert(simple);

		TestHelper.waitForEvents(collection1Events, 1, 5000);
		TestHelper.waitForEvents(collection2Events, 1, 5000);
		TestHelper.waitForEvents(globalEvents, 2, 5000);

		// Verify collection-specific listeners
		synchronized (collection1Events) {
			Assertions.assertTrue(collection1Events.size() >= 1,
					"TestChangeStreamEntity listener should receive events");
			Assertions.assertEquals("TestChangeStreamEntity", collection1Events.get(0).getCollectionName());
		}

		synchronized (collection2Events) {
			Assertions.assertTrue(collection2Events.size() >= 1, "SimpleTable listener should receive events");
			Assertions.assertEquals("SimpleTable", collection2Events.get(0).getCollectionName());
		}

		// Verify global listener receives events from both collections
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

		LOGGER.info("=== Multi-Collection Observation test completed successfully ===");
	}

	// ============================================================================
	// SECTION 6: Field-Specific Inspection
	// ============================================================================

	@Order(8)
	@Test
	public void testFieldSpecificInspection() throws Exception {
		LOGGER.info("=== TEST: Field-Specific Inspection ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Register listener that only cares about 'name' field updates
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.filter(event -> {
					LOGGER.info("Retrieve (1) data oid={} {} field={}", event.getOid(), event.getOperationType(),
							event.getUpdatedFields());
					if (!event.isUpdate()) {
						return true; // Accept non-update events
					}
					return event.getUpdatedFields().contains("name");
				}).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Iris", "user", 90);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		waitForEvents(1, 5);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture INSERT");
		}

		this.capturedEvents.clear();

		// Update 'name' field - should be captured
		inserted.name = "Iris Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		waitForEvents(1, 5);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture name field update");
			Assertions.assertTrue(this.capturedEvents.get(0).getUpdatedFields().contains("name"));
		}

		this.capturedEvents.clear();

		// Update only 'value' field - should NOT be captured
		inserted.value = 120;
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture value-only update");

		this.capturedEvents.clear();

		// Update both 'name' and 'value' - should be captured
		inserted.name = "Iris Final";
		inserted.value = 150;
		ConfigureDb.da.updateById(inserted, inserted.id);

		waitForEvents(1, 5);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture update containing name field");
			Assertions.assertTrue(this.capturedEvents.get(0).getUpdatedFields().contains("name"));
			Assertions.assertTrue(this.capturedEvents.get(0).getUpdatedFields().contains("value"));
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== Field-Specific Inspection test completed successfully ===");
	}

	// ============================================================================
	// SECTION 7: ObjectId Type Testing
	// ============================================================================

	@Order(9)
	@Test
	public void testObjectIdHandling() throws Exception {
		LOGGER.info("=== TEST: ObjectId Handling ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Jack", "admin", 200);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size());
			final ChangeEvent event = this.capturedEvents.get(0);

			// Verify oid is not null
			Assertions.assertNotNull(event.getOid(), "Event should have oid");

			// Get the oid - it's a BsonObjectId or ObjectId from MongoDB's _id field
			// This is NOT the same as the entity's id field (which is a Long)
			final Object oidObj = event.getOid();
			if (oidObj instanceof org.bson.BsonObjectId) {
				final org.bson.BsonObjectId bsonOid = (org.bson.BsonObjectId) oidObj;
				LOGGER.info("Inserted entity ID (Long): {}, Event OID (BsonObjectId): {}", inserted.id,
						bsonOid.getValue());
				// Just verify that we can access the OID - don't compare with entity.id as they're different fields
				Assertions.assertNotNull(bsonOid.getValue());
			} else if (oidObj instanceof ObjectId) {
				final ObjectId oid = (ObjectId) oidObj;
				LOGGER.info("Inserted entity ID (Long): {}, Event OID (ObjectId): {}", inserted.id, oid);
				// Just verify that we can access the OID - don't compare with entity.id as they're different fields
				Assertions.assertNotNull(oid);
			} else {
				Assertions.fail("Unexpected oid type: " + oidObj.getClass());
			}
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== ObjectId Handling test completed successfully ===");
	}

	// ============================================================================
	// SECTION 8: Operation Type Filtering
	// ============================================================================

	@Order(10)
	@Test
	public void testOperationTypeFiltering() throws Exception {
		LOGGER.info("=== TEST: Operation Type Filtering ===");

		manager.clearAllListeners();
		this.capturedEvents.clear();

		// Register listener that only receives INSERT and DELETE (not UPDATE)
		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").withMode(FullDocument.UPDATE_LOOKUP)
				.filterOperation(OperationType.INSERT, OperationType.DELETE).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Kate", "user", 55);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture INSERT");
			Assertions.assertTrue(this.capturedEvents.get(0).isInsert());
		}

		this.capturedEvents.clear();

		// Update - should NOT be captured
		inserted.name = "Kate Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture UPDATE");

		// Delete - should be captured
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture DELETE");
			Assertions.assertTrue(this.capturedEvents.get(0).isDelete());
		}

		LOGGER.info("=== Operation Type Filtering test completed successfully ===");
	}
}
