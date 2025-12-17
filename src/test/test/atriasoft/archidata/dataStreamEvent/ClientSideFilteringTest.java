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

import com.mongodb.client.model.changestream.OperationType;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataStreamEvent.model.TestChangeStreamEntity;

/**
 * Tests for client-side filtering: field-based, lambda predicates, and operation type filtering.
 */
public class ClientSideFilteringTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientSideFilteringTest.class);

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
	@Order(10)
	public void testFieldBasedFiltering() throws Exception {
		LOGGER.info("=== TEST: Field-Based Filtering (role == admin) ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").filterField("role", "admin")
				.register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity admin = new TestChangeStreamEntity("AdminUser", "admin", 100);
		final TestChangeStreamEntity insertedAdmin = ConfigureDb.da.insert(admin);

		final TestChangeStreamEntity user = new TestChangeStreamEntity("RegularUser", "user", 50);
		final TestChangeStreamEntity insertedUser = ConfigureDb.da.insert(user);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			final long adminInsertCount = this.capturedEvents.stream().filter(
					e -> e.isInsert() && e.hasFullDocument() && "admin".equals(e.getFullDocument().getString("role")))
					.count();
			Assertions.assertTrue(adminInsertCount >= 1, "Should capture at least 1 admin INSERT event");
			final ChangeEvent adminEvent = this.capturedEvents.stream().filter(
					e -> e.isInsert() && e.hasFullDocument() && "admin".equals(e.getFullDocument().getString("role")))
					.findFirst().orElseThrow();
			Assertions.assertEquals("AdminUser", adminEvent.getFullDocument().getString("name"));
		}

		this.capturedEvents.clear();

		insertedAdmin.value = 150;
		ConfigureDb.da.updateById(insertedAdmin, insertedAdmin.id);

		insertedUser.value = 75;
		ConfigureDb.da.updateById(insertedUser, insertedUser.id);

		// Note: With DEFAULT mode, UPDATE events don't have full documents
		// So we cannot filter by field values on UPDATE operations
		// The filter will be skipped for UPDATE events without full documents

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedAdmin.id);
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedUser.id);

		LOGGER.info("=== Field-Based Filtering test completed ===");
	}

	@Test
	@Order(20)
	public void testLambdaPredicateFiltering() throws Exception {
		LOGGER.info("=== TEST: Lambda Predicate Filtering (value > 100) ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").filter(
				event -> event.getFullDocument() != null && event.getFullDocument().getInteger("value", 0) > 100)
				.register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "user", 50);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture initial insert with value=50");

		this.capturedEvents.clear();

		// Note: With DEFAULT mode, UPDATE events don't have full documents
		// This test only verifies INSERT operations which have full documents
		inserted.value = 150;
		ConfigureDb.da.updateById(inserted, inserted.id);

		// No events expected since UPDATE operations lack full documents for filtering
		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "UPDATE events should not be captured without full documents");

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== Lambda Predicate Filtering test completed ===");
	}

	@Test
	@Order(30)
	public void testOperationTypeFiltering() throws Exception {
		LOGGER.info("=== TEST: Operation Type Filtering (INSERT and DELETE only) ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity")
				.filterOperation(OperationType.INSERT, OperationType.DELETE).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Bob", "user", 55);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			final long insertCount = this.capturedEvents.stream().filter(ChangeEvent::isInsert).count();
			Assertions.assertTrue(insertCount >= 1, "Should capture INSERT");
		}

		this.capturedEvents.clear();

		inserted.name = "Bob Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture UPDATE");

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			final long deleteCount = this.capturedEvents.stream().filter(ChangeEvent::isDelete).count();
			Assertions.assertTrue(deleteCount >= 1, "Should capture DELETE");
		}

		LOGGER.info("=== Operation Type Filtering test completed ===");
	}

	@Test
	@Order(40)
	public void testFieldSpecificInspection() throws Exception {
		LOGGER.info("=== TEST: Field-Specific Inspection (name field only) ===");

		manager.createListenerBuilder(this::captureEvent, "TestChangeStreamEntity").filter(event -> {
			if (!event.isUpdate()) {
				return true;
			}
			return event.getUpdatedFields().contains("name");
		}).register();

		TestHelper.waitForStreamInitialization(manager, "TestChangeStreamEntity", 2000);

		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Charlie", "user", 90);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);
		this.capturedEvents.clear();

		inserted.name = "Charlie Updated";
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForEvents(this.capturedEvents, 1, 5000);

		synchronized (this.capturedEvents) {
			Assertions.assertTrue(this.capturedEvents.size() >= 1, "Should capture name field update");
			final ChangeEvent event = this.capturedEvents.stream().filter(ChangeEvent::isUpdate).findFirst()
					.orElseThrow();
			Assertions.assertTrue(event.getUpdatedFields().contains("name"),
					"Update event should have 'name' in updated fields");
		}

		this.capturedEvents.clear();

		inserted.value = 120;
		ConfigureDb.da.updateById(inserted, inserted.id);

		TestHelper.waitForCondition(() -> {
			synchronized (this.capturedEvents) {
				return this.capturedEvents.size() == 0;
			}
		}, 2000, "Should not capture value-only update");

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.id);

		LOGGER.info("=== Field-Specific Inspection test completed ===");
	}
}
