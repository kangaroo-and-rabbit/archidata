package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

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
	private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

	private ExecutorService producerThread;
	private ExecutorService consumerThread;
	private Integer gracePeriodMinutes = null;

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
			try {
				final LocalDateTime now = LocalDateTime.now();
				final int currentMinute = now.getMinute();
				if (currentMinute != lastMinute) {
					lastMinute = currentMinute;

					for (final ScheduledTask task : this.scheduledTasks.values()) {
						if (!this.queue.contains(task) && !now.isBefore(task.executeAt())) {
							LOGGER.info("Add scheduled task '{}' to queue", task.name());
							this.queue.put(task);
							// Remove from scheduledTasks to avoid re-adding
							this.scheduledTasks.remove(task.name());
						}
					}
					// scheduled task:
					for (final CronTask task : this.cronTasks.values()) {
						if (task.matches(now)) {
							if (task.uniqueInQueue()) {
								if (!this.queue.contains(task)) {
									LOGGER.info("Add Unique Task in Queue: {}", task.name());
									this.queue.put(task);
								} else {
									LOGGER.info("Reject Unique Task in Queue: {} (already added)", task.name());
								}
							} else {
								LOGGER.info("Add Task in Queue: {}", task.name());
								this.queue.put(task);
							}
						}
					}
				}
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		LOGGER.debug("Stop CRON producer thread");
	}

	private void consumerThread() {
		LOGGER.debug("Start CRON consumer thread");
		while (!Thread.currentThread().isInterrupted()) {
			try {
				final Task task = this.queue.take();
				LOGGER.info("CRON consume task: '{}'", task.name());
				final long start = System.currentTimeMillis();
				try {
					task.action().run();
				} finally {
					final long duration = System.currentTimeMillis() - start;
					if (duration > 120_000) { // 2 minutes
						LOGGER.error("Task '{}' executed in {} ms took too long! > 2 minutes", task.name(), duration);
					} else {
						LOGGER.debug("Task '{}' executed in {} ms", task.name(), duration);
					}
				}
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (final Exception ex) {
				LOGGER.error("Fail in CRON consumer throw in CronTask: {}", ex.getMessage(), ex);
			}
		}
		LOGGER.debug("Stop CRON consumer thread");
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
