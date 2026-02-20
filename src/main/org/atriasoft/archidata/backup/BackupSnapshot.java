package org.atriasoft.archidata.backup;

import java.util.Map;

/**
 * Immutable in-memory snapshot of MongoDB collections.
 * <p>
 * Each entry maps a collection name to its serialized content (newline-delimited JSON, UTF-8 encoded).
 * The format matches what BackupEngine writes into tar archive entries.
 * <p>
 * Useful for capturing and restoring database state in tests or for temporary rollback without file I/O.
 *
 * @param collections map of collection name to serialized document bytes
 */
public record BackupSnapshot(
		Map<String, byte[]> collections) {

	/** Return the number of collections in this snapshot. */
	public int size() {
		return this.collections.size();
	}

	/** Return {@code true} if this snapshot contains no collections. */
	public boolean isEmpty() {
		return this.collections.isEmpty();
	}
}
