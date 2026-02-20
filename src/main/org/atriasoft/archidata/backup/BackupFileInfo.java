package org.atriasoft.archidata.backup;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Metadata for a backup file parsed from its filename.
 */
record BackupFileInfo(
		Path path,
		String sequence,
		LocalDate date,
		boolean partial)
		implements Comparable<BackupFileInfo> {

	@Override
	public int compareTo(final BackupFileInfo other) {
		final int dateCompare = this.date.compareTo(other.date);
		if (dateCompare != 0) {
			return dateCompare;
		}
		return this.sequence.compareTo(other.sequence);
	}
}
