package org.atriasoft.archidata.dataStreamEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import org.bson.conversions.Bson;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.FullDocument;

/**
 * Singleton manager for MongoDB Change Stream notifications. This is the main
 * entry point for working with MongoDB change streams in ArchiData.
 *
 * <p>
 * The manager handles:
 * </p>
 * <ul>
 * <li>Lifecycle management (starting and stopping the change stream
 * system)</li>
 * <li>Worker thread pool management (one worker per watched collection)</li>
 * <li>Listener registration and event distribution</li>
 * <li>FullDocument mode computation (automatic selection of most demanding
 * mode)</li>
 * <li>Both server-side (aggregation pipeline) and client-side (predicate)
 * filtering</li>
 * <li>Global listeners (all collections) and collection-specific listeners</li>
 * </ul>
 *
 * <p>
 * Basic usage example:
 * </p>
 *
 * <pre>
 * ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
 * manager.start(mongoDatabase);
 *
 * // Register a listener for the "users" collection
 * manager.createListenerBuilder(event -&gt; {
 * 	System.out.println("User changed: " + event.getOid());
 * }, "users").register();
 *
 * // Register a global listener (all collections)
 * manager.registerListener(event -&gt; {
 * 	System.out.println("Database changed: " + event.getCollectionName());
 * });
 *
 * // Clean shutdown
 * manager.stop();
 * </pre>
 *
 * <p>
 * The manager uses DEFAULT FullDocument mode, which provides full documents for
 * INSERT operations and update descriptions for UPDATE operations.
 * </p>
 *
 * <p>
 * Thread Safety: This class is thread-safe and uses ReentrantReadWriteLock to
 * protect internal state. Multiple threads can safely register listeners and
 * receive events concurrently.
 * </p>
 *
 * @see ChangeEvent
 * @see ChangeNotificationListener
 * @see ChangeStreamWorker
 * @see ListenerRegistrationBuilder
 * @see CollectionWatchBuilder
 */
