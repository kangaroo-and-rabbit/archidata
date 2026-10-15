package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a scheduled task with a cron expression, parsed once at creation (see {@link CronExpression}
 * for the syntax).
 */
record CronTask(
		String name,
		CronExpression schedule,
		Runnable action,
		boolean uniqueInQueue)
		implements Task {

	/**
	 * Creates a task and parses its cron expression.
	 *
	 * @param name the task name
	 * @param cronExpression the five-field expression
	 * @param action the action to run
	 * @param uniqueInQueue {@code true} to keep at most one pending instance in the queue
	 * @throws IllegalArgumentException if the expression is invalid
	 */
	CronTask(final String name, final String cronExpression, final Runnable action, final boolean uniqueInQueue) {
		this(name, CronExpression.parse(cronExpression), action, uniqueInQueue);
	}

	/**
	 * Check if the cron expression matches the given time (minute precision).
	 *
	 * @param time the time to test
	 * @return {@code true} if the task is due at this minute
	 */
	public boolean matches(final LocalDateTime time) {
		return this.schedule.matches(time);
	}

	/** @return the cron expression as written by the caller */
	public String cronExpression() {
		return this.schedule.expression();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final CronTask other)) {
			return false;
		}
		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}
}
