package org.atriasoft.archidata.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReturnDocument;

/**
 * Brings the MongoDB indexes in line with what the code declares.
 *
 * <p>At startup, for every registered entity: the declared indexes (see {@link IndexRegistry}) are
 * compared with the indexes the database actually holds, and the difference is applied — missing
 * indexes are created, redefined ones are replaced, and indexes that are no longer declared are
 * dropped, so the database matches the code rather than accumulating the leftovers of every past
 * version.
 *
 * <pre>{@code
 * final IndexEngine indexes = new IndexEngine();
 * indexes.addClass(User.class, Media.class);
 * indexes.synchronize();
 * }</pre>
 *
 * <p><b>Independent of the migrations.</b> The engine never reads the migration state, so a server
 * that receives a duplicated database and never runs the migrations can still bring its indexes up
 * to date. The only thing it needs is the list of entities, which comes from the code.
 *
 * <p><b>What it never touches.</b> The {@code _id_} index — MongoDB creates it with the collection
 * and refuses to drop it — and every collection that is not registered. A server embedding only a
 * part of the model can therefore synchronize without disturbing the collections it ignores.
 *
 * <p><b>Foreign indexes.</b> By default an index that the code does not declare is dropped, which
 * is what makes the database strictly conform to the code. It also means an index created by hand
 * — including a text or geospatial index, which this engine cannot declare — is removed. Use
 * {@link #setDropUnmanaged(boolean)} to keep them instead: only the indexes named {@code kar_*}
 * are then managed.
 *
 * <p><b>Failure.</b> By default the first index that cannot be applied stops the synchronization
 * and propagates: a unique index that duplicated data rejects is a problem to fix, not to log and
 * forget while the server starts anyway.
 */