public class ChangeNotificationManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeNotificationManager.class);
	private static final ChangeNotificationManager INSTANCE = new ChangeNotificationManager();

	// Thread pool for Change Stream workers
	private ExecutorService executorService;

	// Workers (one per collection)
	private final Map<String, ChangeStreamWorker> workers = new ConcurrentHashMap<>();
	// Store resume tokens to avoid re-reading events when restarting workers
	private final Map<String, org.bson.BsonDocument> resumeTokens = new ConcurrentHashMap<>();

	// Server-side filtering pipelines by collection
	private final Map<String, List<Bson>> collectionPipelines = new ConcurrentHashMap<>();

	// Listeners by collection with their requested mode and client filter
	private final Map<String, Set<ListenerRegistration>> collectionListeners = new ConcurrentHashMap<>();

	// Global listeners (all collections)
	private final Set<ListenerRegistration> globalListeners = ConcurrentHashMap.newKeySet();

	// MongoDB database reference
	private MongoDatabase database;

	// Global FullDocument mode set at startup (applies to all collections)
	private FullDocument globalFullDocumentMode = FullDocument.UPDATE_LOOKUP;

	// Lifecycle
	private volatile boolean running = false;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	// Statistics
	private final AtomicLong totalEventsProcessed = new AtomicLong(0);

	/**
	 * Private constructor to enforce singleton pattern.
	 */
	private ChangeNotificationManager() {
		// Singleton
	}

	/**
	 * Gets the singleton instance of the ChangeNotificationManager.
	 *
	 * @return The singleton instance
	 */
	public static ChangeNotificationManager getInstance() {
		return INSTANCE;
	}

	// ========== Lifecycle ==========

	/**
	 * Starts the Change Notification Manager and initializes the worker thread
	 * pool. After starting, workers will be created for all registered listeners.
	 *
	 * <p>
	 * This method must be called before the manager can process change events. If
	 * the manager is already running, this method logs a warning and returns
	 * without action.
	 * </p>
	 *
	 * @param database The MongoDB database instance to watch for changes
	 * @throws IllegalStateException if database is null
	 */
	public void start(final MongoDatabase database) {
		start(database, FullDocument.DEFAULT);
	}

	/**
	 * Starts the Change Notification Manager with specified MongoDB database and mode.
	 * This method initializes the worker thread pool and starts the executor pool.
	 * The specified mode will be used globally for all collections.
	 *
	 * @param database The MongoDB database instance to watch for changes
	 * @param mode The FullDocument mode to use for all collections
	 * @throws IllegalArgumentException if database or mode is null
	 */
	public void start(final MongoDatabase database, final FullDocument mode) {
		if (database == null) {
			throw new IllegalArgumentException("Database cannot be null");
		}
		if (mode == null) {
			throw new IllegalArgumentException("FullDocument mode cannot be null");
		}

		this.lock.writeLock().lock();
		try {
			if (this.running) {
				// Check if we're trying to start with a different mode
				if (!this.globalFullDocumentMode.equals(mode)) {
					LOGGER.info(
							"ChangeNotificationManager is running with mode {} but requested mode is {}. Restarting with new mode.",
							this.globalFullDocumentMode, mode);
					// Need to stop and restart with new mode
					this.lock.writeLock().unlock();
					try {
						stop();
					} finally {
						this.lock.writeLock().lock();
					}
				} else {
					LOGGER.debug("ChangeNotificationManager is already running with mode {}", mode);
					return;
				}
			}

			this.database = database;
			this.globalFullDocumentMode = mode;
			this.executorService = Executors.newCachedThreadPool();
			this.running = true;

			// Start workers for all registered collections
			startAllWorkers();

			LOGGER.info("ChangeNotificationManager started with mode: {}", mode);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Starts the Change Notification Manager with default configuration from environment.
	 * Uses DbConfig from environment variables and DEFAULT FullDocument mode.
	 *
	 * @throws DataAccessException if database configuration fails
	 */
	public void start() throws DataAccessException {
		start(new DbConfig());
	}

	/**
	 * Starts the Change Notification Manager with specified DbConfig.
	 * Uses DEFAULT FullDocument mode.
	 *
	 * @param dbConfig The database configuration to use
	 * @throws DataAccessException if database connection fails
	 * @throws IllegalArgumentException if dbConfig is null
	 */
	public void start(final DbConfig dbConfig) throws DataAccessException {
		start(dbConfig, FullDocument.DEFAULT);
	}

	/**
	 * Starts the Change Notification Manager with specified DbConfig and mode.
	 * Creates a MongoClient from the provided configuration.
	 *
	 * @param dbConfig The database configuration to use
	 * @param mode The FullDocument mode to use for all collections
	 * @throws DataAccessException if database connection fails
	 * @throws IllegalArgumentException if dbConfig or mode is null
	 */
	public void start(final DbConfig dbConfig, final FullDocument mode) throws DataAccessException {
		if (dbConfig == null) {
			throw new IllegalArgumentException("DbConfig cannot be null");
		}
		if (mode == null) {
			throw new IllegalArgumentException("FullDocument mode cannot be null");
		}

		try {
			final MongoClient mongoClient = createMongoClient(dbConfig);
			final MongoDatabase databaseTmp = mongoClient.getDatabase(dbConfig.getDbName());
			start(databaseTmp, mode);
		} catch (final Exception e) {
			throw new DataAccessException("Failed to connect to MongoDB: " + e.getMessage());
		}
	}

	/**
	 * Creates a MongoClient from the provided DbConfig.
	 * Uses the same connection URL format as DbIoMongo for consistency.
	 *
	 * @param dbConfig The database configuration
	 * @return A configured MongoClient instance
	 */
	private MongoClient createMongoClient(final DbConfig dbConfig) {
		// Build URL like DbIoMongo does, with proper authentication
		final String url = dbConfig.getUrl();
		final ConnectionString connectionString = new ConnectionString(url);
		final MongoClientSettings clientSettings = MongoClientSettings.builder().applyConnectionString(connectionString)
				.build();
		return MongoClients.create(clientSettings);
	}

	/**
	 * Stops the Change Notification Manager and shuts down all worker threads.
	 * This method gracefully stops all change stream workers and waits for them to
	 * complete.
	 *
	 * <p>
	 * The shutdown process:
	 * </p>
	 * <ol>
	 * <li>Signals all workers to stop</li>
	 * <li>Waits up to 1 second for graceful shutdown</li>
	 * <li>Forces shutdown if workers don't stop within timeout</li>
	 * <li>Waits an additional 500ms for forced shutdown to complete</li>
	 * </ol>
	 *
	 * <p>
	 * After stopping, the manager can be restarted by calling start() again. If
	 * the manager is not running, this method logs a warning and returns without
	 * action.
	 * </p>
	 */
	public void stop() {
		this.lock.writeLock().lock();
		try {
			if (!this.running) {
				LOGGER.warn("ChangeNotificationManager is not running");
				return;
			}

			this.running = false;

			// Stop all workers and save their tokens
			for (final String collectionName : new HashSet<>(this.workers.keySet())) {
				stopWorkerForCollection(collectionName);
			}

			// Shutdown executor
			this.executorService.shutdown();
			try {
				final long start = System.currentTimeMillis();
				if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
					this.executorService.shutdownNow();
					// Give shutdownNow a brief moment to complete
					this.executorService.awaitTermination(2, TimeUnit.SECONDS);
				}
				final long end = System.currentTimeMillis();
				LOGGER.info("Executor shutdown completed in {} seconds", (end - start) * 0.001);
			} catch (final InterruptedException e) {
				this.executorService.shutdownNow();
				Thread.currentThread().interrupt();
			}

			// Clear all state
			this.workers.clear();
			this.collectionListeners.clear();
			this.globalListeners.clear();
			this.resumeTokens.clear();
			this.database = null;

			// Reset global mode to UPDATE_LOOKUP for next test
			this.globalFullDocumentMode = FullDocument.UPDATE_LOOKUP;

			LOGGER.info("ChangeNotificationManager stopped");
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Checks if the Change Notification Manager is currently running.
	 *
	 * @return true if the manager is started and processing events, false
	 *         otherwise
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Check if a collection is being watched
	 *
	 * @param collectionName The collection name
	 * @return true if the collection has an active worker
	 */
	public boolean isWatching(final String collectionName) {
		return this.workers.containsKey(collectionName);
	}

	// ========== Collection Watch Configuration ==========

	/**
	 * Starts watching one or more collections for changes. Workers will be created
	 * for the specified collections if the manager is running.
	 *
	 * <p>
	 * Note: You still need to register listeners to actually receive events. This
	 * method only starts the workers.
	 * </p>
	 *
	 * @param collectionNames One or more collection names to watch
	 */
	public void watchCollection(final String... collectionNames) {
		for (final String name : collectionNames) {
			watchCollectionInternal(name, null);
		}
	}

	/**
	 * Stops watching a collection. The worker for this collection will be stopped
	 * and removed.
	 *
	 * <p>
	 * Listeners registered for this collection will remain registered but will not
	 * receive events until the collection is watched again.
	 * </p>
	 *
	 * @param collectionName The collection name to stop watching
	 */
	public void unwatchCollection(final String collectionName) {
		this.lock.writeLock().lock();
		try {
			stopWorkerForCollection(collectionName);
			this.collectionPipelines.remove(collectionName);
			LOGGER.info("Unwatched collection: {}", collectionName);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Starts watching all non-system collections in the database. System
	 * collections (those starting with "system.") are automatically excluded.
	 *
	 * <p>
	 * This is a convenience method that discovers all collections in the database
	 * and starts watching them. Use with caution in databases with many
	 * collections.
	 * </p>
	 *
	 * @throws IllegalStateException if the manager is not started
	 */
	public void watchAllCollections() {
		if (this.database == null) {
			throw new IllegalStateException("Manager not started - call start() first");
		}

		for (final String collectionName : this.database.listCollectionNames()) {
			if (!collectionName.startsWith("system.")) {
				watchCollection(collectionName);
			}
		}
	}

	/**
	 * Stops watching all collections. All workers will be stopped and removed.
	 *
	 * <p>
	 * Listeners will remain registered but will not receive events until
	 * collections are watched again.
	 * </p>
	 */
	public void unwatchAllCollections() {
		this.lock.writeLock().lock();
		try {
			final Set<String> collections = new HashSet<>(this.workers.keySet());
			for (final String collectionName : collections) {
				unwatchCollection(collectionName);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Gets the set of all currently watched collection names.
	 *
	 * @return An unmodifiable set of collection names that have active workers
	 */
	public Set<String> getWatchedCollections() {
		return Collections.unmodifiableSet(this.workers.keySet());
	}

	/**
	 * Creates a builder for advanced collection watch configuration with
	 * server-side filtering.
	 *
	 * <p>
	 * Use this method when you need server-side filtering via aggregation
	 * pipelines. For client-side filtering, use createListenerBuilder() instead.
	 * </p>
	 *
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * manager.watch("users").fullDocument(FullDocument.UPDATE_LOOKUP).onlyUpdates().whenFieldUpdated("email")
	 * 		.start();
	 * </pre>
	 *
	 * @param collectionName The collection name to watch
	 * @return A CollectionWatchBuilder for fluent configuration
	 * @see CollectionWatchBuilder
	 */
	public CollectionWatchBuilder watch(final String collectionName) {
		return new CollectionWatchBuilder(this, collectionName);
	}

	// ========== Listener Registration ==========

	/**
	 * Register a global listener (all collections)
	 *
	 * @param listener The listener
	 */
	public void registerListener(final ChangeNotificationListener listener) {
		registerListener(listener, (FullDocument) null);
	}

	/**
	 * Register a global listener with specific mode
	 *
	 * @param listener The listener
	 * @param mode FullDocument mode
	 */
	public void registerListener(final ChangeNotificationListener listener, final FullDocument mode) {
		this.lock.writeLock().lock();
		try {
			this.globalListeners.add(new ListenerRegistration(listener, mode, null));
			LOGGER.info("Registered global listener with mode: {}", mode);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Creates a builder for registering a global listener with filtering options.
	 * The listener must call {@code .register()} on the returned builder to
	 * complete registration.
	 *
	 * @param listener The listener
	 * @return Builder for filter configuration
	 */
	public ListenerRegistrationBuilder createListenerBuilder(final ChangeNotificationListener listener) {
		return new ListenerRegistrationBuilder(this, listener, null);
	}

	/**
	 * Register a listener for a specific collection
	 *
	 * @param listener The listener
	 * @param collectionName Collection name
	 */
	public void registerListener(final ChangeNotificationListener listener, final String collectionName) {
		registerListener(listener, collectionName, null);
	}

	/**
	 * Register a listener for a specific collection with mode
	 *
	 * @param listener The listener
	 * @param collectionName Collection name
	 * @param mode FullDocument mode
	 */
	public void registerListener(
			final ChangeNotificationListener listener,
			final String collectionName,
			final FullDocument mode) {
		registerListener(listener, Set.of(collectionName), mode);
	}

	/**
	 * Creates a builder for registering a listener for a specific collection with
	 * filtering options. The listener must call {@code .register()} on the
	 * returned builder to complete registration.
	 *
	 * @param listener The listener
	 * @param collectionName Collection name
	 * @return Builder for filter configuration
	 */
	public ListenerRegistrationBuilder createListenerBuilder(
			final ChangeNotificationListener listener,
			final String collectionName) {
		return new ListenerRegistrationBuilder(this, listener, Set.of(collectionName));
	}

	/**
	 * Register a listener for multiple collections
	 *
	 * @param listener The listener
	 * @param collectionNames Collection names
	 */
	public void registerListener(final ChangeNotificationListener listener, final Set<String> collectionNames) {
		registerListener(listener, collectionNames, null);
	}

	/**
	 * Register a listener for multiple collections with mode
	 *
	 * @param listener The listener
	 * @param collectionNames Collection names
	 * @param mode FullDocument mode
	 */
	public void registerListener(
			final ChangeNotificationListener listener,
			final Set<String> collectionNames,
			final FullDocument mode) {
		this.lock.writeLock().lock();
		try {
			final ListenerRegistration registration = new ListenerRegistration(listener, mode, null);

			for (final String collectionName : collectionNames) {
				this.collectionListeners.computeIfAbsent(collectionName, k -> ConcurrentHashMap.newKeySet())
						.add(registration);

				// Auto-watch if manager is running
				if (this.running && !this.workers.containsKey(collectionName)) {
					startWorkerForCollection(collectionName);
				}
			}

			LOGGER.info("Registered listener for collections: {} with mode: {}", collectionNames, mode);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Creates a builder for registering a listener for multiple collections with
	 * filtering options. The listener must call {@code .register()} on the
	 * returned builder to complete registration.
	 *
	 * @param listener The listener
	 * @param collectionNames Collection names
	 * @return Builder for filter configuration
	 */
	public ListenerRegistrationBuilder createListenerBuilder(
			final ChangeNotificationListener listener,
			final Set<String> collectionNames) {
		return new ListenerRegistrationBuilder(this, listener, collectionNames);
	}

	/**
	 * Internal registration with all parameters (called by builder)
	 */
	void registerListenerInternal(
			final ChangeNotificationListener listener,
			final Set<String> collectionNames,
			final FullDocument mode,
			final Predicate<ChangeEvent> clientFilter) {
		this.lock.writeLock().lock();
		try {
			final ListenerRegistration registration = new ListenerRegistration(listener, mode, clientFilter);

			if (collectionNames == null || collectionNames.isEmpty()) {
				// Global listener
				this.globalListeners.add(registration);
				LOGGER.info("Registered global listener with mode: {} and filter: {}", mode, clientFilter != null);
			} else {
				// Collection-specific listener
				for (final String collectionName : collectionNames) {
					this.collectionListeners.computeIfAbsent(collectionName, k -> ConcurrentHashMap.newKeySet())
							.add(registration);

					// Auto-watch if manager is running and no worker exists
					if (this.running && !this.workers.containsKey(collectionName)) {
						startWorkerForCollection(collectionName);
					}
				}

				LOGGER.info("Registered listener for collections: {} with mode: {} and filter: {}", collectionNames,
						mode, clientFilter != null);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Unregister a listener from all collections
	 *
	 * @param listener The listener to remove
	 */
	public void unregisterListener(final ChangeNotificationListener listener) {
		this.lock.writeLock().lock();
		try {
			// Remove from global listeners
			this.globalListeners.removeIf(reg -> reg.getListener().equals(listener));

			// Remove from collection listeners
			for (final Set<ListenerRegistration> listeners : this.collectionListeners.values()) {
				listeners.removeIf(reg -> reg.getListener().equals(listener));
			}

			LOGGER.info("Unregistered listener");
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Unregister a listener from a specific collection
	 *
	 * @param listener The listener
	 * @param collectionName Collection name
	 */
	public void unregisterListener(final ChangeNotificationListener listener, final String collectionName) {
		this.lock.writeLock().lock();
		try {
			final Set<ListenerRegistration> listeners = this.collectionListeners.get(collectionName);
			if (listeners != null) {
				listeners.removeIf(reg -> reg.getListener().equals(listener));
			}
			LOGGER.info("Unregistered listener from collection: {}", collectionName);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Gets the total number of registered listeners (global and
	 * collection-specific).
	 *
	 * @return The count of all registered listeners
	 */
	public int getListenerCount() {
		int count = this.globalListeners.size();
		for (final Set<ListenerRegistration> listeners : this.collectionListeners.values()) {
			count += listeners.size();
		}
		return count;
	}

	// ========== Configuration ==========

	/**
	 * Sets the default FullDocument mode used when a listener doesn't specify a
	 * mode.
	 *
	 * <p>
	 * This only affects new listeners. Existing workers will not be reconfigured.
	 * </p>
	 *
	 * @param mode The default FullDocument mode (typically DEFAULT or
	 *            UPDATE_LOOKUP)
	 */
	public void setDefaultFullDocumentMode(final FullDocument mode) {
		this.globalFullDocumentMode = mode;
	}

	/**
	 * Gets the default FullDocument mode.
	 *
	 * @return The default mode
	 */
	public FullDocument getDefaultFullDocumentMode() {
		return this.globalFullDocumentMode;
	}

	// ========== Monitoring ==========

	/**
	 * Gets the status of all worker threads. This is useful for monitoring the
	 * health of the change stream system.
	 *
	 * @return An unmodifiable map of collection names to their worker statuses
	 *         (STARTING, RUNNING, RECONNECTING, ERROR, STOPPED)
	 */
	public Map<String, WorkerStatus> getWorkerStatuses() {
		final Map<String, WorkerStatus> statuses = new HashMap<>();
		for (final Map.Entry<String, ChangeStreamWorker> entry : this.workers.entrySet()) {
			statuses.put(entry.getKey(), entry.getValue().getStatus());
		}
		return Collections.unmodifiableMap(statuses);
	}

	/**
	 * Gets the effective FullDocument mode for each watched collection. The
	 * effective mode is the most demanding mode requested by any listener for that
	 * collection.
	 *
	 * @return An unmodifiable map of collection names to their effective
	 *         FullDocument modes
	 */
	public Map<String, FullDocument> getCollectionModes() {
		final Map<String, FullDocument> modes = new HashMap<>();
		for (final String collectionName : this.workers.keySet()) {
			modes.put(collectionName, computeEffectiveMode(collectionName));
		}
		return Collections.unmodifiableMap(modes);
	}

	/**
	 * Gets the total number of change events processed by all workers since the
	 * manager started.
	 *
	 * @return The total event count
	 */
	public long getTotalEventsProcessed() {
		return this.totalEventsProcessed.get();
	}

	/**
	 * Clears all listeners and stops all workers. This is primarily intended for
	 * testing purposes to reset the manager to a clean state.
	 *
	 * <p>
	 * This method:
	 * </p>
	 * <ul>
	 * <li>Removes all global listeners</li>
	 * <li>Removes all collection-specific listeners</li>
	 * <li>Stops all worker threads</li>
	 * <li>Clears the worker map</li>
	 * </ul>
	 *
	 * <p>
	 * After calling this method, you can register new listeners and they will work
	 * as if the manager was freshly started.
	 * </p>
	 */
	public void clearAllListeners() {
		LOGGER.info("Clearing all listeners...");
		this.lock.writeLock().lock();
		try {
			// Clear all listeners but keep workers running
			// This prevents re-reading events when new listeners are added
			this.globalListeners.clear();
			this.collectionListeners.clear();
		} finally {
			this.lock.writeLock().unlock();
		}
		LOGGER.info("All listeners cleared (workers kept running)");
	}

	// ========== Internal Methods ==========

	/**
	 * Called by ChangeStreamWorker when an event arrives
	 */
	void notifyChange(final ChangeEvent event) {
		this.lock.readLock().lock();
		try {
			this.totalEventsProcessed.incrementAndGet();

			final String collectionName = event.getCollectionName();
			LOGGER.info("ChangeNotificationManager received event: collection={}, operation={}, oid={}", collectionName,
					event.getOperationType(), event.getOid());

			// 1. Notify global listeners
			for (final ListenerRegistration reg : this.globalListeners) {
				if (reg.acceptEvent(event)) {
					try {
						reg.getListener().onNotification(event);
					} catch (final Exception e) {
						LOGGER.error("Error in global listener", e);
					}
				}
			}

			// 2. Notify collection-specific listeners
			final Set<ListenerRegistration> listeners = this.collectionListeners.get(collectionName);
			if (listeners != null) {
				for (final ListenerRegistration reg : listeners) {
					if (reg.acceptEvent(event)) {
						try {
							reg.getListener().onNotification(event);
						} catch (final Exception e) {
							LOGGER.error("Error in collection listener for {}", collectionName, e);
						}
					}
				}
			}
		} finally {
			this.lock.readLock().unlock();
		}
	}

	/**
	 * Gets the effective FullDocument mode for a collection.
	 *
	 * <p>
	 * Since the architecture uses a global mode set at startup, this method
	 * returns the same mode for all collections regardless of the collection name.
	 * The mode is defined when calling {@link #start(MongoDatabase, FullDocument)}
	 * and remains fixed for the lifetime of the application.
	 * </p>
	 *
	 * @param collectionName The collection name (parameter kept for backward compatibility)
	 * @return The global FullDocument mode set at startup
	 */
	public FullDocument computeEffectiveMode(final String collectionName) {
		// Mode is now global and fixed at startup
		return this.globalFullDocumentMode;
	}

	private void startAllWorkers() {
		// Get all collections that need watching
		final Set<String> collectionsToWatch = new HashSet<>(this.collectionListeners.keySet());

		// Collections from explicit watch() calls
		collectionsToWatch.addAll(this.collectionPipelines.keySet());

		for (final String collectionName : collectionsToWatch) {
			startWorkerForCollection(collectionName);
		}
	}

	private void stopWorkerForCollection(final String collectionName) {
		final ChangeStreamWorker worker = this.workers.remove(collectionName);
		if (worker != null) {
			// Save the resume token before stopping
			final org.bson.BsonDocument token = worker.getResumeToken();
			if (token != null) {
				this.resumeTokens.put(collectionName, token);
				LOGGER.debug("Saved resume token for collection: {}", collectionName);
			}
			worker.stop();
		}
	}

	private void startWorkerForCollection(final String collectionName) {
		if (this.workers.containsKey(collectionName)) {
			return; // Already running
		}

		if (this.database == null) {
			LOGGER.warn("Cannot start worker - database not set");
			return;
		}

		final MongoCollection<org.bson.Document> collection = this.database.getCollection(collectionName);
		final FullDocument mode = computeEffectiveMode(collectionName);
		final List<Bson> pipeline = this.collectionPipelines.get(collectionName);

		final ChangeStreamWorker worker = new ChangeStreamWorker(collectionName, collection, this, mode, pipeline);

		// If we have a saved resume token for this collection, use it
		final org.bson.BsonDocument savedResumeToken = this.resumeTokens.get(collectionName);
		if (savedResumeToken != null) {
			worker.setResumeToken(savedResumeToken);
			LOGGER.info("Using saved resume token for collection: {}", collectionName);
		}

		this.workers.put(collectionName, worker);
		this.executorService.submit(worker);

		LOGGER.info("Started ChangeStreamWorker for collection: {} with mode: {}", collectionName, mode);
	}

	private void watchCollectionInternal(final String collectionName, final List<Bson> pipeline) {
		this.lock.writeLock().lock();
		try {
			if (pipeline != null) {
				this.collectionPipelines.put(collectionName, pipeline);
			}

			if (this.running && !this.workers.containsKey(collectionName)) {
				startWorkerForCollection(collectionName);
			}

			LOGGER.info("Watching collection: {}", collectionName);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Called by CollectionWatchBuilder
	 */
	void startWatchingCollection(final String collectionName, final FullDocument mode, final List<Bson> pipeline) {
		// mode parameter is ignored as we use global mode
		watchCollectionInternal(collectionName, pipeline);
	}

	// ========== Inner Classes ==========

	/**
	 * Internal class to hold listener with its client filter
	 */
	static class ListenerRegistration {
		private final ChangeNotificationListener listener;
		private final Predicate<ChangeEvent> clientFilter;

		public ListenerRegistration(final ChangeNotificationListener listener, final FullDocument mode,
				final Predicate<ChangeEvent> clientFilter) {
			this.listener = listener;
			// mode parameter is kept for backward compatibility but not stored as we use global mode
			this.clientFilter = clientFilter;
		}

		public ChangeNotificationListener getListener() {
			return this.listener;
		}

		public Predicate<ChangeEvent> getClientFilter() {
			return this.clientFilter;
		}

		public boolean hasClientFilter() {
			return this.clientFilter != null;
		}

		public boolean acceptEvent(final ChangeEvent event) {
			return this.clientFilter == null || this.clientFilter.test(event);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			final ListenerRegistration that = (ListenerRegistration) o;
			return Objects.equals(this.listener, that.listener) && Objects.equals(this.clientFilter, that.clientFilter);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.listener, this.clientFilter);
		}
	}
}
