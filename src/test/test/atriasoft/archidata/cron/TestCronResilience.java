package test.atriasoft.archidata.cron;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.atriasoft.archidata.cron.CronExecutionListener;
import org.atriasoft.archidata.cron.CronScheduler;
import org.atriasoft.archidata.cron.CronTriggerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * The consumer thread must survive ANY throwable from a task — including {@link Error}s. A dead
 * consumer freezes every periodic task silently: the producer keeps rejecting re-enqueues of the
 * never-consumed pending tasks while the rest of the process (HTTP, …) looks perfectly healthy.
 * This exact failure was observed in production with an {@code OutOfMemoryError} thrown by a
 * backup task: the run was even recorded as SUCCESS because only {@code Exception} was caught.
 */
public class TestCronResilience {

	private CronScheduler scheduler;

	@AfterEach
	public void tearDown() {
		if (this.scheduler != null) {
			this.scheduler.stop();
			this.scheduler = null;
		}
	}

	private CronScheduler startedScheduler() {
		final CronScheduler local = new CronScheduler();
		local.disableGracePeriodMinutes();
		local.setEventGraceMillis(50L);
		this.scheduler = local;
		return local;
	}

	/** Listener recording each end notification (success flag + failure). */
	private record EndRecord(
			String taskName,
			boolean success,
			Throwable failure) {}

	private static class RecordingListener implements CronExecutionListener {
		final List<EndRecord> ends = new CopyOnWriteArrayList<>();

		@Override
		public Object onStart(final String taskName, final CronTriggerType trigger) {
			return taskName;
		}

		@Override
		public void onEnd(final Object handle, final boolean success) {
			throw new IllegalStateException("the scheduler must call the failure-aware variant");
		}

		@Override
		public void onEnd(final Object handle, final boolean success, final Throwable failure) {
			this.ends.add(new EndRecord((String) handle, success, failure));
		}
	}

	private static boolean awaitLatch(final CountDownLatch latch, final long timeoutMs) {
		try {
			return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	@Test
	@Timeout(10)
	public void consumerSurvivesExceptionAndError() {
		final CronScheduler local = startedScheduler();
		final RecordingListener listener = new RecordingListener();
		local.setExecutionListener(listener);
		local.start();

		final CountDownLatch survivorRan = new CountDownLatch(1);
		// Cron expressions that never match during the test: only wake-ups fire them.
		local.addTask("throws-exception", "0 0 1 1 1", () -> {
			throw new IllegalStateException("boom exception");
		});
		local.addTask("throws-error", "0 0 1 1 1", () -> {
			throw new OutOfMemoryError("boom error (fabricated, no real allocation failure)");
		});
		local.addTask("survivor", "0 0 1 1 1", survivorRan::countDown);

		local.triggerWakeup("throws-exception");
		local.triggerWakeup("throws-error");
		local.triggerWakeup("survivor");

		// The survivor runs LAST on the single consumer thread: if it ran, the consumer
		// survived both the exception and the error.
		Assertions.assertTrue(awaitLatch(survivorRan, 5_000),
				"the consumer thread must survive a task throwing an Exception and a task throwing an Error");

		Assertions.assertEquals(3, listener.ends.size());
		final EndRecord exceptionEnd = listener.ends.get(0);
		Assertions.assertEquals("throws-exception", exceptionEnd.taskName());
		Assertions.assertFalse(exceptionEnd.success(), "a throwing task must not be recorded as a success");
		Assertions.assertInstanceOf(IllegalStateException.class, exceptionEnd.failure());

		final EndRecord errorEnd = listener.ends.get(1);
		Assertions.assertEquals("throws-error", errorEnd.taskName());
		Assertions.assertFalse(errorEnd.success(), "an Error-throwing task must not be recorded as a success");
		Assertions.assertInstanceOf(OutOfMemoryError.class, errorEnd.failure());

		final EndRecord survivorEnd = listener.ends.get(2);
		Assertions.assertTrue(survivorEnd.success());
		Assertions.assertNull(survivorEnd.failure());
	}

	@Test
	@Timeout(10)
	public void legacyListenerKeepsWorkingThroughTheDefaultMethod() {
		final CronScheduler local = startedScheduler();
		final List<Boolean> ends = new CopyOnWriteArrayList<>();
		// Legacy shape: only the two-argument onEnd is implemented.
		local.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				return null;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {
				ends.add(success);
			}
		});
		local.start();

		final CountDownLatch ran = new CountDownLatch(1);
		local.addTask("legacy-fail", "0 0 1 1 1", () -> {
			throw new OutOfMemoryError("boom");
		});
		local.addTask("legacy-ok", "0 0 1 1 1", ran::countDown);
		local.triggerWakeup("legacy-fail");
		local.triggerWakeup("legacy-ok");

		Assertions.assertTrue(awaitLatch(ran, 5_000));
		Assertions.assertEquals(List.of(false, true), ends);
	}
}
