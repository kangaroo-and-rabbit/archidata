package org.atriasoft.archidata.backup;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Metadata for a backup file parsed from its filename.
 * <p>
 * {@code delta} marks an incremental backup (sequence ending with {@code _delta}): it only
 * contains the documents modified since the full backup it is based on, and is superseded by
 * any newer full backup.
 */
record BackupFileInfo(
		Path path,
		String sequence,
		LocalDateTime dateTime,
		boolean partial,
		boolean delta)
		implements Comparable<BackupFileInfo> {

	/** @return the day of the archive, as written in its name */
	LocalDate date() {
		return this.dateTime.toLocalDate();
	}

	/** Compare the times, not the texts: the text puts 12-45-00 before 12:30:00, and a name without a time after both. */
	@Override
	public int compareTo(final BackupFileInfo other) {
		final int timeCompare = this.dateTime.compareTo(other.dateTime);
		if (timeCompare != 0) {
			return timeCompare;
		}
		return this.sequence.compareTo(other.sequence);
	}
}
