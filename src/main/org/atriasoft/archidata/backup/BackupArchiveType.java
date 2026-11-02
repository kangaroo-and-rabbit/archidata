package org.atriasoft.archidata.backup;

/** Kind of a backup archive, read from the suffix of its sequence. */
public enum BackupArchiveType {
	/** Every registered collection. */
	FULL,
	/** Only what changed since the full backup it is based on. */
	DELTA,
	/** A subset of the collections: never the base of a restore. */
	PARTIAL,
}
