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

		// The change stream delivers the events in the order of the writes: the regular user is written first,
		// so when the admin insert has arrived the filter has already had its say on the user insert.
		final TestChangeStreamEntity user = new TestChangeStreamEntity("RegularUser", "user", 50);
		final TestChangeStreamEntity insertedUser = ConfigureDb.da.insert(user);

		final TestChangeStreamEntity admin = new TestChangeStreamEntity("AdminUser", "admin", 100);
		final TestChangeStreamEntity insertedAdmin = ConfigureDb.da.insert(admin);

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Only the admin INSERT should be captured");
			final ChangeEvent adminEvent = this.capturedEvents.get(0);
			Assertions.assertTrue(adminEvent.isInsert());
			Assertions.assertTrue(adminEvent.hasFullDocument());
			Assertions.assertEquals("admin", adminEvent.getFullDocument().getString("role"));
			Assertions.assertEquals("AdminUser", adminEvent.getFullDocument().getString("name"));
			this.capturedEvents.clear();
		}

		// The manager runs with UPDATE_LOOKUP: an UPDATE carries the full document, so the filter applies to it too.
		// The user update is written first, the admin one is the marker that must arrive.
		insertedUser.value = 75;
		ConfigureDb.da.updateById(insertedUser, insertedUser.getId());

		insertedAdmin.value = 150;
		ConfigureDb.da.updateById(insertedAdmin, insertedAdmin.getId());

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Only the admin UPDATE should be captured");
			final ChangeEvent updateEvent = this.capturedEvents.get(0);
			Assertions.assertTrue(updateEvent.isUpdate());
			Assertions.assertEquals("admin", updateEvent.getFullDocument().getString("role"));
			Assertions.assertEquals(150, updateEvent.getFullDocument().getInteger("value"));
		}

		// A DELETE has no full document: the field filter drops it. Nothing can be written after it to mark the end
		// of the dispatch, so it is only a clean-up here.
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedAdmin.getId());
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, insertedUser.getId());

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

		// value = 50: the predicate refuses the INSERT
		final TestChangeStreamEntity entity = new TestChangeStreamEntity("Alice", "user", 50);
		final TestChangeStreamEntity inserted = ConfigureDb.da.insert(entity);

		// The manager runs with UPDATE_LOOKUP: the UPDATE carries the full document (value = 150), so it passes the
		// predicate. It is the marker of the test: the events arrive in the order of the writes, so once it is
		// there, the INSERT has been dispatched and refused.
		inserted.value = 150;
		ConfigureDb.da.updateById(inserted, inserted.getId());

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(),
					"Only the UPDATE (value=150) should be captured, not the INSERT (value=50)");
			final ChangeEvent event = this.capturedEvents.get(0);
			Assertions.assertTrue(event.isUpdate());
			Assertions.assertEquals(150, event.getFullDocument().getInteger("value"));
		}

		// A DELETE has no full document: the predicate refuses it. No later event can mark the end of the dispatch,
		// so this is only the clean-up of the entity.
		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.getId());

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

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture exactly the INSERT");
			Assertions.assertTrue(this.capturedEvents.get(0).isInsert());
			this.capturedEvents.clear();
		}

		// The UPDATE is refused by the filter. The DELETE written after it is the marker that must arrive: the
		// events come in the order of the writes, so once it is there the UPDATE has been dispatched and refused.
		inserted.name = "Bob Updated";
		ConfigureDb.da.updateById(inserted, inserted.getId());

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.getId());

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture only the DELETE, not the UPDATE");
			Assertions.assertTrue(this.capturedEvents.get(0).isDelete());
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

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);
		this.capturedEvents.clear();

		inserted.name = "Charlie Updated";
		ConfigureDb.da.updateById(inserted, inserted.getId());

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture exactly the name field update");
			final ChangeEvent event = this.capturedEvents.get(0);
			Assertions.assertTrue(event.isUpdate());
			Assertions.assertTrue(event.getUpdatedFields().contains("name"),
					"Update event should have 'name' in updated fields");
		}

		synchronized (this.capturedEvents) {
			this.capturedEvents.clear();
		}

		// A value-only UPDATE is refused by the filter. The UPDATE of the name written after it is the marker that
		// must arrive: the events come in the order of the writes, so once it is there the value-only UPDATE has
		// been dispatched and refused.
		inserted.value = 120;
		ConfigureDb.da.updateById(inserted, inserted.getId());

		inserted.name = "Charlie Final";
		ConfigureDb.da.updateById(inserted, inserted.getId());

		TestHelper.waitForEvents(this.capturedEvents, 1, 2000);

		synchronized (this.capturedEvents) {
			Assertions.assertEquals(1, this.capturedEvents.size(), "Should capture only the name update");
			Assertions.assertTrue(this.capturedEvents.get(0).getUpdatedFields().contains("name"));
			Assertions.assertFalse(this.capturedEvents.get(0).getUpdatedFields().contains("value"),
					"The value did not change in this update");
		}

		ConfigureDb.da.deleteById(TestChangeStreamEntity.class, inserted.getId());

		LOGGER.info("=== Field-Specific Inspection test completed ===");
	}
}
