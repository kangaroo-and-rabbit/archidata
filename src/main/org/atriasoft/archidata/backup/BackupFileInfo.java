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
		LocalDate date,
		boolean partial,
		boolean delta)
		implements Comparable<BackupFileInfo> {

	@Override
	public int compareTo(final BackupFileInfo other) {
		final int dateCompare = this.date.compareTo(other.date);
		if (dateCompare != 0) {
			return dateCompare;
		}
		// Same day: the text alone would put 12-45-00 before 12:30:00, and a name without a time after both.
		final LocalDateTime time = BackupEngine.parseDateTimeFromSequence(this.sequence);
		final LocalDateTime otherTime = BackupEngine.parseDateTimeFromSequence(other.sequence);
		if (time != null && otherTime != null) {
			final int timeCompare = time.compareTo(otherTime);
			if (timeCompare != 0) {
				return timeCompare;
			}
		}
		return this.sequence.compareTo(other.sequence);
	}
}
