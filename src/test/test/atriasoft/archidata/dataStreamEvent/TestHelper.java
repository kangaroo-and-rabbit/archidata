package test.atriasoft.archidata.dataStreamEvent;

import java.util.List;
import java.util.function.Supplier;

import org.atriasoft.archidata.dataStreamEvent.ChangeEvent;

/**
 * Helper class for testing with active waiting instead of Thread.sleep()
 */
public class TestHelper {

	/**
	 * Wait actively for events to arrive with polling
	 *
	 * @param events The list of events to monitor
	 * @param expectedCount Expected number of events
	 * @param timeoutMs Total timeout in milliseconds
	 * @param pollIntervalMs Polling interval in milliseconds
	 * @throws InterruptedException if interrupted
	 * @throws AssertionError if timeout is reached
	 */
	public static void waitForEvents(
			final List<ChangeEvent> events,
			final int expectedCount,
			final long timeoutMs,
			final long pollIntervalMs) throws InterruptedException {
		final long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			synchronized (events) {
				if (events.size() >= expectedCount) {
					return;
				}
			}
			Thread.sleep(pollIntervalMs);
		}
		synchronized (events) {
			throw new AssertionError(
					String.format("Timeout waiting for events. Expected: %d, Got: %d", expectedCount, events.size()));
		}
	}

	/**
	 * Wait actively for events to arrive with default polling interval (10ms)
	 *
	 * @param events The list of events to monitor
	 * @param expectedCount Expected number of events
	 * @param timeoutMs Total timeout in milliseconds
	 * @throws InterruptedException if interrupted
	 * @throws AssertionError if timeout is reached
	 */
	public static void waitForEvents(final List<ChangeEvent> events, final int expectedCount, final long timeoutMs)
			throws InterruptedException {
		waitForEvents(events, expectedCount, timeoutMs, 10);
	}

	/**
	 * Wait for a condition to become true with active polling
	 *
	 * @param condition Condition supplier to check
	 * @param timeoutMs Total timeout in milliseconds
	 * @param pollIntervalMs Polling interval in milliseconds
	 * @param errorMessage Error message if timeout
	 * @throws InterruptedException if interrupted
	 * @throws AssertionError if timeout is reached
	 */
	public static void waitForCondition(
			final Supplier<Boolean> condition,
			final long timeoutMs,
			final long pollIntervalMs,
			final String errorMessage) throws InterruptedException {
		final long startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime < timeoutMs) {
			if (condition.get()) {
				return;
			}
			Thread.sleep(pollIntervalMs);
		}
		throw new AssertionError(errorMessage);
	}

	/**
	 * Wait for a condition to become true with default polling interval (10ms)
	 *
	 * @param condition Condition supplier to check
	 * @param timeoutMs Total timeout in milliseconds
	 * @param errorMessage Error message if timeout
	 * @throws InterruptedException if interrupted
	 * @throws AssertionError if timeout is reached
	 */
	public static void waitForCondition(
			final Supplier<Boolean> condition,
			final long timeoutMs,
			final String errorMessage) throws InterruptedException {
		waitForCondition(condition, timeoutMs, 10, errorMessage);
	}

	/**
	 * Wait for the change stream to initialize (small delay but with active
	 * verification)
	 *
	 * @param manager The notification manager
	 * @param collectionName Collection name to check
	 * @param timeoutMs Timeout in milliseconds
	 * @throws InterruptedException if interrupted
	 */
	public static void waitForStreamInitialization(
			final org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager manager,
			final String collectionName,
			final long timeoutMs) throws InterruptedException {
		waitForCondition(() -> manager.isWatching(collectionName), timeoutMs,
				"Change stream for collection '" + collectionName + "' did not initialize in time");
		// Additional small delay for stream to be fully ready
		Thread.sleep(100);
	}
}
