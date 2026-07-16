package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cron scheduler with producer/consumer threads.
 */
public class CronScheduler {
	/** Constructs a new CronScheduler with empty task maps and queue. */
	public CronScheduler() {
		// default constructor
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CronScheduler.class);
	private final Map<String, CronTask> cronTasks = new ConcurrentHashMap<>();
	private final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
	private final BlockingQueue<PendingTask> queue = new LinkedBlockingQueue<>();
	/**
	 * Wake signal for the producer thread. Released whenever a one-time / immediate task is added so the
	 * producer rescans at once instead of sleeping until the next minute boundary.
	 */
	private final Semaphore producerWake = new Semaphore(0);

	/**
	 * A task queued for execution, tagged with what triggered it. Equality is by task (name + type,
	 * via the task's own {@code equals}), ignoring the trigger, so the {@code uniqueInQueue} dedup keeps
	 * working regardless of trigger.
	 */
	private record PendingTask(
			Task task,
			CronTriggerType trigger) {
		@Override
		public boolean equals(final Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof final PendingTask casted)) {
				return false;
			}
			return Objects.equals(this.task, casted.task);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.task);
		}
	}

	private ExecutorService producerThread;
	private ExecutorService consumerThread;
	private Integer gracePeriodMinutes = null;

	/** Default minimum spacing between two event-driven executions of the same task. */
	private static final long DEFAULT_EVENT_GRACE_MILLIS = 15_000L;
	/** Minimum spacing between two event-driven executions of the same task (debounce window). */
	private long eventGraceMillis = DEFAULT_EVENT_GRACE_MILLIS;
	/** Last time (epoch ms) a task was enqueued because of a wake-up event, keyed by task name. */
	private final Map<String, Long> lastEventEnqueueMillis = new ConcurrentHashMap<>();
	/** Names of tasks currently being executed by the consumer thread. */
	private final Set<String> runningTaskNames = ConcurrentHashMap.newKeySet();
	/** Names of tasks that have a trailing (coalesced) wake-up run already scheduled. */
	private final Set<String> trailingScheduledNames = ConcurrentHashMap.newKeySet();
	/** Guards the wake-up bookkeeping so concurrent HTTP threads coalesce deterministically. */
	private final Object wakeupLock = new Object();
	/** One-shot timer used to fire the trailing (coalesced) wake-up runs. */
	private ScheduledExecutorService rerunTimer;
	/** Optional lifecycle listener notified around every task execution. */
	private CronExecutionListener executionListener;

	/**
	 * Registers a lifecycle listener notified before and after every task execution. Set once, at
	 * scheduler initialization. Pass {@code null} to clear.
	 *
	 * @param listener the listener, or {@code null}
	 */
	public void setExecutionListener(final CronExecutionListener listener) {
		this.executionListener = listener;
	}

	/**
	 * Sets a grace period (in minutes) to delay task execution after scheduler start.
	 *
	 * @param minutes the number of minutes to wait before processing tasks, or {@code null} to disable
	 */
	public void setGracePeriodMinutes(final Integer minutes) {
		if (minutes == null || minutes <= 0) {
			disableGracePeriodMinutes();
			return;
		}
		this.gracePeriodMinutes = minutes;
	}

	/**
	 * Disable the grace period.
	 */
	public void disableGracePeriodMinutes() {
		this.gracePeriodMinutes = null;
	}

	/**
	 * Starts the scheduler with a producer thread that scans for due tasks
	 * and a consumer thread that executes them.
	 */
	public synchronized void start() {
		if (this.producerThread != null && !this.producerThread.isShutdown()) {
			return;
		}
		this.producerThread = Executors.newSingleThreadExecutor();
		this.consumerThread = Executors.newSingleThreadExecutor();
		this.rerunTimer = Executors.newSingleThreadScheduledExecutor();
		this.producerThread.submit((Runnable) this::producerThread);
		this.consumerThread.submit((Runnable) this::consumerThread);
	}

	private void producerThread() {
		LOGGER.debug("Start CRON producer thread");
		// Step 1: wait for grace period
		if (this.gracePeriodMinutes == null) {
			LOGGER.debug("grace period disabled");
		} else {
			LOGGER.debug("grace period [BEGIN]");
			if (this.gracePeriodMinutes > 0) {
				try {
					Thread.sleep(this.gracePeriodMinutes * 60_000L);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			LOGGER.debug("grace period [ END ]");
		}
		// Step 2: normal scheduling loop
		int lastMinute = -1;
		while (!Thread.currentThread().isInterrupted()) {
			final LocalDateTime now = LocalDateTime.now();
			// One-time / immediate tasks fire as soon as their time is reached. They are checked on every
			// loop pass (not only on a minute change) so a task added at runtime via addTask(Runnable) or
			// addTask(name, executeAt, action) runs at once after a wake signal, never waiting the minute.
			processScheduledTasks(now);
			// Recurring cron tasks only change state at minute granularity: evaluate them once per minute.
			final int currentMinute = now.getMinute();
			if (currentMinute != lastMinute) {
				lastMinute = currentMinute;
				processCronTasks(now);
			}
			// Sleep up to a second, but return immediately if a new immediate task was just added.
			try {
				this.producerWake.tryAcquire(1, TimeUnit.SECONDS);
				this.producerWake.drainPermits();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// WARN for the same reason as the consumer: a silently stopped producer means no
		// periodic task is ever enqueued again.
		LOGGER.warn("Stop CRON producer thread");
	}

	/** Enqueues every one-time scheduled task whose execution time has been reached. */
	private void processScheduledTasks(final LocalDateTime now) {
		for (final ScheduledTask task : this.scheduledTasks.values()) {
			if (now.isBefore(task.executeAt())) {
				continue;
			}
			// Remove first: a concurrent rescan that loses the race sees nothing and cannot double-enqueue.
			if (this.scheduledTasks.remove(task.name()) != null) {
				LOGGER.info("Add scheduled task '{}' to queue", task.name());
				this.queue.offer(new PendingTask(task, CronTriggerType.SCHEDULED));
			}
		}
	}

	/** Enqueues every recurring cron task matching the current minute, honouring {@code uniqueInQueue}. */
	private void processCronTasks(final LocalDateTime now) {
		for (final CronTask task : this.cronTasks.values()) {
			if (!task.matches(now)) {
				continue;
			}
			final PendingTask pending = new PendingTask(task, CronTriggerType.SCHEDULED);
			if (task.uniqueInQueue()) {
				if (!this.queue.contains(pending)) {
					LOGGER.info("Add Unique Task in Queue: {}", task.name());
					this.queue.offer(pending);
				} else {
					LOGGER.info("Reject Unique Task in Queue: {} (already added)", task.name());
				}
			} else {
				LOGGER.info("Add Task in Queue: {}", task.name());
				this.queue.offer(pending);
			}
		}
	}

	/** Wakes the producer thread so it rescans immediately instead of waiting for the next loop tick. */
	private void signalProducer() {
		this.producerWake.release();
	}

	private void consumerThread() {
		LOGGER.debug("Start CRON consumer thread");
		while (!Thread.currentThread().isInterrupted()) {
			final PendingTask pending;
			try {
				pending = this.queue.take();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			try {
				runTask(pending);
			} catch (final Throwable ex) {
				// Belt and braces: runTask already contains everything, but NOTHING may kill
				// this loop — a dead consumer freezes every periodic task silently (the
				// producer keeps rejecting re-enqueues of the never-consumed pending tasks).
				LOGGER.error("CRON consumer survived an unexpected error around task '{}': {}", pending.task().name(),
						ex.getMessage(), ex);
			}
		}
		// WARN: outside of an explicit stop() this must be visible — a stopped consumer
		// means no periodic task will ever run again in this process.
		LOGGER.warn("Stop CRON consumer thread");
	}

	/** Runs one task: tracks it, notifies the listener around it, and captures the outcome. */
	private void runTask(final PendingTask pending) {
		final Task task = pending.task();
		final String name = task.name();
		LOGGER.info("CRON consume task: '{}'", name);
		this.runningTaskNames.add(name);
		final Object handle = notifyStart(name, pending.trigger());
		final long start = System.currentTimeMillis();
		Throwable failure = null;
		try {
			task.action().run();
		} catch (final Throwable ex) {
			// Throwable, not Exception: an Error (OutOfMemoryError during a heavy task, …)
			// must be recorded as a failure and MUST NOT propagate — it would kill the
			// consumer loop through the executor's FutureTask without any log, silently
			// freezing every periodic task while the rest of the process keeps running.
			failure = ex;
			LOGGER.error("Fail in CRON consumer throw in task '{}': {}", name, ex.getMessage(), ex);
		} finally {
			this.runningTaskNames.remove(name);
			notifyEnd(handle, failure);
			final long duration = System.currentTimeMillis() - start;
			if (duration > 120_000) { // 2 minutes
				LOGGER.error("Task '{}' executed in {} ms took too long! > 2 minutes", name, duration);
			} else {
				LOGGER.debug("Task '{}' executed in {} ms", name, duration);
			}
		}
	}

	/** Notifies the listener that a task is starting; returns its handle (or null). Never throws. */
	private Object notifyStart(final String name, final CronTriggerType trigger) {
		final CronExecutionListener listener = this.executionListener;
		if (listener == null) {
			return null;
		}
		try {
			return listener.onStart(name, trigger);
		} catch (final Exception ex) {
			LOGGER.error("CRON execution listener onStart failed for '{}': {}", name, ex.getMessage(), ex);
			return null;
		}
	}

	/** Notifies the listener that a task finished. Never throws. */
	private void notifyEnd(final Object handle, final Throwable failure) {
		final CronExecutionListener listener = this.executionListener;
		if (listener == null) {
			return;
		}
		try {
			listener.onEnd(handle, failure == null, failure);
		} catch (final Exception ex) {
			LOGGER.error("CRON execution listener onEnd failed: {}", ex.getMessage(), ex);
		}
	}

	/**
	 * Stops the scheduler by shutting down both the producer and consumer threads
	 * and clearing the task queue.
	 */
	public synchronized void stop() {
		LOGGER.debug("Request STOP CRON");
		if (this.producerThread != null) {
			this.producerThread.shutdownNow();
		}
		this.queue.clear();
		if (this.consumerThread != null) {
			this.consumerThread.shutdownNow();
		}
		if (this.rerunTimer != null) {
			this.rerunTimer.shutdownNow();
		}
		this.trailingScheduledNames.clear();
		this.runningTaskNames.clear();
	}

	/**
	 * Adds a recurring cron task with a cron expression schedule.
	 *
	 * @param name task name (generated if null or empty)
	 * @param cronExpression cron expression ("minute hour dayOfMonth month dayOfWeek")
	 *        minute(0-59) hour(0-23) dayOfMonth(1-31) month(1-12) dayOfWeek(1-7).
	 *        Value formats: "*" (all), "*\/5" (every 5), "1-5" (range), "1,5,6" (list),
	 *        "1-5,10,20-22" (combined)
	 * @param action the action to execute
	 * @param uniqueInQueue if {@code true}, only one pending instance in queue
	 * @throws IllegalArgumentException if the cron expression is invalid
	 */
	public void addTask(String name, final String cronExpression, final Runnable action, final boolean uniqueInQueue)
			throws IllegalArgumentException {
		if (name == null || name.isEmpty()) {
			name = new ObjectId().toString();
		}
		LOGGER.debug("Add task name '{}' cron='{}'", name, cronExpression);
		validateCronExpression(cronExpression);
		this.cronTasks.put(name, new CronTask(name, cronExpression, action, uniqueInQueue));
	}

	/**
	 * Adds a cron task with the default unique-in-queue behavior (unique).
	 *
	 * @param name the task name (generated if null or empty)
	 * @param cronExpression the cron expression defining the schedule
	 * @param action the action to execute
	 */
	public void addTask(final String name, final String cronExpression, final Runnable action) {
		addTask(name, cronExpression, action, true);
	}

	/**
	 * Adds a one-time scheduled task to execute at the specified date and time.
	 *
	 * @param name the task name (generated if null or empty)
	 * @param executeAt the date and time at which the task should execute (must be in the future)
	 * @param action the action to execute
	 * @throws IllegalArgumentException if executeAt is in the past
	 */
	public void addTask(String name, final LocalDateTime executeAt, final Runnable action) {
		if (executeAt == null || executeAt.isBefore(LocalDateTime.now().minusMinutes(1))) {
			throw new IllegalArgumentException("Execution time must be in the future (1 minute delta)");
		}
		if (name == null || name.isEmpty()) {
			name = new ObjectId().toString();
		}
		this.scheduledTasks.put(name, new ScheduledTask(name, executeAt, action));
		LOGGER.info("Add scheduled task '{}' at {}", name, executeAt);
		signalProducer();
	}

	/**
	 * Adds an immediate one-time task that executes as soon as possible.
	 *
	 * @param action the action to execute
	 */
	public void addTask(final Runnable action) {
		final String name = new ObjectId().toString();
		final LocalDateTime date = LocalDateTime.now();
		this.scheduledTasks.put(name, new ScheduledTask(name, date, action));
		LOGGER.info("Add scheduled task '{}' at (now)", name, date);
		signalProducer();
	}

	/**
	 * Removes a cron task by name.
	 *
	 * @param name the name of the task to remove
	 */
	public void removeTask(final String name) {
		LOGGER.debug("remove task name '{}'", name);
		this.cronTasks.remove(name);
	}

	/**
	 * Sets the debounce window between two event-driven executions of the same task.
	 *
	 * <p>A burst of {@link #triggerWakeup(String)} calls within this window collapses to a single
	 * trailing execution fired once the window elapses; while the window is open no extra run is
	 * enqueued. Defaults to {@value #DEFAULT_EVENT_GRACE_MILLIS} ms.
	 *
	 * @param millis the debounce window in milliseconds (must be strictly positive)
	 * @throws IllegalArgumentException if {@code millis <= 0}
	 */
	public void setEventGraceMillis(final long millis) {
		if (millis <= 0) {
			throw new IllegalArgumentException("eventGraceMillis must be > 0");
		}
		this.eventGraceMillis = millis;
	}

	/**
	 * Returns the current event debounce window in milliseconds.
	 *
	 * @return the debounce window in milliseconds
	 */
	public long getEventGraceMillis() {
		return this.eventGraceMillis;
	}

	/**
	 * Wakes up a named {@link CronTask} immediately, outside of its cron schedule.
	 *
	 * <p>The call is safe to invoke from any thread (e.g. HTTP request threads). The task is never
	 * executed in the calling thread: only the single consumer thread runs actions, so executions
	 * stay serialized. Behaviour:
	 * <ul>
	 *   <li><b>Leading edge</b>: if the task was not woken within the last {@link #getEventGraceMillis()}
	 *       ms and is not currently running, it is enqueued at once.</li>
	 *   <li><b>Debounce</b>: a burst of calls within the grace window collapses into a single trailing
	 *       run fired when the window elapses — so 200 events/second cause at most one extra run.</li>
	 *   <li><b>Busy</b>: if the task is currently running, a single rerun is scheduled and fired after
	 *       it completes (plus the grace window), guaranteeing the latest event is honoured.</li>
	 * </ul>
	 * Respects the task's {@code uniqueInQueue} flag and is a no-op for an unknown name.
	 *
	 * @param taskName the name of the {@link CronTask} to wake up
	 */
	public void triggerWakeup(final String taskName) {
		if (taskName == null || taskName.isEmpty()) {
			return;
		}
		final CronTask task = this.cronTasks.get(taskName);
		if (task == null) {
			LOGGER.debug("triggerWakeup: unknown task '{}' (no-op)", taskName);
			return;
		}
		synchronized (this.wakeupLock) {
			final long now = System.currentTimeMillis();
			final long last = this.lastEventEnqueueMillis.getOrDefault(taskName, 0L);
			final boolean outsideGrace = now - last >= this.eventGraceMillis;
			if (outsideGrace && !this.runningTaskNames.contains(taskName)) {
				// Leading edge: free to run now.
				this.lastEventEnqueueMillis.put(taskName, now);
				enqueueWakeup(task, CronTriggerType.WAKEUP);
			} else {
				// Within the grace window or currently running: coalesce into one trailing run.
				scheduleTrailing(taskName);
			}
		}
	}

	/** Schedules a single trailing wake-up run for the task. Must be called while holding {@link #wakeupLock}. */
	private void scheduleTrailing(final String taskName) {
		if (this.rerunTimer == null || this.rerunTimer.isShutdown()) {
			// Scheduler not started: nothing to fire later. Leading-edge enqueue already covered the rest.
			return;
		}
		if (!this.trailingScheduledNames.add(taskName)) {
			// A trailing run is already pending for this task: coalesce.
			return;
		}
		this.rerunTimer.schedule(() -> fireTrailing(taskName), this.eventGraceMillis, TimeUnit.MILLISECONDS);
	}

	/** Fires (or re-arms) a trailing wake-up run once its grace window has elapsed. */
	private void fireTrailing(final String taskName) {
		synchronized (this.wakeupLock) {
			this.trailingScheduledNames.remove(taskName);
			final CronTask task = this.cronTasks.get(taskName);
			if (task == null) {
				// Task removed while a trailing run was pending.
				return;
			}
			if (this.runningTaskNames.contains(taskName)) {
				// Still running: re-arm a trailing run for after it completes (+ grace).
				scheduleTrailing(taskName);
				return;
			}
			this.lastEventEnqueueMillis.put(taskName, System.currentTimeMillis());
			enqueueWakeup(task, CronTriggerType.RERUN);
		}
	}

	/** Enqueues a task for a wake-up, honouring its {@code uniqueInQueue} flag. Non-blocking. */
	private void enqueueWakeup(final CronTask task, final CronTriggerType trigger) {
		final PendingTask pending = new PendingTask(task, trigger);
		if (task.uniqueInQueue() && this.queue.contains(pending)) {
			LOGGER.debug("triggerWakeup: '{}' already queued, skip", task.name());
			return;
		}
		LOGGER.info("triggerWakeup: enqueue '{}'", task.name());
		this.queue.offer(pending);
	}

	private void validateCronExpression(final String expr) throws IllegalArgumentException {
		final String[] parts = expr.split(" ");
		if (parts.length != 5) {
			throw new IllegalArgumentException("Cron expression must have 5 fields (minute hour day month dayOfWeek)");
		}

		// ranges for fields: minute(0-59), hour(0-23), day(1-31), month(1-12), dayOfWeek(1-7)
		final int[][] limits = { { 0, 59 }, // minute
				{ 0, 23 }, // hour
				{ 1, 31 }, // day
				{ 1, 12 }, // month
				{ 1, 7 } // dayOfWeek (1=Monday..7=Sunday)
		};

		for (int i = 0; i < parts.length; i++) {
			final String field = parts[i];
			final int min = limits[i][0];
			final int max = limits[i][1];

			validateField(field, min, max, i);
		}
	}

	private void validateField(final String field, final int min, final int max, final int position)
			throws IllegalArgumentException {
		final String fieldName = switch (position) {
			case 0 -> "minute";
			case 1 -> "hour";
			case 2 -> "day of month";
			case 3 -> "month";
			case 4 -> "day of week";
			default -> "unknown";
		};

		if (field.equals("*")) {
			return;
		}

		for (final String part : field.split(",")) {
			if (part.startsWith("*/")) {
				final int step = Integer.parseInt(part.substring(2));
				if (step <= 0) {
					throw new IllegalArgumentException("Invalid step in " + fieldName + ": " + part);
				}
			} else if (part.contains("-")) {
				final String[] range = part.split("-");
				if (range.length != 2) {
					throw new IllegalArgumentException("Invalid range in " + fieldName + ": " + part);
				}
				final int start = Integer.parseInt(range[0]);
				final int end = Integer.parseInt(range[1]);
				if (start > end || start < min || end > max) {
					throw new IllegalArgumentException(
							"Range out of bounds in " + fieldName + ": " + part + " (valid: " + min + "-" + max + ")");
				}
			} else {
				final int value = Integer.parseInt(part);
				if (value < min || value > max) {
					throw new IllegalArgumentException(
							"Value out of bounds in " + fieldName + ": " + part + " (valid: " + min + "-" + max + ")");
				}
			}
		}
	}

}
