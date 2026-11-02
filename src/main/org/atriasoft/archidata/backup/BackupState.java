package org.atriasoft.archidata.backup;

import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * State of the backups of an application.
 *
 * @param running {@code true} while a backup is being written
 * @param lastAttempt last finished attempt since the server started, {@code null} when there is none yet; a delta that
 *            works does not replace a full that failed
 * @param archives the archives, most recent first
 */
public record BackupState(
		boolean running,
		@Nullable BackupAttempt lastAttempt,
		@NotNull List<@NotNull BackupArchive> archives) {

	/**
	 * Compact constructor copying the list, so that the state can not change once built.
	 *
	 * @param running {@code true} while a backup is being written
	 * @param lastAttempt last finished attempt since the server started, {@code null} when there is none yet
	 * @param archives the archives, most recent first
	 */
	public BackupState {
		archives = List.copyOf(archives);
	}
}
