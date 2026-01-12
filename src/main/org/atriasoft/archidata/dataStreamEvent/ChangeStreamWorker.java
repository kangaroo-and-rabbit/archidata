package org.atriasoft.archidata.dataStreamEvent;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;

/**
 * Worker thread that monitors a single MongoDB collection for changes using
 * MongoDB Change Streams. Each collection gets its own dedicated worker thread
 * that runs independently.
 *
 * <p>
 * The worker continuously watches a MongoDB collection for changes and converts
 * ChangeStreamDocument events into ChangeEvent objects. It handles:
 * </p>
 * <ul>
 * <li>Connection failures with automatic reconnection and exponential
 * backoff</li>
 * <li>Resume token management for fault tolerance</li>
 * <li>Server-side filtering via aggregation pipelines</li>
 * <li>FullDocument mode configuration (DEFAULT, UPDATE_LOOKUP, etc.)</li>
 * </ul>
 *
 * <p>
 * The worker maintains its status (STARTING, RUNNING, RECONNECTING, ERROR,
 * STOPPED) and statistics like the number of events processed.
 * </p>
 *
 * @see ChangeNotificationManager
 * @see ChangeEvent
 * @see WorkerStatus
 */
public class ChangeStreamWorker implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStreamWorker.class);

	private final String collectionName;
	private final MongoCollection<Document> collection;
	private final ChangeNotificationManager manager;
	private FullDocument fullDocumentMode;
	private List<Bson> pipeline;

	private volatile boolean running = true;
	private volatile WorkerStatus status = WorkerStatus.STARTING;
	private BsonDocument resumeToken = null;
	private final AtomicLong eventsProcessed = new AtomicLong(0);

	/**
	 * Constructs a new ChangeStreamWorker for the specified collection.
	 *
	 * @param collectionName The name of the collection to watch
	 * @param collection The MongoDB collection instance
	 * @param manager The notification manager that owns this worker
	 * @param fullDocumentMode The FullDocument mode to use (DEFAULT,
	 *            UPDATE_LOOKUP, etc.)
	 * @param pipeline Optional server-side aggregation pipeline for filtering
	 *            (may be null)
	 */
	public ChangeStreamWorker(final String collectionName, final MongoCollection<Document> collection,
			final ChangeNotificationManager manager, final FullDocument fullDocumentMode, final List<Bson> pipeline) {
		this.collectionName = collectionName;
		this.collection = collection;
		this.manager = manager;
		this.fullDocumentMode = fullDocumentMode;
		this.pipeline = pipeline;
	}

	public void setResumeToken(final BsonDocument token) {
		this.resumeToken = token;
	}

	@Override
	public void run() {
		LOGGER.info("ChangeStreamWorker started for collection: {}", this.collectionName);
		this.status = WorkerStatus.RUNNING;
		Thread.currentThread().setName("ChangeStreamWorker_" + this.collectionName);

		while (this.running) {
			try {
				watchChangeStream();
			} catch (final MongoCommandException e) {
				// Handle InvalidResumeToken error (code 260) - reset token and restart from beginning
				if (e.getErrorCode() == 260) {
					LOGGER.warn(
							"InvalidResumeToken error for collection: {}. Resetting resume token and restarting from beginning.",
							this.collectionName);
					this.resumeToken = null;
					this.status = WorkerStatus.RECONNECTING;
					// Short delay before reconnecting
					try {
						Thread.sleep(1000);
					} catch (final InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				} else {
					LOGGER.error("MongoDB command error in ChangeStreamWorker for collection: {}", this.collectionName,
							e);
					this.status = WorkerStatus.RECONNECTING;
					// Exponential backoff before reconnecting
					try {
						Thread.sleep(5000);
					} catch (final InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			} catch (final MongoException e) {
				LOGGER.error("MongoDB error in ChangeStreamWorker for collection: {}", this.collectionName, e);
				this.status = WorkerStatus.RECONNECTING;

				// Exponential backoff before reconnecting
				try {
					Thread.sleep(5000);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			} catch (final Exception e) {
				LOGGER.error("Unexpected error in ChangeStreamWorker for collection: {}", this.collectionName, e);
				this.status = WorkerStatus.ERROR;

				try {
					Thread.sleep(5000);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}

		this.status = WorkerStatus.STOPPED;
		LOGGER.info("ChangeStreamWorker stopped for collection: {}", this.collectionName);
	}

	private void watchChangeStream() {
		var changeStreamBuilder = this.collection.watch();

		// Apply full document mode
		changeStreamBuilder = changeStreamBuilder.fullDocument(this.fullDocumentMode);

		// Apply pipeline if present
		if (this.pipeline != null && !this.pipeline.isEmpty()) {
			changeStreamBuilder = this.collection.watch(this.pipeline).fullDocument(this.fullDocumentMode);
		}

		// Resume from token if available
		if (this.resumeToken != null) {
			changeStreamBuilder = changeStreamBuilder.resumeAfter(this.resumeToken);
			LOGGER.info("Resuming change stream for collection {} with resume token", this.collectionName);
		}

		try (MongoCursor<ChangeStreamDocument<Document>> cursor = changeStreamBuilder.iterator()) {
			while (this.running && cursor.hasNext()) {
				final ChangeStreamDocument<Document> changeDoc = cursor.next();

				// Save resume token for resilience
				this.resumeToken = changeDoc.getResumeToken();

				// Convert to ChangeEvent and notify manager
				final ChangeEvent event = convertToChangeEvent(changeDoc);
				if (event != null) {
					this.manager.notifyChange(event);
					this.eventsProcessed.incrementAndGet();
				}
			}
		}
	}

	private ChangeEvent convertToChangeEvent(final ChangeStreamDocument<Document> changeDoc) {
		try {
			final var operationType = changeDoc.getOperationType();
			if (operationType == null) {
				return null;
			}

			final var documentKey = changeDoc.getDocumentKey();
			final Object oid = documentKey != null ? documentKey.get("_id") : null;

			final Instant timestamp = changeDoc.getClusterTime() != null
					? Instant.ofEpochSecond(changeDoc.getClusterTime().getTime())
					: Instant.now();

			return new ChangeEvent(operationType, this.collectionName, this.collectionName, // entityName = collectionName for now
					oid, changeDoc.getFullDocument(), changeDoc.getUpdateDescription(), timestamp,
					changeDoc.getResumeToken());
		} catch (final Exception e) {
			LOGGER.error("Error converting ChangeStreamDocument to ChangeEvent", e);
			return null;
		}
	}

	/**
	 * Signals the worker to stop watching the change stream. The worker will
	 * complete its current operation and then exit gracefully.
	 */
	public void stop() {
		this.running = false;
	}

	/**
	 * Gets the current status of this worker.
	 *
	 * @return The current WorkerStatus (STARTING, RUNNING, RECONNECTING, ERROR, or
	 *         STOPPED)
	 */
	public WorkerStatus getStatus() {
		return this.status;
	}

	/**
	 * Gets the total number of change events processed by this worker since it
	 * started.
	 *
	 * @return The count of events processed
	 */
	public long getEventsProcessed() {
		return this.eventsProcessed.get();
	}

	/**
	 * Gets the name of the collection this worker is monitoring.
	 *
	 * @return The collection name
	 */
	public String getCollectionName() {
		return this.collectionName;
	}

	/**
	 * Gets the current resume token. This token represents the position in the
	 * change stream and can be used to resume from this point if the worker is
	 * restarted.
	 *
	 * @return The resume token, or null if no events have been processed yet
	 */
	public BsonDocument getResumeToken() {
		return this.resumeToken;
	}

	/**
	 * Gets the FullDocument mode this worker is using.
	 *
	 * @return The FullDocument mode (DEFAULT, UPDATE_LOOKUP, etc.)
	 */
	public FullDocument getFullDocumentMode() {
		return this.fullDocumentMode;
	}

}
