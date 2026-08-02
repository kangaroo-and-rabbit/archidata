package test.atriasoft.archidata.journal;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.journal.ChangeJournalEngine;
import org.atriasoft.archidata.journal.ChangeJournalEngine.InitialCapture;
import org.atriasoft.archidata.journal.ChangeJournalEntry;
import org.atriasoft.archidata.journal.ChangeJournalMarker;
import org.atriasoft.archidata.journal.ChangeJournalReport;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.journal.model.JournalDataOther;
import test.atriasoft.archidata.journal.model.JournalDataWithUpdate;
import test.atriasoft.archidata.journal.model.JournalDataWithoutUpdate;

/**
 * Tests of the incremental change journal: the modified documents of every registered collection
 * are appended to a single journal collection, and a per-collection marker drives what the next
 * run selects.
 */
public class TestChangeJournal {

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
	public void cleanCollections() throws Exception {
		DataAccess.drop(JournalDataWithUpdate.class);
		DataAccess.drop(JournalDataOther.class);
		DataAccess.drop(JournalDataWithoutUpdate.class);
		JournalTestTools.dropJournal();
	}

	private JournalDataWithUpdate insert(final long value) throws Exception {
		final JournalDataWithUpdate data = new JournalDataWithUpdate();
		data.dataLong = value;
		data.dataString = "value-" + value;
		return DataAccess.insert(data);
	}

	private ChangeJournalEngine createEngine() throws Exception {
		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(JournalDataWithUpdate.class, JournalDataOther.class);
		return engine;
	}

	@Test
	public void testFirstRunCapturesEverything() throws Exception {
		insert(1);
		insert(2);
		insert(3);
		Thread.sleep(20);

		final ChangeJournalReport report = createEngine().run();

		Assertions.assertTrue(report.isSuccess());
		Assertions.assertEquals(3, report.totalCaptured());
		Assertions.assertEquals(3L, report.capturedByCollection().get("JournalDataWithUpdate"));
		Assertions.assertEquals(0L, report.capturedByCollection().get("JournalDataOther"));
		Assertions.assertEquals(3, JournalTestTools.journalEntries().size());
	}

	@Test
	public void testFirstRunSkippedWhenInitialCaptureSkip() throws Exception {
		insert(1);
		insert(2);
		Thread.sleep(20);

		final ChangeJournalEngine engine = createEngine();
		engine.setInitialCapture(InitialCapture.SKIP);
		final ChangeJournalReport report = engine.run();

		Assertions.assertEquals(0, report.totalCaptured());
		Assertions.assertEquals(0, JournalTestTools.journalEntries().size());
		// The marker is still set: only the modifications made after this run are journalized.
		final List<ChangeJournalMarker> markers = engine.getMarkers();
		Assertions.assertEquals(2, markers.size());
		Thread.sleep(20);
		insert(3);
		Thread.sleep(20);

		Assertions.assertEquals(1, engine.run().totalCaptured());
		final List<Document> entries = JournalTestTools.journalEntries();
		Assertions.assertEquals(1, entries.size());
		Assertions.assertTrue(entries.get(0).getString("data").contains("value-3"));
	}

	@Test
	public void testSecondRunOnlyCapturesTheModifiedDocuments() throws Exception {
		insert(1);
		insert(2);
		final JournalDataWithUpdate toModify = insert(3);
		Thread.sleep(20);

		final ChangeJournalEngine engine = createEngine();
		Assertions.assertEquals(3, engine.run().totalCaptured());
		Thread.sleep(20);

		// Nothing changed: the run captures nothing.
		Assertions.assertEquals(0, engine.run().totalCaptured());
		Assertions.assertEquals(3, JournalTestTools.journalEntries().size());
		Thread.sleep(20);

		// One update and one creation.
		toModify.dataLong = 333L;
		DataAccess.updateById(toModify, toModify.getOid());
		insert(4);
		Thread.sleep(20);

		Assertions.assertEquals(2, engine.run().totalCaptured());
		Assertions.assertEquals(5, JournalTestTools.journalEntries().size());
	}

