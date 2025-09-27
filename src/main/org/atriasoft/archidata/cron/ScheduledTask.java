package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;
import java.util.Objects;

public record ScheduledTask(
		String name,
		LocalDateTime executeAt,
		Runnable action)
		implements Task {

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

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}
}
