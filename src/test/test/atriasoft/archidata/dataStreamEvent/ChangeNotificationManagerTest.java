package test.atriasoft.archidata.dataStreamEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationListener;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.atriasoft.archidata.dataStreamEvent.CollectionWatchBuilder;
import org.atriasoft.archidata.dataStreamEvent.ListenerRegistrationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.changestream.FullDocument;

/**
 * Basic tests for ChangeNotificationManager
 * Note: These tests verify the API and basic functionality without MongoDB
 */
public class ChangeNotificationManagerTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeNotificationManagerTest.class);

	private ChangeNotificationManager manager;

	@BeforeEach
	public void setUp() {
		this.manager = ChangeNotificationManager.getInstance();
	}

	@AfterEach
	public void tearDown() {
		if (this.manager.isRunning()) {
			this.manager.stop();
		}
		// Clear all listeners for next test (singleton state cleanup)
		this.manager.clearAllListeners();
	}

	@Test
	@Order(1)
	public void testSingletonInstance() {
		final ChangeNotificationManager instance1 = ChangeNotificationManager.getInstance();
		final ChangeNotificationManager instance2 = ChangeNotificationManager.getInstance();
		assertEquals(instance1, instance2, "Should return same singleton instance");
	}

	@Test
	@Order(20)
	public void testInitialState() {
		assertFalse(this.manager.isRunning(), "Manager should not be running initially");
		assertEquals(0, this.manager.getListenerCount(), "Should have no listeners initially");
		assertTrue(this.manager.getWatchedCollections().isEmpty(), "Should have no watched collections initially");
	}

	@Test
	@Order(30)
	public void testRegisterGlobalListener() {
		this.manager.registerListener(event -> LOGGER.info("Event received: {}", event));

		assertEquals(1, this.manager.getListenerCount(), "Should have 1 listener");
	}

	@Test
	@Order(40)
	public void testRegisterCollectionListener() {
		this.manager.registerListener(event -> LOGGER.info("User event: {}", event), "users");

		assertEquals(1, this.manager.getListenerCount(), "Should have 1 listener");
	}

	@Test
	@Order(50)
	public void testRegisterMultipleListeners() {
		this.manager.registerListener(event -> LOGGER.info("Global: {}", event));
		this.manager.registerListener(event -> LOGGER.info("Users: {}", event), "users");
		this.manager.registerListener(event -> LOGGER.info("Orders: {}", event), "orders");

		assertEquals(3, this.manager.getListenerCount(), "Should have 3 listeners");
	}

	@Test
	@Order(60)
	public void testUnregisterListener() {
		final ChangeNotificationListener listener = event -> LOGGER.info("Event: {}", event);
		this.manager.registerListener(listener);
		assertEquals(1, this.manager.getListenerCount());

		this.manager.unregisterListener(listener);
		assertEquals(0, this.manager.getListenerCount(), "Listener should be removed");
	}

	@Test
	@Order(70)
	public void testDefaultFullDocumentMode() {
		// The global mode defaults to UPDATE_LOOKUP when not explicitly set during start()
		// Since this test doesn't start the manager with a database, it uses the internal default
		assertEquals(FullDocument.UPDATE_LOOKUP, this.manager.getDefaultFullDocumentMode());
	}

	@Test
	@Order(80)
	public void testComputeEffectiveMode() {
		// Register listeners - the mode parameter is now ignored, global mode is used
		this.manager.registerListener(event -> LOGGER.info("Listener 1"), "users", FullDocument.DEFAULT);
		this.manager.registerListener(event -> LOGGER.info("Listener 2"), "users", FullDocument.UPDATE_LOOKUP);

		// The effective mode is always the global mode (UPDATE_LOOKUP by default)
		final FullDocument effectiveMode = this.manager.computeEffectiveMode("users");
		assertEquals(FullDocument.UPDATE_LOOKUP, effectiveMode, "Should always return the global mode");
	}

	@Test
	@Order(90)
	public void testBuilderApi() {
		this.manager.createListenerBuilder(event -> LOGGER.info("Admin user: {}", event), "users")
				.filterField("role", "admin").register();

		assertEquals(1, this.manager.getListenerCount(), "Should have 1 listener registered via builder");
	}

	@Test
	@Order(100)
	public void testWatchBuilder() {
		final CollectionWatchBuilder builder = this.manager.watch("users");
		assertNotNull(builder, "Watch builder should not be null");
	}

	@Test
	@Order(110)
	public void testListenerRegistrationBuilder() {
		final ListenerRegistrationBuilder builder = this.manager
				.createListenerBuilder(event -> LOGGER.info("Event: {}", event));
		assertNotNull(builder, "Listener registration builder should not be null");
	}
}
