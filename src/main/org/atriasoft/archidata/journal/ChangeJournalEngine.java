package org.atriasoft.archidata.journal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.FieldPathCursor;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOptions;

/**
 * Periodic incremental journal of the modified documents of a MongoDB database.
 *
 * <p>At every run, and for each journalized collection, the engine:
 * <ol>
 *   <li>reads the per-collection marker ({@link ChangeJournalMarker}) holding the upper bound of
 *       the previous successful capture;</li>
 *   <li>selects the documents modified since that date ({@code updatedAt}, falling back to
 *       {@code createdAt} for documents without {@code updatedAt});</li>
 *   <li>appends one {@link ChangeJournalEntry} per selected document into the single journal
 *       collection — carrying the source collection name, the serialized document, and the record
 *       date of the run;</li>
 *   <li>moves the marker forward to the run date, but only once the collection is fully written.</li>
 * </ol>
 *
 * <p>The journal is append-only: each captured version adds an entry, the history of a document is
 * therefore the set of entries sharing the same ({@code collectionName}, {@code sourceId}).
 * Trimming that history is the job of {@link ChangeJournalPurge}, run separately.
 *
 * <p>Typical wiring on a {@link org.atriasoft.archidata.cron.CronScheduler}:
 * <pre>
 * final ChangeJournalEngine journal = new ChangeJournalEngine();
 * journal.addClass(User.class, Media.class);
 * scheduler.addTask("change-journal", "*&#47;15 * * * *", journal.asCronTask());
 * </pre>
 *
 * <p><b>Limitations.</b> Detection relies on the document dates: a document modified in place
 * without its {@code updatedAt} being refreshed is not detected, and deletions are never detected
 * (nothing is written when a document disappears). Documents carrying neither {@code createdAt} nor
 * {@code updatedAt} can only be captured by the initial full capture (see
 * {@link #setInitialCapture(InitialCapture)}), never by the incremental runs — capturing them every
 * run would duplicate them forever.
 *
 * <p><b>Protected fields.</b> The capture copies the documents as they are stored. For a class
 * registered with {@link #addClass}, its {@code @DataNotRead} fields (secrets, password hashes, tokens)
 * are left out of the journal, also when they stand inside an embedded object, the elements of a list
 * or a set, or the values of a map; the system fields ({@code createdAt}, {@code updatedAt}, {@code deleted})
 * are kept. Its {@code @DataEncrypt} fields are copied in their stored form, that is encrypted. A
 * collection registered by name ({@link #addCollection}, {@link #runAll()}) has no model: declare its
 * sensitive fields with {@link #excludeFields(String, String...)}, and do not journalize token or session
 * collections at all.
 */
