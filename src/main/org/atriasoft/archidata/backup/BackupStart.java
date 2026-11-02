package org.atriasoft.archidata.backup;

/** Answer of {@link BackupService#startAsync()} and {@link BackupService#startDeltaAsync()}. */
public enum BackupStart {
	/** The backup is running in its own thread, or a full will once the delta in progress is written. */
	STARTED,
	/** Nothing was started: a backup is already running. */
	ALREADY_RUNNING,
	/** Nothing was started: the volume of the folder has too little free space, see {@link BackupService#SPACE_MARGIN_BYTES}. */
	INSUFFICIENT_SPACE,
}
