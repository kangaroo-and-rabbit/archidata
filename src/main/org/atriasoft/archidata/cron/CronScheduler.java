package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cron scheduler with producer/consumer threads.
 */
public class CronScheduler {
	private static final Logger LOGGER = LoggerFactory.getLogger(CronScheduler.class);
	private final Map<String, CronTask> tasks = new ConcurrentHashMap<>();
	private final BlockingQueue<CronTask> queue = new LinkedBlockingQueue<>();

	private ExecutorService producerThread;
	private ExecutorService consumerThread;
	private Integer gracePeriodMinutes = null;

	/**
	 * Set a grace period (in minutes) to ignore tasks after start.
	 */
	public void setGracePeriodMinutes(Integer minutes) {
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
	 * Start scheduler: producer scans every 5s, consumer executes.
	 */
	public synchronized void start() {
		if (producerThread != null && !producerThread.isShutdown()) {
			return;
		}

		producerThread = Executors.newSingleThreadExecutor();
		consumerThread = Executors.newSingleThreadExecutor();

		// Producer: scans every 5 seconds
		producerThread.submit(() -> this.producerThread());

		// Consumer: executes tasks
		consumerThread.submit(() -> this.consumerThread());
	}

	private void producerThread() {
		LOGGER.info("Start CRON producer thread");
		// Step 1: wait for grace period
		LOGGER.info("grace period [BEGIN]");
		if (gracePeriodMinutes > 0) {
			try {
				Thread.sleep(gracePeriodMinutes * 60_000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		LOGGER.info("grace period [ END ]");
		// Step 2: normal scheduling loop
		int lastMinute = -1;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				LocalDateTime now = LocalDateTime.now();
				int currentMinute = now.getMinute();
				if (currentMinute != lastMinute) {
					lastMinute = currentMinute;
					for (CronTask task : tasks.values()) {
						if (task.matches(now)) {
							if (task.uniqueInQueue()) {
								if (!queue.contains(task)) {
									LOGGER.info("Add Unique Task in Queue: {}", task.name());
									queue.put(task);
								} else {
									LOGGER.info("Reject Unique Task in Queue: {} (already added)", task.name());
								}
							} else {
								LOGGER.info("Add Task in Queue: {}", task.name());
								queue.put(task);
							}
						}
					}
				}
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		LOGGER.info("Stop CRON producer thread");
	}

	private void consumerThread() {
		LOGGER.info("Start CRON consumer thread");
		while (!Thread.currentThread().isInterrupted()) {
			try {
				CronTask task = queue.take();
				LOGGER.info("CRON consume task: '{}'", task.name());
				task.action().run();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception ex) {
				LOGGER.error("Fail in CRON consumer throw in CronTask: {}", ex.getMessage());
				ex.printStackTrace();
			}
		}
		LOGGER.info("Stop CRON consumer thread");
	}

	/**
	 * Stop scheduler threads.
	 */
	public synchronized void stop() {
		LOGGER.info("Request STOP CRON");
		if (producerThread != null) {
			producerThread.shutdownNow();
		}
		queue.clear();
		if (consumerThread != null) {
			consumerThread.shutdownNow();
		}
	}

	/**
	 * Add a task.
	 * @param name task name
	 * @param cronExpression cron expression ("minute hour day month dayOfWeek")
	 *        With values:
	 *          - "*": All the values.
	 *          - "*\/5": all the 5 unit"
	 *          - "1-5" Values include between 1 and 5 units.
	 *          - "1,5,6": Values equal at 1, 5 and 6.
	 *          - "1-5,10,20-22": 1 à 5 or 10 or 20 à 22.
	 * @param action lambda to execute
	 * @param uniqueInQueue if true, only one pending instance in queue
	 */
	public void addTask(String name, String cronExpression, Runnable action, boolean uniqueInQueue)
			throws IllegalArgumentException {
		LOGGER.info("Add task name '{}' cron='{}'", name, cronExpression);
		validateCronExpression(cronExpression);
		tasks.put(name, new CronTask(name, cronExpression, action, uniqueInQueue));
	}

	/**
	 * Remove a task.
	 */
	public void removeTask(String name) {
		LOGGER.info("remove task name '{}'", name);
		tasks.remove(name);
	}

	private void validateCronExpression(String expr) throws IllegalArgumentException {
		String[] parts = expr.split(" ");
		if (parts.length != 5) {
			throw new IllegalArgumentException("Cron expression must have 5 fields (minute hour day month dayOfWeek)");
		}

		// ranges for fields: minute(0-59), hour(0-23), day(1-31), month(1-12), dayOfWeek(1-7)
		int[][] limits = { { 0, 59 }, // minute
				{ 0, 23 }, // hour
				{ 1, 31 }, // day
				{ 1, 12 }, // month
				{ 1, 7 } // dayOfWeek (1=Monday..7=Sunday)
		};

		for (int i = 0; i < parts.length; i++) {
			String field = parts[i];
			int min = limits[i][0];
			int max = limits[i][1];

			validateField(field, min, max, i);
		}
	}

	private void validateField(String field, int min, int max, int position) throws IllegalArgumentException {
		String fieldName = switch (position) {
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

		for (String part : field.split(",")) {
			if (part.startsWith("*/")) {
				int step = Integer.parseInt(part.substring(2));
				if (step <= 0) {
					throw new IllegalArgumentException("Invalid step in " + fieldName + ": " + part);
				}
			} else if (part.contains("-")) {
				String[] range = part.split("-");
				if (range.length != 2) {
					throw new IllegalArgumentException("Invalid range in " + fieldName + ": " + part);
				}
				int start = Integer.parseInt(range[0]);
				int end = Integer.parseInt(range[1]);
				if (start > end || start < min || end > max) {
					throw new IllegalArgumentException(
							"Range out of bounds in " + fieldName + ": " + part + " (valid: " + min + "-" + max + ")");
				}
			} else {
				int value = Integer.parseInt(part);
				if (value < min || value > max) {
					throw new IllegalArgumentException(
							"Value out of bounds in " + fieldName + ": " + part + " (valid: " + min + "-" + max + ")");
				}
			}
		}
	}

}