public class ChangeJournalEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChangeJournalEngine.class);

	/** Behaviour of the very first capture of a collection (no marker stored yet). */
	public static enum InitialCapture {
		/** Capture every document of the collection: the journal starts with a full snapshot. */
		FULL,
		/** Capture nothing: only set the marker, so the journal starts with the next modifications. */
		SKIP,
	}

	/** Number of documents fetched per round-trip when iterating a source collection cursor. */
	private static final int CURSOR_BATCH_SIZE = 1000;

	/** Default maximum number of journal entries written by a single {@code insertMany}. */
	public static final int DEFAULT_BATCH_MAX_DOCUMENTS = 1000;
	/** Default maximum accumulated serialized size (in bytes) of a single {@code insertMany}. */
	public static final int DEFAULT_BATCH_MAX_BYTES = 8 * 1024 * 1024;

	private final String journalCollectionName;
	private final String markerCollectionName;
	private final List<String> collectionNames = new ArrayList<>();
	/** Database field names left out of the capture, per source collection. */
	private final Map<String, Set<String>> excludedFields = new HashMap<>();
	/** Root of the model of every class registered for a source collection (several classes may share one). */
	private final Map<String, List<FieldPathCursor>> nestedProtection = new HashMap<>();
	private int batchMaxDocuments = DEFAULT_BATCH_MAX_DOCUMENTS;
	private int batchMaxBytes = DEFAULT_BATCH_MAX_BYTES;
	private long safetyOverlapMillis = 0L;
	private InitialCapture initialCapture = InitialCapture.FULL;
	private boolean indexesEnsured = false;

	/**
	 * Create a journal engine using the default collection names
	 * ({@link ChangeJournalEntry#COLLECTION_NAME} and {@link ChangeJournalMarker#COLLECTION_NAME}).
	 */
	public ChangeJournalEngine() {
		this(ChangeJournalEntry.COLLECTION_NAME, ChangeJournalMarker.COLLECTION_NAME);
	}

	/**
	 * Create a journal engine writing in specific collections.
	 * @param journalCollectionName collection storing the journal entries
	 * @param markerCollectionName collection storing the per-collection markers
	 */
	public ChangeJournalEngine(final String journalCollectionName, final String markerCollectionName) {
		if (journalCollectionName == null || journalCollectionName.isEmpty()) {
			throw new IllegalArgumentException("journalCollectionName must not be empty");
		}
		if (markerCollectionName == null || markerCollectionName.isEmpty()) {
			throw new IllegalArgumentException("markerCollectionName must not be empty");
		}
		if (journalCollectionName.equals(markerCollectionName)) {
			throw new IllegalArgumentException("journalCollectionName and markerCollectionName must differ");
		}
		this.journalCollectionName = journalCollectionName;
		this.markerCollectionName = markerCollectionName;
	}

	/**
	 * Returns the name of the collection storing the journal entries.
	 * @return the journal collection name
	 */
	public String getJournalCollectionName() {
		return this.journalCollectionName;
	}

	/**
	 * Returns the name of the collection storing the per-collection markers.
	 * @return the marker collection name
	 */
	public String getMarkerCollectionName() {
		return this.markerCollectionName;
	}

	/**
	 * Register model classes to journalize. The MongoDB collection name is resolved from the class
	 * annotations.
	 * @param classes the model classes to register
	 * @throws DataAccessException if a collection name cannot be resolved from the class annotations
	 */
	public void addClass(final Class<?>... classes) throws DataAccessException {
		for (final Class<?> clazz : classes) {
			final String name = AnnotationTools.getTableName(clazz, null);
			addCollection(name);
			final DbClassModel model;
			try {
				model = DbClassModel.of(clazz);
			} catch (final IntrospectionException ex) {
				throw new DataAccessException("Cannot analyse the model of " + clazz.getCanonicalName(), ex);
			}
			for (final DbPropertyDescriptor desc : model.getAllFields()) {
				// The secrets are left out; the system fields (createdAt, updatedAt, deleted) are
				// @DataNotRead too but drive the capture and are kept. @DataEncrypt stays encrypted.
				if (desc.isNotRead() && !desc.isGenericField()) {
					excludeFields(name, desc.getDbFieldName());
				}
			}
			// Every class is walked at capture time, the walker decides what is protected (embedded objects,
			// polymorphic values...). Several classes on one collection: all of them are applied.
			try {
				this.nestedProtection.computeIfAbsent(name, it -> new ArrayList<>()).add(FieldPathCursor.of(clazz));
			} catch (final IntrospectionException ex) {
				throw new DataAccessException("Cannot analyse the model of " + clazz.getCanonicalName(), ex);
			}
		}
	}

	/**
	 * Leave fields out of the capture of a collection (in addition to the {@code @DataNotRead} fields of a
	 * class registered with {@link #addClass}). Useful for the collections registered by name or captured
	 * by {@link #runAll()}.
	 * @param collectionName the source collection name
	 * @param fieldNames the database field names (top level) to leave out
	 */
	public void excludeFields(final String collectionName, final String... fieldNames) {
		final Set<String> set = this.excludedFields.computeIfAbsent(collectionName, it -> new LinkedHashSet<>());
		for (final String field : fieldNames) {
			if (!"_id".equals(field)) {
				set.add(field);
			}
		}
	}

	/**
	 * Returns the database field names left out of the capture of a collection.
	 * @param collectionName the source collection name
	 * @return the excluded field names (empty when none)
	 */
	public Set<String> getExcludedFields(final String collectionName) {
		return Set.copyOf(this.excludedFields.getOrDefault(collectionName, Set.of()));
	}

	/**
	 * Register collection names to journalize. The journal and marker collections are silently
	 * rejected: journalizing the journal would loop.
	 * @param names the MongoDB collection names to journalize
	 */
	public void addCollection(final String... names) {
		for (final String name : names) {
			if (isInternalCollection(name)) {
				LOGGER.warn("Reject the journal internal collection '{}' from the journalized collections", name);
				continue;
			}
			if (!this.collectionNames.contains(name)) {
				this.collectionNames.add(name);
			}
		}
	}

	/**
	 * Returns the currently registered source collection names.
	 * @return the registered collection names
	 */
	public List<String> getCollections() {
		return List.copyOf(this.collectionNames);
	}

	/**
	 * Set the maximum number of journal entries written by a single {@code insertMany}.
	 * @param value maximum number of entries per batch (default {@link #DEFAULT_BATCH_MAX_DOCUMENTS})
	 */
	public void setBatchMaxDocuments(final int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("batchMaxDocuments must be > 0");
		}
		this.batchMaxDocuments = value;
	}

	/**
	 * Set the maximum accumulated serialized size (in bytes) of a single {@code insertMany}.
	 * The first threshold reached (bytes or documents) triggers the write, which bounds the memory
	 * usage of a run whatever the source collection size is.
	 * @param value maximum batch size in bytes (default {@link #DEFAULT_BATCH_MAX_BYTES})
	 */
	public void setBatchMaxBytes(final int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("batchMaxBytes must be > 0");
		}
		this.batchMaxBytes = value;
	}

	/**
	 * Set a safety overlap subtracted from the marker when selecting the modified documents.
	 * <p>
	 * Useful when the {@code updatedAt} dates are generated by several hosts whose clocks may drift
	 * slightly: a document dated a few milliseconds in the past of the marker would otherwise be
	 * missed. The price of the overlap is duplicated entries for the documents modified inside the
	 * window — harmless in an append-only journal. Defaults to {@code 0} (no overlap).
	 * @param millis the overlap in milliseconds (must be non-negative)
	 */
	public void setSafetyOverlapMillis(final long millis) {
		if (millis < 0) {
			throw new IllegalArgumentException("safetyOverlapMillis must be >= 0");
		}
		this.safetyOverlapMillis = millis;
	}

	/**
	 * Returns the safety overlap subtracted from the marker, in milliseconds.
	 * @return the overlap in milliseconds
	 */
	public long getSafetyOverlapMillis() {
		return this.safetyOverlapMillis;
	}

	/**
	 * Set what the very first capture of a collection (no marker yet) does.
	 * @param value {@link InitialCapture#FULL} to start the journal with a full snapshot (default),
	 *        {@link InitialCapture#SKIP} to only set the marker
	 */
	public void setInitialCapture(final InitialCapture value) {
		if (value == null) {
			throw new IllegalArgumentException("initialCapture must not be null");
		}
		this.initialCapture = value;
	}

	/**
	 * Returns the behaviour of the very first capture of a collection.
	 * @return the initial capture mode
	 */
	public InitialCapture getInitialCapture() {
		return this.initialCapture;
	}

	/**
	 * Journalize the registered collections (see {@link #addClass} / {@link #addCollection}).
	 * @return the report of the run
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public ChangeJournalReport run() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			return execute(db, this.collectionNames);
		}
	}

	/**
	 * Journalize every collection discovered in the database, excluding the {@code system.*}
	 * collections and the journal's own collections. Does not require any prior
	 * {@link #addClass}/{@link #addCollection} call.
	 * @return the report of the run
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public ChangeJournalReport runAll() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			final List<String> allNames = db.listCollectionNames().into(new ArrayList<>());
			final List<String> filtered = allNames.stream()//
					.filter(name -> !name.startsWith("system."))//
					.filter(name -> !isInternalCollection(name))//
					.sorted()//
					.toList();
			LOGGER.info("Journal: discovered {} collections (filtered from {} total)", filtered.size(),
					allNames.size());
			return execute(db, filtered);
		}
	}

	/**
	 * Wrap {@link #run()} in a {@link Runnable} suitable for a
	 * {@link org.atriasoft.archidata.cron.CronScheduler}. Any failure is logged, never propagated:
	 * a failed collection simply keeps its marker and is retried at the next run.
	 * @return the task to schedule
	 */
	public Runnable asCronTask() {
		return asCronTask(false);
	}

	/**
	 * Wrap {@link #run()} or {@link #runAll()} in a {@link Runnable} suitable for a
	 * {@link org.atriasoft.archidata.cron.CronScheduler}. Any failure is logged, never propagated.
	 * @param allCollections {@code true} to journalize every discovered collection
	 *        ({@link #runAll()}), {@code false} to journalize the registered ones ({@link #run()})
	 * @return the task to schedule
	 */
	public Runnable asCronTask(final boolean allCollections) {
		return () -> {
			try {
				final ChangeJournalReport report = allCollections ? runAll() : run();
				LOGGER.info("Journal: {} entry(ies) recorded at {} ({} collection(s) failed)", report.totalCaptured(),
						report.runDate(), report.failedCollections().size());
			} catch (final Exception ex) {
				LOGGER.error("Journal: capture run failed: {}", ex.getMessage(), ex);
			}
		};
	}

	/**
	 * Read the current markers, one per journalized collection.
	 * @return the markers, ordered by collection name
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public List<ChangeJournalMarker> getMarkers() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			final List<ChangeJournalMarker> out = new ArrayList<>();
			for (final Document doc : db.getCollection(this.markerCollectionName).find()
					.sort(Indexes.ascending("collectionName"))) {
				out.add(toMarker(doc));
			}
			return out;
		}
	}

	/**
	 * Drop the marker of a collection: its next capture behaves like a first capture (see
	 * {@link #setInitialCapture(InitialCapture)}).
	 * @param collectionName the source collection whose marker is removed
	 * @return {@code true} if a marker was actually removed
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public boolean resetMarker(final String collectionName) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			return db.getCollection(this.markerCollectionName).deleteOne(new Document("collectionName", collectionName))
					.getDeletedCount() > 0;
		}
	}

	/**
	 * Drop every marker: the next run behaves like a first capture for all the collections.
	 * @return the number of removed markers
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public long resetAllMarkers() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			return db.getCollection(this.markerCollectionName).deleteMany(new Document()).getDeletedCount();
		}
	}

	/** Tells whether the given name is one of the journal's own collections. */
	private boolean isInternalCollection(final String name) {
		return this.journalCollectionName.equals(name) || this.markerCollectionName.equals(name);
	}

	/**
	 * Capture every given collection. Each collection is independent: a failure is recorded in the
	 * report and leaves its marker untouched, the other collections are still captured.
	 */
	private ChangeJournalReport execute(final MongoDatabase db, final List<String> names) {
		// Freeze the upper bound before reading anything: a document modified while the run is in
		// progress is dated after the bound, so it is left for the next run instead of being lost.
		final Date runDate = new Date();
		ensureIndexes(db);
		final MongoCollection<Document> journal = db.getCollection(this.journalCollectionName);
		final MongoCollection<Document> markers = db.getCollection(this.markerCollectionName);
		// LinkedHashMap: keep the collection order of the run in the report.
		final Map<String, Long> captured = new LinkedHashMap<>();
		final List<String> failed = new ArrayList<>();
		for (final String name : names) {
			try {
				final long count = journalizeCollection(db, name, runDate, journal, markers);
				captured.put(name, count);
			} catch (final Exception ex) {
				LOGGER.error("Journal: fail to capture the collection '{}': {}", name, ex.getMessage(), ex);
				failed.add(name);
			}
		}
		return new ChangeJournalReport(runDate, captured, failed);
	}

	/** Capture one collection and move its marker forward. Returns the number of written entries. */
	private long journalizeCollection(
			final MongoDatabase db,
			final String name,
			final Date runDate,
			final MongoCollection<Document> journal,
			final MongoCollection<Document> markers) {
		final Document marker = markers.find(new Document("collectionName", name)).first();
		final Date lastSavedAt = marker == null ? null : marker.getDate("lastSavedAt");
		final long previousTotal = marker == null ? 0L
				: marker.get("totalCount", Number.class) == null ? 0L
						: marker.get("totalCount", Number.class).longValue();
		long count = 0L;
		if (lastSavedAt == null && this.initialCapture == InitialCapture.SKIP) {
			LOGGER.info("Journal: '{}' first capture skipped (initialCapture=SKIP)", name);
		} else {
			count = captureDocuments(db, name, lastSavedAt, runDate, journal);
		}
		writeMarker(markers, name, runDate, count, previousTotal + count);
		LOGGER.info("Journal: '{}' captured {} entry(ies) (since {})", name, count,
				lastSavedAt == null ? "origin" : lastSavedAt);
		return count;
	}

	/** Stream the modified documents of a collection into the journal, batch by batch. */
	private long captureDocuments(
			final MongoDatabase db,
			final String name,
			final Date lastSavedAt,
			final Date runDate,
			final MongoCollection<Document> journal) {
		final JsonWriterSettings settings = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
		final Document filter = buildFilter(lastSavedAt, runDate);
		final List<Document> batch = new ArrayList<>();
		long batchBytes = 0L;
		long count = 0L;
		final Set<String> excluded = this.excludedFields.getOrDefault(name, Set.of());
		final FindIterable<Document> base = db.getCollection(name).find(filter).batchSize(CURSOR_BATCH_SIZE);
		final FindIterable<Document> find = excluded.isEmpty() ? base
				: base.projection(Projections.exclude(new ArrayList<>(excluded)));
		final List<FieldPathCursor> nested = this.nestedProtection.getOrDefault(name, List.of());
		for (final Document doc : find) {
			for (final FieldPathCursor root : nested) {
				removeNestedSecrets(doc, root);
			}
			final String json = doc.toJson(settings);
			batch.add(buildEntry(name, doc, json, runDate));
			batchBytes += json.length();
			count++;
			if (batch.size() >= this.batchMaxDocuments || batchBytes >= this.batchMaxBytes) {
				flush(journal, batch);
				batchBytes = 0L;
			}
		}
		flush(journal, batch);
		return count;
	}

	/**
	 * Remove, at any depth, the {@code @DataNotRead} fields of the embedded objects, of the elements of the
	 * lists and sets, and of the values of the maps. The top level is already cut by the projection (and keeps
	 * its system fields); {@code @DataEncrypt} fields stay, in their stored (encrypted) form. Fails closed:
	 * below the root, a key the model does not know (stored by the class of the value, for example a field
	 * added by a subclass) and a position that cannot be resolved with certainty (polymorphic type, too
	 * deep) are dropped.
	 */
	private static void removeNestedSecrets(final Map<String, Object> doc, final FieldPathCursor position) {
		final Iterator<Map.Entry<String, Object>> iterator = doc.entrySet().iterator();
		while (iterator.hasNext()) {
			final Map.Entry<String, Object> entry = iterator.next();
			final FieldPathCursor next = position.member(entry.getKey());
			switch (next.status()) {
				case PROTECTED:
					if (position.depth() > 0 && next.field().isNotRead()) {
						iterator.remove();
					}
					break;
				case UNSAFE:
					iterator.remove();
					break;
				case UNKNOWN:
					if (position.depth() > 0) {
						iterator.remove();
					}
					break;
				case OK:
					if (!next.isFreeForm()) {
						removeNestedSecretsFromValue(entry.getValue(), next);
					}
					break;
				default:
					break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static void removeNestedSecretsFromValue(final Object value, final FieldPathCursor position) {
		if (value instanceof final Map<?, ?> sub) {
			removeNestedSecrets((Map<String, Object>) sub, position);
		} else if (value instanceof final List<?> list) {
			for (final Object item : list) {
				removeNestedSecretsFromValue(item, position);
			}
		}
	}

	/** Write the pending entries and clear the batch. No-op on an empty batch. */
	private void flush(final MongoCollection<Document> journal, final List<Document> batch) {
		if (batch.isEmpty()) {
			return;
		}
		// Unordered: one rejected entry must not silently drop the rest of the batch.
		journal.insertMany(batch, new InsertManyOptions().ordered(false));
		batch.clear();
	}

	/** Build the journal entry document of one captured source document. */
	private static Document buildEntry(
			final String collectionName,
			final Document source,
			final String json,
			final Date runDate) {
		final Object id = source.get("_id");
		return new Document()//
				.append("collectionName", collectionName)//
				.append("sourceId", id == null ? null : id.toString())//
				.append("data", json)//
				.append("sourceUpdatedAt", sourceDate(source))//
				.append("recordedAt", runDate);
	}

	/** Date carried by a source document: {@code updatedAt} if any, {@code createdAt} otherwise. */
	private static Date sourceDate(final Document source) {
		final Object updated = source.get("updatedAt");
		if (updated instanceof final Date casted) {
			return casted;
		}
		final Object created = source.get("createdAt");
		if (created instanceof final Date casted) {
			return casted;
		}
		return null;
	}

	/**
	 * Build the MongoDB filter selecting the documents to capture.
	 * <p>
	 * First capture ({@code lastSavedAt == null}) selects everything. Incremental captures select
	 * the documents dated in {@code ]lastSavedAt - safetyOverlap ; runDate]}, using
	 * {@code updatedAt} and falling back to {@code createdAt} for the documents that carry no
	 * {@code updatedAt}. The upper bound is what makes a run repeatable: a document modified during
	 * the run is dated after it and is captured by the next run.
	 */
	private Document buildFilter(final Date lastSavedAt, final Date runDate) {
		if (lastSavedAt == null) {
			return new Document();
		}
		final Date lowerBound = this.safetyOverlapMillis == 0L ? lastSavedAt
				: new Date(lastSavedAt.getTime() - this.safetyOverlapMillis);
		final Document range = new Document("$gt", lowerBound).append("$lte", runDate);
		return new Document("$or", List.of(//
				new Document("updatedAt", range), //
				new Document("updatedAt", new Document("$exists", false)).append("createdAt", range)));
	}

	/** Move the marker of a collection forward, creating it if needed. */
	private void writeMarker(
			final MongoCollection<Document> markers,
			final String name,
			final Date runDate,
			final long count,
			final long totalCount) {
		markers.updateOne(new Document("collectionName", name), //
				new Document("$set", new Document("collectionName", name)//
						.append("lastSavedAt", runDate)//
						.append("lastRunAt", runDate)//
						.append("lastCount", count)//
						.append("totalCount", totalCount)), //
				new UpdateOptions().upsert(true));
	}

	/** Convert a raw marker document into its model. */
	private static ChangeJournalMarker toMarker(final Document doc) {
		final ChangeJournalMarker out = new ChangeJournalMarker();
		out.setOid(doc.getObjectId("_id"));
		out.setCollectionName(doc.getString("collectionName"));
		out.setLastSavedAt(doc.getDate("lastSavedAt"));
		out.setLastRunAt(doc.getDate("lastRunAt"));
		final Number last = doc.get("lastCount", Number.class);
		out.setLastCount(last == null ? 0L : last.longValue());
		final Number total = doc.get("totalCount", Number.class);
		out.setTotalCount(total == null ? 0L : total.longValue());
		return out;
	}

	/**
	 * Create the journal indexes once per engine instance ({@code createIndex} is idempotent, but
	 * it is a round-trip: no need to pay it at every run).
	 */
	private void ensureIndexes(final MongoDatabase db) {
		if (this.indexesEnsured) {
			return;
		}
		// Read the journal of one collection, most recent first.
		db.getCollection(this.journalCollectionName).createIndex(Indexes.compoundIndex(//
				Indexes.ascending("collectionName"), Indexes.descending("recordedAt")));
		// History of one source document, most recent first: also the exact order the purge walks.
		db.getCollection(this.journalCollectionName).createIndex(Indexes.compoundIndex(//
				Indexes.ascending("collectionName"), Indexes.ascending("sourceId"), //
				Indexes.descending("recordedAt"), Indexes.descending("_id")));
		// One marker per collection, enforced by the database itself.
		db.getCollection(this.markerCollectionName).createIndex(Indexes.ascending("collectionName"),
				new IndexOptions().unique(true));
		this.indexesEnsured = true;
	}
}
