package test.atriasoft.archidata.cron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.atriasoft.archidata.cron.CronExecutionListener;
import org.atriasoft.archidata.cron.CronScheduler;
import org.atriasoft.archidata.cron.CronTriggerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for the event-driven wake-up of {@link CronScheduler#triggerWakeup(String)}.
 *
 * <p>These tests use a short grace window so they run fast, and synchronize on observable effects
 * (latches / counters) rather than fixed sleeps wherever possible to limit flakiness.
 */
public class TestCronWakeup {

	private static final String TASK = "wakeup-task";
	/** Short debounce window for fast tests. */
	private static final long GRACE_MS = 150L;

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
		local.setEventGraceMillis(GRACE_MS);
		local.start();
		this.scheduler = local;
		return local;
	}

	/** Spin-waits (capped) until {@code condition} holds; returns whether it became true. */
	private static boolean waitUntil(final BooleanSupplier condition, final long timeoutMs) {
		final long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return true;
			}
			try {
				Thread.sleep(5L);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return condition.getAsBoolean();
	}

	@Test
	@Timeout(10)
	public void wakeupRunsKnownTaskImmediately() {
		final CronScheduler s = startedScheduler();
		final CountDownLatch ran = new CountDownLatch(1);
		// Never matches on the cron clock during the test: only the wake-up can fire it.
		s.addTask(TASK, "0 0 1 1 1", ran::countDown);

		s.triggerWakeup(TASK);

		Assertions.assertTrue(awaitLatch(ran, 3_000),
				"the task should have been executed almost immediately after triggerWakeup");
	}

	@Test
	@Timeout(10)
	public void unknownTaskIsNoOp() {
		final CronScheduler s = startedScheduler();
		final AtomicInteger runs = new AtomicInteger();
		s.addTask(TASK, "0 0 1 1 1", runs::incrementAndGet);

		// Must not throw and must not run anything.
		Assertions.assertDoesNotThrow(() -> s.triggerWakeup("does-not-exist"));
		Assertions.assertDoesNotThrow(() -> s.triggerWakeup(null));
		Assertions.assertDoesNotThrow(() -> s.triggerWakeup(""));

		// Give any (erroneous) scheduling a chance to fire.
		sleep(GRACE_MS * 3);
		Assertions.assertEquals(0, runs.get(), "no task should run for an unknown / empty name");
	}

	@Test
	@Timeout(10)
	public void burstIsDebouncedToAtMostTwoRuns() {
		final CronScheduler s = startedScheduler();
		final AtomicInteger runs = new AtomicInteger();
		s.addTask(TASK, "0 0 1 1 1", runs::incrementAndGet);

		// 500 wake-ups within the grace window: 1 leading + 1 coalesced trailing run, never 500.
		for (int i = 0; i < 500; i++) {
			s.triggerWakeup(TASK);
		}

		// Wait for the leading run, then for the trailing window to elapse and settle.
		Assertions.assertTrue(waitUntil(() -> runs.get() >= 1, 3_000), "the leading run must happen");
		sleep(GRACE_MS * 4);

		final int total = runs.get();
		Assertions.assertTrue(total >= 1 && total <= 2,
				"a burst must collapse to at most 2 runs (leading + trailing), got " + total);
	}

	@Test
	@Timeout(10)
	public void wakeupWhileRunningSchedulesExactlyOneRerun() {
		final CronScheduler s = startedScheduler();
		final AtomicInteger runs = new AtomicInteger();
		final CountDownLatch firstStarted = new CountDownLatch(1);
		final CountDownLatch gate = new CountDownLatch(1);

		s.addTask(TASK, "0 0 1 1 1", () -> {
			final int n = runs.incrementAndGet();
			if (n == 1) {
				// Hold the consumer busy on the first run so further wake-ups land while running.
				firstStarted.countDown();
				awaitLatch(gate, 5_000);
			}
		});

		// Leading edge -> starts run #1, which blocks on the gate.
		s.triggerWakeup(TASK);
		Assertions.assertTrue(awaitLatch(firstStarted, 3_000), "run #1 should have started");

		// Several wake-ups while run #1 is still executing: must coalesce into a single rerun.
		for (int i = 0; i < 10; i++) {
			s.triggerWakeup(TASK);
		}

		// Let run #1 finish; the coalesced rerun must then fire exactly once.
		gate.countDown();
		Assertions.assertTrue(waitUntil(() -> runs.get() >= 2, 3_000),
				"exactly one rerun must execute after the busy run");

		// Ensure it does not keep re-firing.
		sleep(GRACE_MS * 4);
		Assertions.assertEquals(2, runs.get(),
				"wake-ups during a run must produce exactly one rerun, got " + runs.get());
	}

	@Test
	@Timeout(10)
	public void spacedWakeupsRunEachTime() {
		final CronScheduler s = startedScheduler();
		final AtomicInteger runs = new AtomicInteger();
		s.addTask(TASK, "0 0 1 1 1", runs::incrementAndGet);

		s.triggerWakeup(TASK);
		Assertions.assertTrue(waitUntil(() -> runs.get() == 1, 3_000), "first wake-up should run");

		// Wait beyond the grace window so the next wake-up is a fresh leading edge.
		sleep(GRACE_MS * 3);
		s.triggerWakeup(TASK);
		Assertions.assertTrue(waitUntil(() -> runs.get() == 2, 3_000),
				"a wake-up after the grace window should run again");
	}

	@Test
	@Timeout(10)
	public void removedTaskNoLongerWakes() {
		final CronScheduler s = startedScheduler();
		final AtomicInteger runs = new AtomicInteger();
		s.addTask(TASK, "0 0 1 1 1", runs::incrementAndGet);

		s.triggerWakeup(TASK);
		Assertions.assertTrue(waitUntil(() -> runs.get() == 1, 3_000), "first wake-up should run");

		s.removeTask(TASK);
		sleep(GRACE_MS * 3); // clear the grace window
		s.triggerWakeup(TASK);

		sleep(GRACE_MS * 4);
		Assertions.assertEquals(1, runs.get(), "a removed task must not be woken up anymore");
	}

	@Test
	@Timeout(10)
	public void executionListenerReceivesTriggerAndSuccess() {
		final CronScheduler s = startedScheduler();
		final List<CronTriggerType> triggers = Collections.synchronizedList(new ArrayList<>());
		final List<Boolean> outcomes = Collections.synchronizedList(new ArrayList<>());
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				triggers.add(trigger);
				return "handle-" + taskName;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {
				outcomes.add(success);
			}
		});
		s.addTask(TASK, "0 0 1 1 1", () -> {});

		s.triggerWakeup(TASK);

		Assertions.assertTrue(waitUntil(() -> !outcomes.isEmpty(), 3_000), "onEnd must be called");
		Assertions.assertEquals(CronTriggerType.WAKEUP, triggers.get(0), "a wake-up run reports WAKEUP");
		Assertions.assertEquals(Boolean.TRUE, outcomes.get(0), "a clean run reports success");
	}

	@Test
	@Timeout(10)
	public void executionListenerReportsFailure() {
		final CronScheduler s = startedScheduler();
		final List<Boolean> outcomes = Collections.synchronizedList(new ArrayList<>());
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				return null;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {
				outcomes.add(success);
			}
		});
		s.addTask(TASK, "0 0 1 1 1", () -> {
			throw new IllegalStateException("boom");
		});

		s.triggerWakeup(TASK);

		Assertions.assertTrue(waitUntil(() -> !outcomes.isEmpty(), 3_000), "onEnd must be called even on throw");
		Assertions.assertEquals(Boolean.FALSE, outcomes.get(0), "a throwing run reports failure (no rethrow)");
	}

	@Test
	public void setEventGraceMillisValidatesInput() {
		final CronScheduler s = new CronScheduler();
		s.setEventGraceMillis(500L);
		Assertions.assertEquals(500L, s.getEventGraceMillis());
		Assertions.assertThrows(IllegalArgumentException.class, () -> s.setEventGraceMillis(0L));
		Assertions.assertThrows(IllegalArgumentException.class, () -> s.setEventGraceMillis(-1L));
		// Unchanged after the invalid attempts.
		Assertions.assertEquals(500L, s.getEventGraceMillis());
	}

	@Test
	@Timeout(10)
	public void wakeupWhileRunningReportsRerunToListener() {
		final CronScheduler s = startedScheduler();
		final List<CronTriggerType> triggers = Collections.synchronizedList(new ArrayList<>());
		final AtomicInteger runs = new AtomicInteger();
		final CountDownLatch firstStarted = new CountDownLatch(1);
		final CountDownLatch gate = new CountDownLatch(1);
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				triggers.add(trigger);
				return null;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {}
		});
		s.addTask(TASK, "0 0 1 1 1", () -> {
			if (runs.incrementAndGet() == 1) {
				firstStarted.countDown();
				awaitLatch(gate, 5_000);
			}
		});

		// Leading edge -> run #1 (WAKEUP), held busy on the gate.
		s.triggerWakeup(TASK);
		Assertions.assertTrue(awaitLatch(firstStarted, 3_000), "run #1 should have started");
		// Wake-ups while busy -> a single coalesced rerun.
		for (int i = 0; i < 10; i++) {
			s.triggerWakeup(TASK);
		}
		gate.countDown();

		Assertions.assertTrue(waitUntil(() -> triggers.size() >= 2, 3_000), "the rerun must also be reported");
		Assertions.assertEquals(CronTriggerType.WAKEUP, triggers.get(0), "the first run is a leading wake-up");
		Assertions.assertEquals(CronTriggerType.RERUN, triggers.get(1),
				"a run coalesced from wake-ups during execution reports RERUN");
	}

	@Test
	@Timeout(10)
	public void immediateTaskRunsWithoutWaitingForTheMinute() {
		final CronScheduler s = startedScheduler();
		final List<CronTriggerType> triggers = Collections.synchronizedList(new ArrayList<>());
		final CountDownLatch ran = new CountDownLatch(1);
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				triggers.add(trigger);
				return null;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {}
		});
		// addTask(Runnable) is a one-time "now" task: it must wake the producer and run within seconds,
		// not wait up to a minute for the next producer tick. It is reported as a SCHEDULED run.
		s.addTask(ran::countDown);

		Assertions.assertTrue(awaitLatch(ran, 2_000),
				"an immediate task must run promptly, without waiting for the minute boundary");
		Assertions.assertTrue(waitUntil(() -> !triggers.isEmpty(), 2_000), "onStart must be called");
		Assertions.assertEquals(CronTriggerType.SCHEDULED, triggers.get(0),
				"a task fired by the timetable reports SCHEDULED");
	}

	@Test
	@Timeout(10)
	public void listenerHandleIsPassedFromStartToEnd() {
		final CronScheduler s = startedScheduler();
		final Object token = new Object();
		final List<Object> received = Collections.synchronizedList(new ArrayList<>());
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				return token;
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {
				received.add(handle);
			}
		});
		s.addTask(TASK, "0 0 1 1 1", () -> {});

		s.triggerWakeup(TASK);

		Assertions.assertTrue(waitUntil(() -> !received.isEmpty(), 3_000), "onEnd must be called");
		Assertions.assertSame(token, received.get(0), "onEnd must receive the exact handle returned by onStart");
	}

	@Test
	@Timeout(10)
	public void faultyListenerDoesNotBreakExecution() {
		final CronScheduler s = startedScheduler();
		final CountDownLatch ran = new CountDownLatch(1);
		// Both callbacks throw: the scheduler must swallow them and still run the task.
		s.setExecutionListener(new CronExecutionListener() {
			@Override
			public Object onStart(final String taskName, final CronTriggerType trigger) {
				throw new IllegalStateException("onStart boom");
			}

			@Override
			public void onEnd(final Object handle, final boolean success) {
				throw new IllegalStateException("onEnd boom");
			}
		});
		s.addTask(TASK, "0 0 1 1 1", ran::countDown);

		s.triggerWakeup(TASK);

		Assertions.assertTrue(awaitLatch(ran, 3_000), "the task must run even when the listener throws");
	}

	@Test
	public void triggerOnStoppedSchedulerDoesNotThrow() {
		final CronScheduler s = new CronScheduler();
		s.setEventGraceMillis(GRACE_MS);
		s.addTask(TASK, "0 0 1 1 1", () -> {});
		// Not started: no consumer/timer. Must be safe.
		Assertions.assertDoesNotThrow(() -> s.triggerWakeup(TASK));
		Assertions.assertDoesNotThrow(() -> s.triggerWakeup("unknown"));
	}

	private static boolean awaitLatch(final CountDownLatch latch, final long timeoutMs) {
		try {
			return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static void sleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