	@Test
	public void testJournalIsAppendOnly() throws Exception {
		final JournalDataWithUpdate data = insert(1);
		Thread.sleep(20);
		final ChangeJournalEngine engine = createEngine();
		engine.run();
		Thread.sleep(20);

		data.dataLong = 2L;
		DataAccess.updateById(data, data.getOid());
		Thread.sleep(20);
		engine.run();
		Thread.sleep(20);

		data.dataLong = 3L;
		DataAccess.updateById(data, data.getOid());
		Thread.sleep(20);
		engine.run();

		// The three successive versions of the same source document are kept.
		final List<Document> entries = JournalTestTools.journalEntries().stream()//
				.filter(entry -> data.getOid().toHexString().equals(entry.getString("sourceId")))//
				.sorted((a, b) -> a.getDate("recordedAt").compareTo(b.getDate("recordedAt")))//
				.toList();
		Assertions.assertEquals(3, entries.size());
		Assertions.assertTrue(entries.get(0).getString("data").contains("\"dataLong\": {\"$numberLong\": \"1\"}"));
		Assertions.assertTrue(entries.get(1).getString("data").contains("\"dataLong\": {\"$numberLong\": \"2\"}"));
		Assertions.assertTrue(entries.get(2).getString("data").contains("\"dataLong\": {\"$numberLong\": \"3\"}"));
	}

	@Test
	public void testEveryCollectionSharesTheSameJournal() throws Exception {
		insert(1);
		final JournalDataOther other = new JournalDataOther();
		other.label = "hello";
		DataAccess.insert(other);
		Thread.sleep(20);

		final ChangeJournalReport report = createEngine().run();

		Assertions.assertEquals(2, report.totalCaptured());
		final Set<String> collectionNames = JournalTestTools.journalEntries().stream()//
				.map(entry -> entry.getString("collectionName"))//
				.collect(Collectors.toSet());
		Assertions.assertEquals(Set.of("JournalDataWithUpdate", "JournalDataOther"), collectionNames);
	}

	@Test
	public void testEntryContentAndRecordDate() throws Exception {
		final JournalDataWithUpdate data = insert(42);
		Thread.sleep(20);

		final ChangeJournalReport report = createEngine().run();

		final List<ChangeJournalEntry> entries = DataAccess.getAll(ChangeJournalEntry.class);
		Assertions.assertEquals(1, entries.size());
		final ChangeJournalEntry entry = entries.get(0);
		Assertions.assertEquals("JournalDataWithUpdate", entry.getCollectionName());
		Assertions.assertEquals(data.getOid().toHexString(), entry.getSourceId());
		// The record date is the date of the run, shared by every entry it wrote.
		Assertions.assertEquals(report.runDate(), entry.getRecordedAt());
		// The serialized data round-trips back to the source document (extended JSON is lossless).
		final Document restored = Document.parse(entry.getData());
		Assertions.assertEquals(data.getOid(), restored.getObjectId("_id"));
		Assertions.assertEquals(42L, restored.getLong("dataLong"));
		Assertions.assertEquals("value-42", restored.getString("dataString"));
		// The captured document date is copied out of the source document.
		Assertions.assertNotNull(entry.getSourceUpdatedAt());
		Assertions.assertEquals(restored.getDate("updatedAt"), entry.getSourceUpdatedAt());
	}

	@Test
	public void testReadTheHistoryOfOneDocumentWithATypedQuery() throws Exception {
		final JournalDataWithUpdate data = insert(1);
		Thread.sleep(20);
		final ChangeJournalEngine engine = createEngine();
		engine.run();
		Thread.sleep(20);
		data.dataLong = 2L;
		DataAccess.updateById(data, data.getOid());
		Thread.sleep(20);
		engine.run();

		final List<ChangeJournalEntry> entries = DataAccess.gets(ChangeJournalEntry.class, new Condition(Filters.and(//
				Filters.eq(ChangeJournalEntry::getCollectionName, "JournalDataWithUpdate"),
				Filters.eq(ChangeJournalEntry::getSourceId, data.getOid().toHexString()))),
				OrderBy.desc(ChangeJournalEntry::getRecordedAt));

		Assertions.assertEquals(2, entries.size());
		// Most recent first: the last captured version.
		Assertions.assertEquals(2L, Document.parse(entries.get(0).getData()).getLong("dataLong"));
		Assertions.assertEquals(1L, Document.parse(entries.get(1).getData()).getLong("dataLong"));
	}

