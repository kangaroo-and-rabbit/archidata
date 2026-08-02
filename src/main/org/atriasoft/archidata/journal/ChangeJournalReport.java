package org.atriasoft.archidata.journal;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Outcome of one {@link ChangeJournalEngine} capture run.
 *
 * @param runDate date at which the run started; also the upper bound of the captured documents
 *        and the {@code recordedAt} of every entry written by this run
 * @param capturedByCollection number of entries written, per source collection (collections that
 *        had nothing to capture are present with a count of {@code 0})
 * @param failedCollections names of the collections whose capture failed; their marker was left
 *        untouched, so the next run captures them again
 */
public record ChangeJournalReport(
		Date runDate,
		Map<String, Long> capturedByCollection,
		List<String> failedCollections) {

	/**
	 * Compact constructor making the collections unmodifiable.
	 */
	public ChangeJournalReport {
		capturedByCollection = Collections.unmodifiableMap(capturedByCollection);
		failedCollections = List.copyOf(failedCollections);
	}

	/**
	 * Total number of journal entries written by this run, across every collection.
	 *
	 * @return the number of written entries
	 */
	public long totalCaptured() {
		return this.capturedByCollection.values().stream().mapToLong(Long::longValue).sum();
	}

	/**
	 * Tells whether every collection of this run has been captured successfully.
	 *
	 * @return {@code true} if no collection failed
	 */
	public boolean isSuccess() {
		return this.failedCollections.isEmpty();
	}
}
