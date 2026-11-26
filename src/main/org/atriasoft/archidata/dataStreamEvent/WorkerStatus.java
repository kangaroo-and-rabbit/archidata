package org.atriasoft.archidata.dataStreamEvent;

/**
 * Represents the current status of a ChangeStreamWorker thread during its
 * lifecycle.
 *
 * <p>
 * Workers transition through these states as they operate:
 * </p>
 * <ol>
 * <li>STARTING - Initial state when worker is being created</li>
 * <li>RUNNING - Worker is actively watching the change stream</li>
 * <li>RECONNECTING - Worker is attempting to reconnect after a MongoDB error
 * (temporary state)</li>
 * <li>ERROR - Worker encountered an unexpected error (may retry)</li>
 * <li>STOPPED - Worker has been stopped, or ended by an {@link Error}, and will not restart</li>
 * </ol>
 *
 * @see ChangeStreamWorker
 */
public enum WorkerStatus {
	/**
	 * Worker is in the process of starting up but not yet watching the change
	 * stream.
	 */
	STARTING,

	/**
	 * Worker is actively listening to the change stream and processing events.
	 */
	RUNNING,

	/**
	 * Worker is reconnecting to MongoDB after a connection error. This is a
	 * temporary state: the worker waits a fixed delay before retrying (1 s after an
	 * interruption, an invalid resume token or a resume position no longer in the
	 * oplog, both then forgotten, 5 s after another MongoDB error),
	 * with no exponential backoff and no retry limit.
	 */
	RECONNECTING,

	/**
	 * Worker encountered an unexpected error. The worker retries after a fixed
	 * 5 s delay, with no retry limit.
	 */
	ERROR,

	/**
	 * Worker has been stopped and will not process any more events. This is a
	 * terminal state. A worker of a collection still watched reports it when an
	 * {@link Error} thrown on its thread (by a listener, an
	 * {@code AssertionError} included) ended it, logged at ERROR: unwatch then
	 * watch the collection to start a new worker.
	 */
	STOPPED
}