	@Test
	public void testMarkerIsPerCollection() throws Exception {
		insert(1);
		Thread.sleep(20);

		final ChangeJournalEngine engine = createEngine();
		final ChangeJournalReport report = engine.run();

		final List<ChangeJournalMarker> markers = engine.getMarkers();
		Assertions.assertEquals(2, markers.size());
		final ChangeJournalMarker withUpdate = markers.stream()//
				.filter(marker -> "JournalDataWithUpdate".equals(marker.getCollectionName()))//
				.findFirst().orElseThrow();
		final ChangeJournalMarker other = markers.stream()//
				.filter(marker -> "JournalDataOther".equals(marker.getCollectionName()))//
				.findFirst().orElseThrow();
		Assertions.assertEquals(report.runDate(), withUpdate.getLastSavedAt());
		Assertions.assertEquals(1L, withUpdate.getLastCount());
		Assertions.assertEquals(1L, withUpdate.getTotalCount());
		// A collection with nothing to capture still moves its marker forward.
		Assertions.assertEquals(report.runDate(), other.getLastSavedAt());
		Assertions.assertEquals(0L, other.getLastCount());

		Thread.sleep(20);
		insert(2);
		insert(3);
		Thread.sleep(20);
		engine.run();
		final ChangeJournalMarker updated = engine.getMarkers().stream()//
				.filter(marker -> "JournalDataWithUpdate".equals(marker.getCollectionName()))//
				.findFirst().orElseThrow();
		Assertions.assertEquals(2L, updated.getLastCount());
		Assertions.assertEquals(3L, updated.getTotalCount());
	}

	@Test
	public void testResetMarkerRestartsAFullCapture() throws Exception {
		insert(1);
		insert(2);
		Thread.sleep(20);
		final ChangeJournalEngine engine = createEngine();
		Assertions.assertEquals(2, engine.run().totalCaptured());
		Thread.sleep(20);

		Assertions.assertTrue(engine.resetMarker("JournalDataWithUpdate"));
		Assertions.assertEquals(1, engine.getMarkers().size());
		// Without a marker, the collection is fully captured again.
		Assertions.assertEquals(2, engine.run().totalCaptured());
		Assertions.assertEquals(4, JournalTestTools.journalEntries().size());

		Assertions.assertEquals(2L, engine.resetAllMarkers());
		Assertions.assertEquals(0, engine.getMarkers().size());
	}

	@Test
	public void testDocumentsWithoutTimestampAreOnlyCapturedOnce() throws Exception {
		final JournalDataWithoutUpdate data = new JournalDataWithoutUpdate();
		data.dataString = "no timestamp";
		DataAccess.insert(data);
		Thread.sleep(20);

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(JournalDataWithoutUpdate.class);
		// First capture: the undated document is part of the initial snapshot.
		Assertions.assertEquals(1, engine.run().totalCaptured());
		Thread.sleep(20);
		// Incremental runs cannot date it: capturing it again would duplicate it forever.
		Assertions.assertEquals(0, engine.run().totalCaptured());
		Assertions.assertEquals(1, JournalTestTools.journalEntries().size());
	}

