package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a scheduled task with a cron expression.
 */
record CronTask(
		String name,
		String cronExpression,
		Runnable action,
		boolean uniqueInQueue)
		implements Task {

	/**
	 * Check if the cron expression matches the given time.
	 * Supports: "*" , "number" , "*\/n".
	 * Format: minute hour day month dayOfWeek
	 */
	public boolean matches(final LocalDateTime time) {
		final String[] parts = this.cronExpression.split(" ");
		if (parts.length != 5) {
			throw new IllegalArgumentException("Cron expression must be 'minute hour day month dayOfWeek'");
		}

		return matchField(parts[0], time.getMinute()) //
				&& matchField(parts[1], time.getHour()) //
				&& matchField(parts[2], time.getDayOfMonth()) //
				&& matchField(parts[3], time.getMonthValue()) //
				&& matchField(parts[4], time.getDayOfWeek().getValue());
	}

	private boolean matchField(final String expr, final int value) {
		// Multiple values separated by commas
		final String[] parts = expr.split(",");
		for (final String part : parts) {
			if (part.equals("*")) {
				return true;
			}
			if (part.startsWith("*/")) {
				final int step = Integer.parseInt(part.substring(2));
				if (value % step == 0) {
					return true;
				}
			} else if (part.contains("-")) {
				final String[] range = part.split("-");
				final int start = Integer.parseInt(range[0]);
				final int end = Integer.parseInt(range[1]);
				if (value >= start && value <= end) {
					return true;
				}
			} else if (Integer.parseInt(part) == value) {
				return true;
			}
		}
		return false;
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