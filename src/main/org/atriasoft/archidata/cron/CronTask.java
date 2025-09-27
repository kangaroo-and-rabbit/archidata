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
		boolean uniqueInQueue) {

	/**
	 * Check if the cron expression matches the given time.
	 * Supports: "*" , "number" , "*\/n".
	 * Format: minute hour day month dayOfWeek
	 */
	public boolean matches(LocalDateTime time) {
		String[] parts = cronExpression.split(" ");
		if (parts.length != 5) {
			throw new IllegalArgumentException("Cron expression must be 'minute hour day month dayOfWeek'");
		}

		return matchField(parts[0], time.getMinute()) //
				&& matchField(parts[1], time.getHour()) //
				&& matchField(parts[2], time.getDayOfMonth()) //
				&& matchField(parts[3], time.getMonthValue()) //
				&& matchField(parts[4], time.getDayOfWeek().getValue());
	}

	private boolean matchField(String expr, int value) {
		// Multiple values separated by commas
		String[] parts = expr.split(",");
		for (String part : parts) {
			if (part.equals("*")) {
				return true;
			}
			if (part.startsWith("*/")) {
				int step = Integer.parseInt(part.substring(2));
				if (value % step == 0) {
					return true;
				}
			} else if (part.contains("-")) {
				String[] range = part.split("-");
				int start = Integer.parseInt(range[0]);
				int end = Integer.parseInt(range[1]);
				if (value >= start && value <= end) {
					return true;
				}
			} else {
				if (Integer.parseInt(part) == value) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CronTask other)) {
			return false;
		}
		return Objects.equals(name, other.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}