	@Test
	public void testRunAllDiscoversCollectionsAndSkipsTheJournalItself() throws Exception {
		insert(1);
		final JournalDataOther other = new JournalDataOther();
		other.label = "hello";
		DataAccess.insert(other);
		Thread.sleep(20);

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		final ChangeJournalReport report = engine.runAll();

		Assertions.assertTrue(report.capturedByCollection().containsKey("JournalDataWithUpdate"));
		Assertions.assertTrue(report.capturedByCollection().containsKey("JournalDataOther"));
		Assertions.assertFalse(report.capturedByCollection().containsKey(ChangeJournalEntry.COLLECTION_NAME));
		Assertions.assertFalse(report.capturedByCollection().containsKey(ChangeJournalMarker.COLLECTION_NAME));
		Thread.sleep(20);

		// A second discovery run now sees the journal collections: they must still be excluded.
		final ChangeJournalReport second = engine.runAll();
		Assertions.assertEquals(0, second.totalCaptured());
		Assertions.assertFalse(second.capturedByCollection().containsKey(ChangeJournalEntry.COLLECTION_NAME));
	}

	@Test
	public void testDocumentsModifiedDuringTheRunAreCapturedByTheNextOne() throws Exception {
		final ChangeJournalEngine engine = createEngine();
		// First run on empty collections: only sets the markers.
		Assertions.assertEquals(0, engine.run().totalCaptured());
		Thread.sleep(20);

		// Simulate a document written "during" a run: dated after the run upper bound.
		JournalTestTools.insertRawDated("JournalDataWithUpdate", new Date(System.currentTimeMillis() + 300));
		Assertions.assertEquals(0, engine.run().totalCaptured());
		Thread.sleep(350);

		// It is not lost: the next run, whose upper bound has passed it, captures it.
		Assertions.assertEquals(1, engine.run().totalCaptured());
	}

	@Test
	public void testSafetyOverlapReCapturesTheBorderDocuments() throws Exception {
		final ChangeJournalEngine engine = createEngine();
		insert(1);
		Thread.sleep(20);
		Assertions.assertEquals(1, engine.run().totalCaptured());
		Thread.sleep(20);

		// Without overlap nothing is captured again...
		Assertions.assertEquals(0, engine.run().totalCaptured());
		// ... with an overlap wide enough, the border documents are captured a second time.
		engine.setSafetyOverlapMillis(60_000);
		Assertions.assertEquals(1, engine.run().totalCaptured());
	}

	@Test
	public void testRejectTheJournalCollectionsAsSource() {
		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addCollection(ChangeJournalEntry.COLLECTION_NAME, ChangeJournalMarker.COLLECTION_NAME, "user");
		Assertions.assertEquals(List.of("user"), engine.getCollections());
		// Registering twice the same collection does not duplicate the capture.
		engine.addCollection("user");
		Assertions.assertEquals(List.of("user"), engine.getCollections());
	}

	@Test
	public void testInvalidConfiguration() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ChangeJournalEngine("same", "same"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new ChangeJournalEngine("", "marker"));
		final ChangeJournalEngine engine = new ChangeJournalEngine();
		Assertions.assertThrows(IllegalArgumentException.class, () -> engine.setBatchMaxDocuments(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> engine.setBatchMaxBytes(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> engine.setSafetyOverlapMillis(-1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> engine.setInitialCapture(null));
	}

	@Test
	public void testBatchingDoesNotChangeTheResult() throws Exception {
		for (int iii = 0; iii < 25; iii++) {
			insert(iii);
		}
		Thread.sleep(20);

		final ChangeJournalEngine engine = createEngine();
		// Force several insertMany round-trips.
		engine.setBatchMaxDocuments(4);
		Assertions.assertEquals(25, engine.run().totalCaptured());
		Assertions.assertEquals(25, JournalTestTools.journalEntries().size());
	}

	@Test
	public void testCronTaskNeverThrows() throws Exception {
		insert(1);
		Thread.sleep(20);
		final ChangeJournalEngine engine = createEngine();
		engine.asCronTask().run();
		Assertions.assertEquals(1, JournalTestTools.journalEntries().size());

		// An unknown collection is simply empty for MongoDB: the task must stay silent.
		final ChangeJournalEngine unknown = new ChangeJournalEngine();
		unknown.addCollection("thisCollectionDoesNotExist");
		Assertions.assertDoesNotThrow(() -> unknown.asCronTask().run());
	}
}
