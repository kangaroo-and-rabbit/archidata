package org.atriasoft.archidata.backup;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Metadata for a backup file parsed from its filename.
 * <p>
 * {@code delta} marks an incremental backup (sequence ending with {@code _delta}): it only
 * contains the documents modified since the full backup it is based on, and is superseded by
 * any newer full backup. {@code base} is the date of this full when the name holds it, as
 * {@link BackupEngine#deltaSequence} writes it, {@code null} otherwise.
 */
record BackupFileInfo(
		Path path,
		String sequence,
		LocalDateTime dateTime,
		boolean partial,
		boolean delta,
		String base)
		implements Comparable<BackupFileInfo> {

	/** @return the day of the archive, as written in its name */
	LocalDate date() {
		return this.dateTime.toLocalDate();
	}

	/**
	 * Tell whether this archive is a delta a restore can apply on a full: later than it, and starting from it when its
	 * name says where it starts from. A delta of a former version does not say: its date is all there is.
	 * @param full the full backup
	 * @return {@code true} when it can be applied
	 */
	boolean isDeltaOf(final BackupFileInfo full) {
		return this.delta && compareTo(full) > 0
				&& (this.base == null || this.base.equals(BackupEngine.baseStamp(full.dateTime)));
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
