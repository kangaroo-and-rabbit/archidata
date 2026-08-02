package test.atriasoft.archidata.journal;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.atriasoft.archidata.journal.ChangeJournalPurge;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/**
 * Tests of the journal retention job: the N most recent versions of a document are always kept,
 * and beyond them the entries older than the maximum age are removed.
 */
public class TestChangeJournalPurge {

	private static final long DAY_MILLIS = 24L * 3600L * 1000L;

	private final Date now = new Date();

	@BeforeAll
	public static void configureWebServer() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@BeforeEach
	public void cleanJournal() {
		JournalTestTools.dropJournal();
	}

	/** Insert one entry for the given source document, aged of {@code ageDays} days. */
	private void insertAged(final String sourceId, final long ageDays) {
		JournalTestTools.insertJournalEntry("someCollection", sourceId,
				new Date(this.now.getTime() - ageDays * DAY_MILLIS));
	}

	private List<Document> remainingOf(final String sourceId) {
		return JournalTestTools.journalEntries().stream()//
				.filter(entry -> sourceId.equals(entry.getString("sourceId")))//
				.sorted((a, b) -> b.getDate("recordedAt").compareTo(a.getDate("recordedAt")))//
				.toList();
	}

	@Test
	public void testKeepTheMinimumVersionsWhateverTheirAge() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		// Five versions, all far older than the maximum age.
		insertAged(sourceId, 500);
		insertAged(sourceId, 400);
		insertAged(sourceId, 300);
		insertAged(sourceId, 200);
		insertAged(sourceId, 100);

		final long removed = new ChangeJournalPurge().purge(3, Duration.ofDays(30), this.now);

		Assertions.assertEquals(2, removed);
		final List<Document> remaining = remainingOf(sourceId);
		Assertions.assertEquals(3, remaining.size());
		// The kept ones are the most recent.
		Assertions.assertEquals(new Date(this.now.getTime() - 100 * DAY_MILLIS),
				remaining.get(0).getDate("recordedAt"));
		Assertions.assertEquals(new Date(this.now.getTime() - 300 * DAY_MILLIS),
				remaining.get(2).getDate("recordedAt"));
	}

	@Test
	public void testKeepEverythingYoungerThanTheMaximumAge() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		// Ten recent versions: beyond the two kept ones, none is old enough to be removed.
		for (int iii = 0; iii < 10; iii++) {
			insertAged(sourceId, iii);
		}

		final long removed = new ChangeJournalPurge().purge(2, Duration.ofDays(30), this.now);

		Assertions.assertEquals(0, removed);
		Assertions.assertEquals(10, remainingOf(sourceId).size());
	}

	@Test
	public void testRemoveTheOldEntriesBeyondTheKeptVersions() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		insertAged(sourceId, 1);
		insertAged(sourceId, 2);
		insertAged(sourceId, 50);
		insertAged(sourceId, 60);
		insertAged(sourceId, 70);

		final long removed = new ChangeJournalPurge().purge(2, Duration.ofDays(30), this.now);

		// The 2 most recent are kept by the version rule, the 3 old ones fall beyond the age rule.
		Assertions.assertEquals(3, removed);
		final List<Document> remaining = remainingOf(sourceId);
		Assertions.assertEquals(2, remaining.size());
		Assertions.assertEquals(new Date(this.now.getTime() - DAY_MILLIS), remaining.get(0).getDate("recordedAt"));
	}

	@Test
	public void testRetentionIsPerSourceDocument() throws Exception {
		final String kept = new ObjectId().toHexString();
		final String trimmed = new ObjectId().toHexString();
		// One document modified once, long ago: its only version must survive.
		insertAged(kept, 900);
		// One document with a long history: only the 2 most recent versions survive.
		insertAged(trimmed, 900);
		insertAged(trimmed, 800);
		insertAged(trimmed, 700);
		insertAged(trimmed, 600);

		final long removed = new ChangeJournalPurge().purge(2, Duration.ofDays(30), this.now);

		Assertions.assertEquals(2, removed);
		Assertions.assertEquals(1, remainingOf(kept).size());
		Assertions.assertEquals(2, remainingOf(trimmed).size());
	}

	@Test
	public void testSameSourceIdInTwoCollectionsAreIndependent() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		JournalTestTools.insertJournalEntry("collectionA", sourceId, new Date(this.now.getTime() - 900 * DAY_MILLIS));
		JournalTestTools.insertJournalEntry("collectionB", sourceId, new Date(this.now.getTime() - 900 * DAY_MILLIS));

		final long removed = new ChangeJournalPurge().purge(1, Duration.ofDays(30), this.now);

		// One version kept per (collection, source id) pair, not one per source id.
		Assertions.assertEquals(0, removed);
		Assertions.assertEquals(2, JournalTestTools.journalEntries().size());
	}

	@Test
	public void testZeroMinimumVersionsAppliesOnlyTheAgeRule() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		insertAged(sourceId, 900);
		insertAged(sourceId, 1);

		final long removed = new ChangeJournalPurge().purge(0, Duration.ofDays(30), this.now);

		Assertions.assertEquals(1, removed);
		Assertions.assertEquals(1, remainingOf(sourceId).size());
	}

	@Test
	public void testDryRunRemovesNothing() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		insertAged(sourceId, 900);
		insertAged(sourceId, 800);
		insertAged(sourceId, 1);

		final ChangeJournalPurge purge = new ChangeJournalPurge();
		Assertions.assertEquals(2, purge.purgeDryRun(1, Duration.ofDays(30), this.now));
		Assertions.assertEquals(3, remainingOf(sourceId).size());
		// The real run removes exactly what the dry run announced.
		Assertions.assertEquals(2, purge.purge(1, Duration.ofDays(30), this.now));
		Assertions.assertEquals(1, remainingOf(sourceId).size());
	}

	@Test
	public void testPurgeAcrossSeveralDeleteBatches() throws Exception {
		// 2500 old versions of the same document: more than one delete batch (1000).
		final String sourceId = new ObjectId().toHexString();
		for (int iii = 0; iii < 2500; iii++) {
			insertAged(sourceId, 100 + iii);
		}

		final long removed = new ChangeJournalPurge().purge(5, Duration.ofDays(30), this.now);

		Assertions.assertEquals(2495, removed);
		Assertions.assertEquals(5, remainingOf(sourceId).size());
	}

	@Test
	public void testEmptyJournalIsANoOp() throws Exception {
		Assertions.assertEquals(0, new ChangeJournalPurge().purge(5, Duration.ofDays(30), this.now));
	}

	@Test
	public void testInvalidParameters() {
		final ChangeJournalPurge purge = new ChangeJournalPurge();
		Assertions.assertThrows(IllegalArgumentException.class, () -> purge.purge(-1, Duration.ofDays(30)));
		Assertions.assertThrows(IllegalArgumentException.class, () -> purge.purge(1, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> purge.purge(1, Duration.ofDays(-1)));
		Assertions.assertThrows(IllegalArgumentException.class, () -> purge.asCronTask(-1, Duration.ofDays(30)));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ChangeJournalPurge(""));
	}

	@Test
	public void testCronTaskNeverThrows() throws Exception {
		final String sourceId = new ObjectId().toHexString();
		insertAged(sourceId, 900);
		insertAged(sourceId, 1);

		new ChangeJournalPurge().asCronTask(1, Duration.ofDays(30)).run();

		Assertions.assertEquals(1, remainingOf(sourceId).size());
	}
}
