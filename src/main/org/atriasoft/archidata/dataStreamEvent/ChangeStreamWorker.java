package org.atriasoft.archidata.dataStreamEvent;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.bson.BsonDocument;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;

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
 * <li>Connection failures with automatic reconnection after a fixed delay (1 s
 * or 5 s, no exponential backoff, no retry limit)</li>
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
 * <p>
 * A change stream opened without a start point begins when its {@code aggregate} runs on the server,
 * on this thread: the worker tells the manager when that first attempt is over, so that a registration
 * returns only once a write made after it is sure to be notified. After {@link #stop()}, the worker
 * ends when its current {@code getMore} returns (the server waits at most its await time, 1 s by
 * default, when there is no change), and the manager does not dispatch any event it reads after it
 * was stopped. The position the manager keeps when it stops the worker ({@link #getResumeToken()}) is the one of
 * the last event dispatched (or of the stream when it was last idle), not of the last event read: the next worker of
 * the collection dispatches an event this one read but did not dispatch. Except for a worker started without a
 * token, until it dispatches an event or sees its stream idle (a {@code getMore} with no change, at most the server's
 * await time after the stream opened, 1 s by default): it has no position then, and the next worker starts when its
 * own stream opens, after such an event.
 * </p>
 *
 * @see ChangeNotificationManager
 * @see ChangeEvent
 * @see WorkerStatus
 */
public class ChangeStreamWorker implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStreamWorker.class);

	// Threads: the worker thread is the one the manager's executor runs run() on, one per worker; the manager's
	// notifyChange() calls eventDispatched() on it. The threads that call the manager (and the listener-facing
	// getters) call stop(), setResumeToken() and the getters.
	// The five final fields below are set by the constructor and never change, so any thread reads them.
	private final String collectionName;
	private final MongoCollection<Document> collection;
	private final ChangeNotificationManager manager;
	private final FullDocument fullDocumentMode;
	private final List<Bson> pipeline;

	// Written by stop() (any thread, the manager calls it with its write lock held), read by the worker thread. Volatile.
	private volatile boolean running = true;
	// Written by the worker thread only (runLoop, and run() when an Error ends it), read by any thread through
	// getStatus(). Volatile.
	private volatile WorkerStatus status = WorkerStatus.STARTING;
	// Position of this worker's own stream: the token of the last event it READ, dispatched or not. Plain field, no
	// lock, confined to the worker thread: written by setResumeToken(), which the manager calls on its own thread
	// before it submits the worker (the submission orders that write before the worker thread starts), then read
	// and written by the worker thread only (runLoop and watchChangeStream), which re-opens its stream from it after
	// a failure. It is never the position the manager saves: an event read here may never be dispatched (the worker
	// was stopped meanwhile), so resuming after it would lose it. See dispatchedToken.
	private BsonDocument resumeToken = null;
	// Position a later worker of the collection resumes from, before which every event read was handled: the token
	// the worker was started from, then the token of each event the manager DISPATCHED, or the post-batch resume token
	// of the stream when a poll returns no change. Written by setResumeToken() (before the submission, as above), by
	// eventDispatched(), which notifyChange() calls on the worker thread with the manager's read lock held once the
	// listeners returned, and by streamIdle() on the worker thread without lock (no event is pending then). Read by
	// getResumeToken(), which the manager calls with its write lock held when it stops the worker: the two sides of
	// the lock exclude each other, so the manager reads the position before an event or after an event dispatched in
	// full, never the token of an event read and not dispatched. Null for a worker started without a token until its
	// first dispatch or idle poll: the driver gives no token for a stream just opened without a start point.
	// Volatile: getResumeToken() is public, and the volatile write publishes the token object it holds.
	private volatile BsonDocument dispatchedToken = null;
	// Incremented by the worker thread only, read by any thread through getEventsProcessed(). Atomic.
	private final AtomicLong eventsProcessed = new AtomicLong(0);
	/** Released when the first attempt to open the change stream is over, whether it opened it or failed. */
	private final CountDownLatch firstOpenAttemptDone = new CountDownLatch(1);
	/** Released when {@link #run()} returns. */
	private final CountDownLatch ended = new CountDownLatch(1);
	// Written by the worker thread only (true once the stream is open, false when it closes), read by
	// awaitStreamOpen() on a manager thread. Volatile.
	private volatile boolean streamOpen = false;

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
		this.pipeline = pipeline == null ? null : List.copyOf(pipeline);
	}

	/**
	 * Sets the resume token the worker opens its change stream from. Call it before the worker runs.
	 *
	 * @param token the BSON resume token, or {@code null} to start when the stream opens
	 */
	public void setResumeToken(final BsonDocument token) {
		this.resumeToken = token == null ? null : token.clone();
		this.dispatchedToken = token == null ? null : token.clone();
	}

	/**
	 * Records that the manager dispatched an event of this worker to the listeners. Called by
	 * {@link ChangeNotificationManager} on the worker thread, with its read lock held, once the listeners returned.
	 *
	 * @param token the resume token of the event (a copy the caller does not keep), ignored when {@code null}
	 */
	void eventDispatched(final BsonDocument token) {
		if (token != null) {
			this.dispatchedToken = token;
		}
	}

	/**
	 * Records the position of the stream when a poll returned no change, on the worker thread, without lock: every
	 * event read before was handled, so the manager may resume from there, whichever of this value and the previous
	 * one it reads.
	 */
	private void streamIdle(final BsonDocument token) {
		if (token != null && this.running) {
			this.dispatchedToken = token;
		}
	}

	/**
	 * Runs the worker until {@link #stop()}. An {@link Error} thrown on this thread (by a listener, an
	 * {@code AssertionError} included, or by the JVM) ends the worker: it is logged at ERROR with the collection
	 * name, the status becomes {@link WorkerStatus#STOPPED}, and the error is thrown again.
	 */
	@Override
	public void run() {
		try {
			runLoop();
		} catch (final Error e) {
			// Not retried like an exception: the JVM may be in a broken state (OutOfMemoryError,
			// StackOverflowError...). The executor keeps the Error in the Future nobody reads, so without this
			// log and status the worker died in silence and the manager went on reporting it RUNNING
			try {
				LOGGER.error(
						"ChangeStreamWorker for collection {} ended by an Error (a listener's included): it no"
								+ " longer reads its stream, unwatch then watch the collection to start a new one",
						this.collectionName, e);
			} finally {
				// After the log: a thread that sees STOPPED sees the log written
				this.status = WorkerStatus.STOPPED;
			}
			throw e;
		} finally {
			this.firstOpenAttemptDone.countDown();
			this.ended.countDown();
		}
	}

	private void runLoop() {
		LOGGER.info("ChangeStreamWorker started for collection: {}", this.collectionName);
		this.status = WorkerStatus.RUNNING;
		Thread.currentThread().setName("ChangeStreamWorker_" + this.collectionName);

		while (this.running) {
			try {
				watchChangeStream();
			} catch (final com.mongodb.MongoInterruptedException ex) {
				LOGGER.warn("MongoInterruptedException error for collection: {}=> {} (normal case)",
						this.collectionName, ex.getMessage());
				this.resumeToken = null;
				this.status = WorkerStatus.RECONNECTING;
				// Short delay before reconnecting
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
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
					// Fixed delay before reconnecting
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

				// Fixed delay before reconnecting
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
		ChangeStreamIterable<Document> changeStreamBuilder = this.collection.watch();

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

		// The aggregate runs here: from now on, every change of the collection reaches this cursor
		final MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor;
		try {
			cursor = changeStreamBuilder.cursor();
			this.streamOpen = true;
		} finally {
			// Opened or failed, the first attempt is over: a registration does not wait for the retries
			this.firstOpenAttemptDone.countDown();
		}
		try (cursor) {
			while (this.running) {
				// tryNext() returns null after an empty getMore, so the stop flag is checked at least once per
				// server await time; hasNext() would block until a change arrives, whatever stop() says
				final ChangeStreamDocument<Document> changeDoc = cursor.tryNext();
				if (changeDoc == null) {
					if (cursor.getServerCursor() == null) {
						// The server closed the stream (invalidate): open it again
						return;
					}
					// The stream is idle: every event it returned before was handled (dispatched, or read by this
					// worker once stopped, which then ends here). Its position, the post-batch resume token, is the
					// only one a worker that dispatched nothing yet has: a fresh stream gives no token before this
					streamIdle(cursor.getResumeToken());
					continue;
				}

				// Position of this stream, re-opened from it after a failure. The position the manager saves when it
				// stops the worker moves only once notifyChange() has dispatched the event (eventDispatched()), or
				// when the stream is idle (streamIdle())
				this.resumeToken = changeDoc.getResumeToken();

				// Convert to ChangeEvent and notify manager
				final ChangeEvent event = convertToChangeEvent(changeDoc);
				if (event != null) {
					this.manager.notifyChange(this, event);
					this.eventsProcessed.incrementAndGet();
				}
			}
		} finally {
			this.streamOpen = false;
		}
	}

	private ChangeEvent convertToChangeEvent(final ChangeStreamDocument<Document> changeDoc) {
		try {
			final OperationType operationType = changeDoc.getOperationType();
			if (operationType == null) {
				return null;
			}

			final BsonDocument documentKey = changeDoc.getDocumentKey();
			final Object oid = documentKey != null ? documentKey.get("_id") : null;

			final BsonTimestamp clusterTime = changeDoc.getClusterTime();
			final Instant timestamp = clusterTime != null ? Instant.ofEpochSecond(clusterTime.getTime())
					: Instant.now();

			return new ChangeEvent(operationType, this.collectionName, this.collectionName, // the entity name is the collection name
					oid, changeDoc.getFullDocument(), changeDoc.getUpdateDescription(), timestamp,
					changeDoc.getResumeToken());
		} catch (final Exception e) {
			LOGGER.error("Error converting ChangeStreamDocument to ChangeEvent", e);
			return null;
		}
	}

	/**
	 * Signals the worker to stop watching the change stream. The worker will
	 * complete its current operation and then exit gracefully: it ends when its current
	 * {@code getMore} returns, at most the server's await time (1 s by default) when there is no change.
	 */
	public void stop() {
		this.running = false;
	}

	/**
	 * Tells whether {@link #stop()} was called.
	 *
	 * @return true once the worker was asked to stop
	 */
	boolean isStopRequested() {
		return !this.running;
	}

	/**
	 * Waits until the first attempt to open the change stream is over.
	 *
	 * @param timeout the maximum time to wait
	 * @param unit the unit of {@code timeout}
	 * @return true when the stream is open: a change made after this call returns true reaches the worker
	 * @throws InterruptedException if the calling thread is interrupted while waiting
	 */
	boolean awaitStreamOpen(final long timeout, final TimeUnit unit) throws InterruptedException {
		return this.firstOpenAttemptDone.await(timeout, unit) && this.streamOpen;
	}

	/**
	 * Waits until the worker has ended (its {@link #run()} returned).
	 *
	 * @param timeout the maximum time to wait
	 * @param unit the unit of {@code timeout}
	 * @return true when the worker has ended
	 * @throws InterruptedException if the calling thread is interrupted while waiting
	 */
	boolean awaitEnd(final long timeout, final TimeUnit unit) throws InterruptedException {
		return this.ended.await(timeout, unit);
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
	 * Gets the position a new worker of the collection can resume from without losing an event: the resume token of
	 * the last event the manager dispatched to the listeners, or the position of the stream when it was last idle (a
	 * poll that returned no change), whichever is the latest, or, before both, the token this worker was started
	 * from. An event the worker has read but that was not dispatched (the worker was stopped meanwhile) comes after
	 * this position.
	 *
	 * @return A copy of the resume token, or null for a worker started without a token that has neither dispatched an
	 *         event nor seen its stream idle yet (the driver gives no token for a stream just opened)
	 */
	public BsonDocument getResumeToken() {
		final BsonDocument token = this.dispatchedToken;
		return token == null ? null : token.clone();
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
