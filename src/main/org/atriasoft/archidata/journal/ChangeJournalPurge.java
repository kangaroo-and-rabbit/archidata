package org.atriasoft.archidata.journal;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

/**
 * Retention job of the change journal, run separately from the capture
 * ({@link ChangeJournalEngine}).
 *
 * <p>Two rules are combined, per source document — that is per
 * ({@code collectionName}, {@code sourceId}) pair:
 * <ul>
 *   <li>the {@code minVersions} most recent entries are <b>always</b> kept, whatever their age;</li>
 *   <li>beyond those, an entry is removed once it is older than {@code maxAge}.</li>
 * </ul>
 * So a document modified once two years ago keeps its history (nothing else describes it), while a
 * document modified every hour keeps its recent versions plus everything inside the age window.
 *
 * <p>Typical wiring on a {@link org.atriasoft.archidata.cron.CronScheduler}:
 * <pre>
 * final ChangeJournalPurge purge = new ChangeJournalPurge();
 * scheduler.addTask("change-journal-purge", "30 3 * * *", purge.asCronTask(5, Duration.ofDays(90)));
 * </pre>
 */
public class ChangeJournalPurge {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeJournalPurge.class);

	/** Number of entries fetched per round-trip when walking the journal. */
	private static final int CURSOR_BATCH_SIZE = 1000;
	/** Maximum number of identifiers removed by a single {@code deleteMany}. */
	private static final int DELETE_BATCH_SIZE = 1000;

	private final String journalCollectionName;
	private boolean indexesEnsured = false;

	/** Create a purge job working on the default journal collection ({@link ChangeJournalEntry#COLLECTION_NAME}). */
	public ChangeJournalPurge() {
		this(ChangeJournalEntry.COLLECTION_NAME);
	}

	/**
	 * Create a purge job working on a specific journal collection.
	 * @param journalCollectionName collection storing the journal entries
	 */
	public ChangeJournalPurge(final String journalCollectionName) {
		if (journalCollectionName == null || journalCollectionName.isEmpty()) {
			throw new IllegalArgumentException("journalCollectionName must not be empty");
		}
		this.journalCollectionName = journalCollectionName;
	}

	/**
	 * Returns the name of the purged journal collection.
	 * @return the journal collection name
	 */
	public String getJournalCollectionName() {
		return this.journalCollectionName;
	}

	/**
	 * Remove the expired journal entries, keeping the {@code minVersions} most recent ones of every
	 * source document.
	 * @param minVersions number of most recent entries always kept per source document
	 *        (use {@code 0} to only apply the age rule)
	 * @param maxAge maximum age of an entry beyond the kept versions
	 * @return the number of removed entries
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public long purge(final int minVersions, final Duration maxAge) throws IOException, DataAccessException {
		return purge(minVersions, maxAge, new Date());
	}

	/**
	 * Remove the expired journal entries relative to an explicit reference date (mainly useful for
	 * tests and for replaying a retention).
	 * @param minVersions number of most recent entries always kept per source document
	 * @param maxAge maximum age of an entry beyond the kept versions
	 * @param referenceDate date the ages are computed from
	 * @return the number of removed entries
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public long purge(final int minVersions, final Duration maxAge, final Date referenceDate)
			throws IOException, DataAccessException {
		return execute(minVersions, maxAge, referenceDate, false);
	}

	/**
	 * Count the entries that {@link #purge(int, Duration)} would remove, without removing anything.
	 * @param minVersions number of most recent entries always kept per source document
	 * @param maxAge maximum age of an entry beyond the kept versions
	 * @return the number of entries that would be removed
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public long purgeDryRun(final int minVersions, final Duration maxAge) throws IOException, DataAccessException {
		return purgeDryRun(minVersions, maxAge, new Date());
	}

	/**
	 * Count the entries that {@link #purge(int, Duration, Date)} would remove, without removing
	 * anything.
	 * @param minVersions number of most recent entries always kept per source document
	 * @param maxAge maximum age of an entry beyond the kept versions
	 * @param referenceDate date the ages are computed from
	 * @return the number of entries that would be removed
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public long purgeDryRun(final int minVersions, final Duration maxAge, final Date referenceDate)
			throws IOException, DataAccessException {
		return execute(minVersions, maxAge, referenceDate, true);
	}

	/**
	 * Wrap {@link #purge(int, Duration)} in a {@link Runnable} suitable for a
	 * {@link org.atriasoft.archidata.cron.CronScheduler}. Any failure is logged, never propagated.
	 * @param minVersions number of most recent entries always kept per source document
	 * @param maxAge maximum age of an entry beyond the kept versions
	 * @return the task to schedule
	 */
	public Runnable asCronTask(final int minVersions, final Duration maxAge) {
		checkParameters(minVersions, maxAge);
		return () -> {
			try {
				final long removed = purge(minVersions, maxAge);
				LOGGER.info("Journal purge: {} entry(ies) removed (keep {} version(s), max age {})", removed,
						minVersions, maxAge);
			} catch (final Exception ex) {
				LOGGER.error("Journal purge: run failed: {}", ex.getMessage(), ex);
			}
		};
	}

	private static void checkParameters(final int minVersions, final Duration maxAge) {
		if (minVersions < 0) {
			throw new IllegalArgumentException("minVersions must be >= 0, got: " + minVersions);
		}
		if (maxAge == null || maxAge.isNegative()) {
			throw new IllegalArgumentException("maxAge must be a non-negative duration");
		}
	}

	/**
	 * Walk the whole journal ordered by source document then by decreasing record date, and remove
	 * the entries ranked beyond {@code minVersions} that are older than the cut-off.
	 * <p>
	 * The walk is a streamed cursor with a projection covered by the
	 * ({@code collectionName}, {@code sourceId}, {@code recordedAt}, {@code _id}) index: the memory
	 * usage stays bounded by the delete batch whatever the journal size is.
	 */
	private long execute(final int minVersions, final Duration maxAge, final Date referenceDate, final boolean dryRun)
			throws IOException, DataAccessException {
		checkParameters(minVersions, maxAge);
		Objects.requireNonNull(referenceDate, "referenceDate must not be null");
		final Date cutoff = new Date(referenceDate.getTime() - maxAge.toMillis());
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			ensureIndexes(db);
			final MongoCollection<Document> journal = db.getCollection(this.journalCollectionName);
			final List<ObjectId> toRemove = new ArrayList<>();
			long removed = 0L;
			String currentKey = null;
			int rank = 0;
			for (final Document doc : journal.find()//
					.projection(new Document("_id", 1).append("collectionName", 1).append("sourceId", 1)
							.append("recordedAt", 1))//
					.sort(new Document("collectionName", 1).append("sourceId", 1).append("recordedAt", -1).append("_id",
							-1))//
					.batchSize(CURSOR_BATCH_SIZE)) {
				// ' ' is not producible by a collection name nor by an identifier: no key collision.
				final String key = doc.getString("collectionName") + ' ' + doc.getString("sourceId");
				if (!key.equals(currentKey)) {
					currentKey = key;
					rank = 0;
				} else {
					rank++;
				}
				if (rank < minVersions) {
					continue;
				}
				final Date recordedAt = doc.getDate("recordedAt");
				// An undated entry cannot be aged: keep it rather than removing it blindly.
				if (recordedAt == null || !recordedAt.before(cutoff)) {
					continue;
				}
				if (dryRun) {
					removed++;
					continue;
				}
				toRemove.add(doc.getObjectId("_id"));
				if (toRemove.size() >= DELETE_BATCH_SIZE) {
					removed += deleteBatch(journal, toRemove);
				}
			}
			if (!dryRun) {
				removed += deleteBatch(journal, toRemove);
			}
			LOGGER.info("Journal purge{}: {} entry(ies) older than {} beyond the {} kept version(s)",
					dryRun ? " (dry-run)" : "", removed, cutoff, minVersions);
			return removed;
		}
	}

	/** Remove the pending identifiers and clear the batch. Returns the number of removed entries. */
	private static long deleteBatch(final MongoCollection<Document> journal, final List<ObjectId> ids) {
		if (ids.isEmpty()) {
			return 0L;
		}
		final long count = journal.deleteMany(new Document("_id", new Document("$in", ids))).getDeletedCount();
		ids.clear();
		return count;
	}

	/**
	 * Create the index the purge walk relies on, once per instance. Identical to the one created by
	 * {@link ChangeJournalEngine}: creating it here keeps the purge usable on a journal filled by
	 * another process.
	 */
	private void ensureIndexes(final MongoDatabase db) {
		if (this.indexesEnsured) {
			return;
		}
		db.getCollection(this.journalCollectionName).createIndex(Indexes.compoundIndex(//
				Indexes.ascending("collectionName"), Indexes.ascending("sourceId"), //
				Indexes.descending("recordedAt"), Indexes.descending("_id")));
		this.indexesEnsured = true;
	}
}