public class IndexEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(IndexEngine.class);

	/** Default collection caching what has already been synchronized. */
	public static final String DEFAULT_CACHE_COLLECTION = "KAR_index";
	/** The index MongoDB maintains itself: never dropped, never replaced. */
	public static final String ID_INDEX_NAME = "_id_";
	/** Identifier of the lock document inside the cache collection. */
	private static final String LOCK_ID = "__lock";
	/** How long a synchronization may hold the lock before another instance takes it over. */
	private static final long LOCK_TTL_MILLIS = 120_000L;
	/** Below this age, a cache entry written by another fingerprint means two versions are fighting. */
	private static final long FLAPPING_WINDOW_MILLIS = 600_000L;

	private final String cacheCollectionName;
	/** Registered entities, grouped by target collection: several classes may share a collection. */
	private final Map<String, List<Class<?>>> classesByCollection = new LinkedHashMap<>();
	private final String instanceId = UUID.randomUUID().toString();
	private boolean dropUnmanaged = true;
	private boolean failFast = true;
	private boolean useCache = true;
	private boolean forceCheck = false;

	/** Create an engine using the default cache collection ({@value #DEFAULT_CACHE_COLLECTION}). */
	public IndexEngine() {
		this(DEFAULT_CACHE_COLLECTION);
	}

	/**
	 * Create an engine caching its state in a specific collection.
	 * @param cacheCollectionName the collection storing the synchronization state
	 */
	public IndexEngine(final String cacheCollectionName) {
		if (cacheCollectionName == null || cacheCollectionName.isEmpty()) {
			throw new IllegalArgumentException("cacheCollectionName must not be empty");
		}
		this.cacheCollectionName = cacheCollectionName;
	}

	/**
	 * Register the entities whose indexes are managed.
	 * @param classes the model classes
	 * @throws DataAccessException if a collection name cannot be resolved from the class annotations
	 */
	public void addClass(final Class<?>... classes) throws DataAccessException {
		for (final Class<?> clazz : classes) {
			final String collectionName = AnnotationTools.getTableName(clazz, null);
			if (this.cacheCollectionName.equals(collectionName)) {
				LOGGER.warn("Reject the index cache collection '{}' from the managed collections", collectionName);
				continue;
			}
			final List<Class<?>> stored = this.classesByCollection.computeIfAbsent(collectionName,
					key -> new ArrayList<>());
			if (!stored.contains(clazz)) {
				stored.add(clazz);
			}
		}
	}

	/**
	 * Collections whose indexes are managed by this engine.
	 * @return the registered collection names
	 */
	public List<String> getCollections() {
		return List.copyOf(this.classesByCollection.keySet());
	}

	/**
	 * Drop the indexes the code does not declare.
	 * @param value {@code true} (default) to keep the database strictly conform to the code,
	 *        {@code false} to only manage the {@code kar_*} indexes
	 */
	public void setDropUnmanaged(final boolean value) {
		this.dropUnmanaged = value;
	}

	/**
	 * Stop at the first index that cannot be applied.
	 * @param value {@code true} (default) to propagate the failure, {@code false} to log it and
	 *        report it in the {@link IndexReport}
	 */
	public void setFailFast(final boolean value) {
		this.failFast = value;
	}

	/**
	 * Use the cache collection to skip the collections already known to be in sync.
	 * @param value {@code true} (default) to use the cache
	 */
	public void setUseCache(final boolean value) {
		this.useCache = value;
	}

	/**
	 * Ignore the cache for the next synchronization, and check every collection against the
	 * database. Useful after an index was changed by hand.
	 * @param value {@code true} to force a full check
	 */
	public void setForceCheck(final boolean value) {
		this.forceCheck = value;
	}

	/**
	 * Compute what a synchronization would do, without modifying anything. Always reads the real
	 * state of the database, cache or not: a plan that trusts a cache could not be reviewed.
	 *
	 * @return the plan
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if a declaration is invalid
	 */
	public IndexPlan plan() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			final List<IndexAction> actions = new ArrayList<>();
			for (final String collectionName : this.classesByCollection.keySet()) {
				actions.addAll(planCollection(db, collectionName, resolveWanted(collectionName)));
			}
			return new IndexPlan(actions);
		}
	}

	/**
	 * Bring the database in line with the declarations.
	 *
	 * @return what was applied
	 * @throws IOException if the database connection fails
	 * @throws DataAccessException if a declaration is invalid, or if an index cannot be applied
	 *         while the engine fails fast
	 */
	public IndexReport synchronize() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final MongoDatabase db = ctx.get().getInterface().getDatabase();
			final MongoCollection<Document> cache = db.getCollection(this.cacheCollectionName);
			final boolean locked = this.useCache && acquireLock(cache);
			if (this.useCache && !locked) {
				LOGGER.warn("Index synchronization: another instance holds the lock, skipping this run");
				return new IndexReport(Map.of(), Map.of(), Map.of(), getCollections());
			}
			try {
				return synchronizeAll(db, cache);
			} finally {
				if (locked) {
					releaseLock(cache);
				}
			}
		}
	}

	private IndexReport synchronizeAll(final MongoDatabase db, final MongoCollection<Document> cache)
			throws DataAccessException {
		final Map<String, List<String>> created = new LinkedHashMap<>();
		final Map<String, List<String>> dropped = new LinkedHashMap<>();
		final Map<String, String> failures = new LinkedHashMap<>();
		final List<String> skipped = new ArrayList<>();

		for (final String collectionName : this.classesByCollection.keySet()) {
			final List<IndexSpec> wanted = resolveWanted(collectionName);
			final String fingerprint = fingerprintOf(wanted);
			if (this.useCache && !this.forceCheck && isCachedInSync(cache, collectionName, fingerprint)) {
				skipped.add(collectionName);
				continue;
			}
			final List<IndexAction> actions = planCollection(db, collectionName, wanted);
			applyActions(db, collectionName, actions, created, dropped, failures);
			if (this.useCache && !failures.containsKey(collectionName)) {
				writeCache(cache, collectionName, fingerprint, wanted);
			}
		}
		final IndexReport report = new IndexReport(created, dropped, failures, skipped);
		LOGGER.info("Index synchronization: {}", report);
		return report;
	}

	// ========== Declarations ==========

	/** Every index wanted on a collection, merged across the classes that share it. */
	private List<IndexSpec> resolveWanted(final String collectionName) throws DataAccessException {
		final Map<String, IndexSpec> byName = new LinkedHashMap<>();
		for (final Class<?> clazz : this.classesByCollection.get(collectionName)) {
			for (final IndexSpec spec : IndexRegistry.resolve(clazz)) {
				final IndexSpec previous = byName.putIfAbsent(spec.getName(), spec);
				if (previous != null && !previous.equals(spec)) {
					throw new DataAccessException("Two entities mapped on '" + collectionName
							+ "' declare a different index under the name '" + spec.getName() + "'");
				}
			}
		}
		return List.copyOf(byName.values());
	}

	// ========== Diff ==========

	/** Compare the wanted indexes of one collection with the ones the database holds. */
	private List<IndexAction> planCollection(
			final MongoDatabase db,
			final String collectionName,
			final List<IndexSpec> wanted) {
		final Map<String, Document> existing = readExistingIndexes(db, collectionName);
		final List<IndexAction> actions = new ArrayList<>();
		final Set<String> wantedNames = new LinkedHashSet<>();

		for (final IndexSpec spec : wanted) {
			final String name = spec.getName();
			wantedNames.add(name);
			final Document current = existing.get(name);
			if (current == null) {
				actions.add(new IndexAction(collectionName, IndexAction.Kind.CREATE, name, spec, "missing"));
			} else if (sameDefinition(spec, current)) {
				actions.add(new IndexAction(collectionName, IndexAction.Kind.KEEP, name, spec, null));
			} else {
				// MongoDB cannot alter an index in place: it has to go and come back.
				actions.add(
						new IndexAction(collectionName, IndexAction.Kind.REPLACE, name, spec, "definition changed"));
			}
		}
		for (final Map.Entry<String, Document> entry : existing.entrySet()) {
			final String name = entry.getKey();
			if (wantedNames.contains(name) || ID_INDEX_NAME.equals(name)) {
				continue;
			}
			final boolean managed = name.startsWith(IndexSpec.MANAGED_PREFIX);
			if (managed || this.dropUnmanaged) {
				actions.add(new IndexAction(collectionName, IndexAction.Kind.DROP, name, null,
						managed ? "not declared any more" : "not declared by the code"));
			} else {
				actions.add(new IndexAction(collectionName, IndexAction.Kind.PRESERVE, name, null,
						"not managed by the code"));
			}
		}
		return actions;
	}

	/** Existing indexes of a collection, by name. An unknown collection simply has none. */
	private static Map<String, Document> readExistingIndexes(final MongoDatabase db, final String collectionName) {
		final Map<String, Document> out = new LinkedHashMap<>();
		try {
			for (final Document index : db.getCollection(collectionName).listIndexes()) {
				final String name = index.getString("name");
				if (name != null) {
					out.put(name, index);
				}
			}
		} catch (final MongoException ex) {
			// The collection does not exist yet: nothing is indexed, everything will be created.
			LOGGER.debug("No index readable on '{}': {}", collectionName, ex.getMessage());
		}
		return out;
	}

	/** Tells whether an existing index matches a declaration, options included. */
	static boolean sameDefinition(final IndexSpec spec, final Document existing) {
		final Document wantedKeys = spec.toKeysDocument();
		final Object rawKeys = existing.get("key");
		if (!(rawKeys instanceof final Document currentKeys)) {
			return false;
		}
		if (currentKeys.size() != wantedKeys.size()) {
			return false;
		}
		final var wantedIterator = wantedKeys.entrySet().iterator();
		final var currentIterator = currentKeys.entrySet().iterator();
		while (wantedIterator.hasNext()) {
			final var wantedEntry = wantedIterator.next();
			final var currentEntry = currentIterator.next();
			if (!wantedEntry.getKey().equals(currentEntry.getKey())) {
				return false;
			}
			if (!(currentEntry.getValue() instanceof final Number currentOrder)
					|| ((Number) wantedEntry.getValue()).intValue() != currentOrder.intValue()) {
				return false;
			}
		}
		if (spec.isUnique() != booleanOf(existing, "unique") || spec.isSparse() != booleanOf(existing, "sparse")) {
			return false;
		}
		final Object ttl = existing.get("expireAfterSeconds");
		final long currentTtl = ttl instanceof final Number number ? number.longValue() : -1;
		if (spec.getExpireAfterSeconds() != currentTtl) {
			return false;
		}
		final Object partial = existing.get("partialFilterExpression");
		if (spec.getPartialFilter().isEmpty()) {
			return partial == null;
		}
		return partial instanceof final Document currentPartial
				&& currentPartial.equals(Document.parse(spec.getPartialFilter()));
	}

	private static boolean booleanOf(final Document document, final String key) {
		final Object value = document.get(key);
		return value instanceof final Boolean casted && casted.booleanValue();
	}

	// ========== Apply ==========

	private void applyActions(
			final MongoDatabase db,
			final String collectionName,
			final List<IndexAction> actions,
			final Map<String, List<String>> created,
			final Map<String, List<String>> dropped,
			final Map<String, String> failures) throws DataAccessException {
		final MongoCollection<Document> collection = db.getCollection(collectionName);
		// Replacements first: they free a name a creation may need. Then the creations, so a
		// crash in the middle leaves the collection over-indexed (slow) rather than under-indexed
		// (queries without an index). The obsolete drops come last, for the same reason.
		for (final IndexAction action : actions) {
			try {
				switch (action.kind()) {
					case REPLACE -> {
						collection.dropIndex(action.indexName());
						createIndex(collection, action.spec());
						record(dropped, collectionName, action.indexName());
						record(created, collectionName, action.indexName());
						LOGGER.info("Index replaced: {}.{}", collectionName, action.indexName());
					}
					case CREATE -> {
						createIndex(collection, action.spec());
						record(created, collectionName, action.indexName());
						LOGGER.info("Index created: {}.{}", collectionName, action.indexName());
					}
					default -> {
						// KEEP and PRESERVE need nothing; DROP is applied below.
					}
				}
			} catch (final MongoException ex) {
				handleFailure(collectionName, action, ex, failures);
			}
		}
		for (final IndexAction action : actions) {
			if (action.kind() != IndexAction.Kind.DROP) {
				continue;
			}
			try {
				collection.dropIndex(action.indexName());
				record(dropped, collectionName, action.indexName());
				LOGGER.info("Index dropped: {}.{} ({})", collectionName, action.indexName(), action.detail());
			} catch (final MongoException ex) {
				handleFailure(collectionName, action, ex, failures);
			}
		}
	}

	private void handleFailure(
			final String collectionName,
			final IndexAction action,
			final MongoException ex,
			final Map<String, String> failures) throws DataAccessException {
		final String message = action.kind() + " " + collectionName + "." + action.indexName() + ": " + ex.getMessage();
		if (this.failFast) {
			throw new DataAccessException("Index synchronization failed on " + message, ex);
		}
		LOGGER.error("Index synchronization failed on {}", message);
		failures.put(collectionName, message);
	}

	private static void createIndex(final MongoCollection<Document> collection, final IndexSpec spec) {
		final IndexOptions options = new IndexOptions().name(spec.getName());
		if (spec.isUnique()) {
			options.unique(true);
		}
		if (spec.isSparse()) {
			options.sparse(true);
		}
		if (spec.getExpireAfterSeconds() >= 0) {
			options.expireAfter(spec.getExpireAfterSeconds(), java.util.concurrent.TimeUnit.SECONDS);
		}
		if (!spec.getPartialFilter().isEmpty()) {
			options.partialFilterExpression(Document.parse(spec.getPartialFilter()));
		}
		collection.createIndex(spec.toKeysDocument(), options);
	}

	private static void record(final Map<String, List<String>> target, final String collectionName, final String name) {
		target.computeIfAbsent(collectionName, key -> new ArrayList<>()).add(name);
	}

	// ========== Cache ==========

	/** Canonical fingerprint of what the code wants on a collection, engine options included. */
	private String fingerprintOf(final List<IndexSpec> specs) {
		final List<String> signatures = new ArrayList<>(specs.size());
		for (final IndexSpec spec : specs) {
			signatures.add(spec.getName() + "=" + spec.definitionSignature());
		}
		// Sorted: the declaration order must not change the fingerprint.
		signatures.sort(null);
		// The option belongs to the fingerprint: turning the strict mode on must invalidate a
		// cache written while foreign indexes were tolerated.
		signatures.add("dropUnmanaged=" + this.dropUnmanaged);
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(String.join("\n", signatures).getBytes(StandardCharsets.UTF_8));
			final StringBuilder out = new StringBuilder(hash.length * 2);
			for (final byte value : hash) {
				out.append(String.format("%02x", value));
			}
			return out.toString();
		} catch (final NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	/**
	 * Tells whether the cache proves the collection is already in sync — and warns when another
	 * instance recently synchronized the same collection with a different fingerprint, which means
	 * two versions of the code are undoing each other's indexes at every restart.
	 */
	private boolean isCachedInSync(
			final MongoCollection<Document> cache,
			final String collectionName,
			final String fingerprint) {
		final Document entry = cache.find(new Document("_id", collectionName)).first();
		if (entry == null) {
			return false;
		}
		if (fingerprint.equals(entry.getString("fingerprint"))) {
			return true;
		}
		final Date syncedAt = entry.getDate("syncedAt");
		if (syncedAt != null && System.currentTimeMillis() - syncedAt.getTime() < FLAPPING_WINDOW_MILLIS) {
			LOGGER.warn(
					"Index cache: '{}' was synchronized {} ms ago with a different declaration. Two versions of the "
							+ "code are likely fighting over the same indexes, rebuilding them at every restart.",
					collectionName, System.currentTimeMillis() - syncedAt.getTime());
		}
		return false;
	}

	private void writeCache(
			final MongoCollection<Document> cache,
			final String collectionName,
			final String fingerprint,
			final List<IndexSpec> specs) {
		final List<String> names = new ArrayList<>(specs.size());
		for (final IndexSpec spec : specs) {
			names.add(spec.getName());
		}
		cache.replaceOne(new Document("_id", collectionName), new Document("_id", collectionName)//
				.append("fingerprint", fingerprint)//
				.append("indexes", names)//
				.append("syncedAt", new Date())//
				.append("owner", this.instanceId), //
				new com.mongodb.client.model.ReplaceOptions().upsert(true));
	}

	// ========== Lock ==========

	/**
	 * Take the synchronization lock, so two instances starting together do not drop and recreate
	 * the same indexes concurrently. Returns {@code false} when someone else holds it.
	 */
	private boolean acquireLock(final MongoCollection<Document> cache) {
		final Date now = new Date();
		final Date until = new Date(now.getTime() + LOCK_TTL_MILLIS);
		final Document filter = new Document("_id", LOCK_ID).append("$or", List.of(//
				new Document("lockedUntil", new Document("$exists", false)), //
				new Document("lockedUntil", new Document("$lt", now))));
		final Document update = new Document("$set",
				new Document("lockedUntil", until).append("owner", this.instanceId));
		try {
			final Document result = cache.findOneAndUpdate(filter, update,
					new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
			return result != null;
		} catch (final MongoException ex) {
			// Duplicate key: the lock document exists and the filter did not match it, so it is
			// held by someone else whose lease has not expired.
			LOGGER.debug("Index lock not acquired: {}", ex.getMessage());
			return false;
		}
	}

	private void releaseLock(final MongoCollection<Document> cache) {
		try {
			cache.updateOne(new Document("_id", LOCK_ID).append("owner", this.instanceId),
					new Document("$unset", new Document("lockedUntil", "")));
		} catch (final MongoException ex) {
			// The lease expires on its own: a failed release only delays the next run.
			LOGGER.warn("Index lock not released: {}", ex.getMessage());
		}
	}
}
