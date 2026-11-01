package org.atriasoft.archidata.backup;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory snapshot of MongoDB collections.
 * <p>
 * Each entry maps a collection name to its serialized content (newline-delimited JSON, UTF-8 encoded).
 * The format matches what BackupEngine writes into tar archive entries.
 * <p>
 * The map is read-only. The byte arrays are not copied, a snapshot can hold a whole database: never write to them.
 * <p>
 * Useful for capturing and restoring database state in tests or for temporary rollback without file I/O.
 *
 * @param collections map of collection name to serialized document bytes
 */
public record BackupSnapshot(
		Map<String, byte[]> collections) {

	/**
	 * Compact constructor copying the map into an unmodifiable one, in the same order.
	 */
	public BackupSnapshot {
		collections = Collections.unmodifiableMap(new LinkedHashMap<>(collections));
	}

	/**
	 * Return the number of collections in this snapshot.
	 * @return the number of collections held by this snapshot
	 */
	public int size() {
		return this.collections.size();
	}

	/**
	 * Return {@code true} if this snapshot contains no collections.
	 * @return {@code true} when this snapshot holds no collection
	 */
	public boolean isEmpty() {
		return this.collections.isEmpty();
	}
}
