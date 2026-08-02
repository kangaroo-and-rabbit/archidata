package org.atriasoft.archidata.index;

import java.util.List;
import java.util.Map;

/**
 * Outcome of an index synchronization.
 *
 * @param created names of the created indexes, per collection
 * @param dropped names of the removed indexes, per collection
 * @param failures error message per index that could not be applied — only populated when the
 *        engine does not fail fast
 * @param skippedCollections collections skipped because the cache proved they were already in sync
 */
public record IndexReport(
		Map<String, List<String>> created,
		Map<String, List<String>> dropped,
		Map<String, String> failures,
		List<String> skippedCollections) {

	/** Compact constructor making the collections unmodifiable. */
	public IndexReport {
		created = Map.copyOf(created);
		dropped = Map.copyOf(dropped);
		failures = Map.copyOf(failures);
		skippedCollections = List.copyOf(skippedCollections);
	}

	/**
	 * Total number of created indexes.
	 *
	 * @return the number of creations
	 */
	public int createdCount() {
		return this.created.values().stream().mapToInt(List::size).sum();
	}

	/**
	 * Total number of removed indexes.
	 *
	 * @return the number of drops
	 */
	public int droppedCount() {
		return this.dropped.values().stream().mapToInt(List::size).sum();
	}

	/**
	 * Tells whether everything was applied.
	 *
	 * @return {@code true} when no index failed
	 */
	public boolean isSuccess() {
		return this.failures.isEmpty();
	}

	@Override
	public String toString() {
		return createdCount() + " created, " + droppedCount() + " dropped, " + this.failures.size() + " failed, "
				+ this.skippedCollections.size() + " collection(s) already in sync";
	}
}
