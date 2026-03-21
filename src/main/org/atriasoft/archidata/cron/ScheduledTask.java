package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A one-time scheduled task that executes at a specific date and time.
 *
 * <p>Equality is based solely on the task {@link #name}, allowing the scheduler
 * to prevent duplicate entries in the queue.
 *
 * @param name the unique name identifying this task
 * @param executeAt the date and time at which the task should execute
 * @param action the action to run when the task fires
 */
public record ScheduledTask(
		String name,
		LocalDateTime executeAt,
		Runnable action)
		implements Task {

	/**
	 * Checks equality based on the task name only.
	 *
	 * @param o the object to compare with
	 * @return {@code true} if the other object is a {@code ScheduledTask} with the same name
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof final ScheduledTask other)) {
			return false;
		}
		return Objects.equals(this.name, other.name);
	}

	/**
	 * Returns a hash code based on the task name.
	 *
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}
}
