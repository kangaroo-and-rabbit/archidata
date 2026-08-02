package test.atriasoft.archidata.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.index.IndexAction;
import org.atriasoft.archidata.index.IndexEngine;
import org.atriasoft.archidata.index.IndexPlan;
import org.atriasoft.archidata.index.IndexRegistry;
import org.atriasoft.archidata.index.IndexReport;
import org.atriasoft.archidata.index.IndexSpec;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.index.model.IndexModelEngine;
import test.atriasoft.archidata.index.model.IndexModelEngineChanged;

/**
 * Tests of the index synchronization against a real database: the diff, what it applies, and what
 * it refuses to touch.
 */
public class TestIndexEngine {

	private static final String COLLECTION = "IndexEngineTable";

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
	public void cleanState() {
		IndexRegistry.clear();
		collection().drop();
		ConfigureDb.da.getInterface().getDatabase().getCollection(IndexEngine.DEFAULT_CACHE_COLLECTION).drop();
	}

	private static MongoCollection<Document> collection() {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION);
	}

	/** Names of the indexes the database really holds. */
	private static Set<String> realIndexNames() {
		final List<String> names = new ArrayList<>();
		for (final Document index : collection().listIndexes()) {
			names.add(index.getString("name"));
		}
		return Set.copyOf(names);
	}

	private static Document realIndex(final String name) {
		for (final Document index : collection().listIndexes()) {
			if (name.equals(index.getString("name"))) {
				return index;
			}
		}
		return null;
	}

	private static IndexEngine engine(final Class<?>... classes) throws DataAccessException {
		final IndexEngine engine = new IndexEngine();
		engine.addClass(classes);
		return engine;
	}

	private static Set<String> namesOf(final IndexPlan plan, final IndexAction.Kind kind) {
		return plan.of(kind).stream().map(IndexAction::indexName).collect(Collectors.toSet());
	}

	// ========== plan ==========

	@Test
	public void testPlanOnAnEmptyDatabaseCreatesEverything() throws Exception {
		final IndexPlan plan = engine(IndexModelEngine.class).plan();

		Assertions.assertFalse(plan.isUpToDate());
		Assertions.assertEquals(Set.of("kar_companyId_1_createdOn_-1", "kar_reference_1_" + hashOfReference()),
				namesOf(plan, IndexAction.Kind.CREATE));
		// A plan changes nothing.
		Assertions.assertTrue(realIndexNames().isEmpty() || realIndexNames().equals(Set.of("_id_")));
	}

	/** The unique index on 'reference' carries options, so its name embeds a hash. */
	private static String hashOfReference() throws Exception {
		final String name = IndexSpec.asc("reference").unique().getName();
		return name.substring(name.lastIndexOf('_') + 1);
	}

	@Test
	public void testPlanIsUpToDateAfterASynchronization() throws Exception {
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.synchronize();

		final IndexPlan plan = engine.plan();
		Assertions.assertTrue(plan.isUpToDate(), plan.describe());
		Assertions.assertEquals(2, plan.of(IndexAction.Kind.KEEP).size());
	}

	// ========== synchronize ==========

	@Test
	public void testSynchronizeCreatesTheDeclaredIndexes() throws Exception {
		final IndexReport report = engine(IndexModelEngine.class).synchronize();

		Assertions.assertTrue(report.isSuccess());
		Assertions.assertEquals(2, report.createdCount());
		final Set<String> real = realIndexNames();
		Assertions.assertTrue(real.contains("_id_"));
		Assertions.assertTrue(real.contains("kar_companyId_1_createdOn_-1"), real.toString());
		// The options really landed in the database.
		final Document unique = realIndex(IndexSpec.asc("reference").unique().getName());
		Assertions.assertNotNull(unique);
		Assertions.assertEquals(Boolean.TRUE, unique.getBoolean("unique"));
		Assertions.assertEquals(new Document("reference", 1), unique.get("key"));
	}

	@Test
	public void testSynchronizeIsIdempotent() throws Exception {
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.synchronize();
		engine.setForceCheck(true);

		final IndexReport second = engine.synchronize();

		Assertions.assertEquals(0, second.createdCount());
		Assertions.assertEquals(0, second.droppedCount());
	}

	@Test
	public void testUndeclaredIndexIsDropped() throws Exception {
		engine(IndexModelEngine.class).synchronize();
		// The declaration changes: another entity mapped on the same collection declares only 'label'.
		final IndexEngine changed = engine(IndexModelEngineChanged.class);

		final IndexReport report = changed.synchronize();

		Assertions.assertEquals(1, report.createdCount());
		Assertions.assertEquals(2, report.droppedCount());
		Assertions.assertEquals(Set.of("_id_", "kar_label_1"), realIndexNames());
	}

	@Test
	public void testIdIndexIsNeverTouched() throws Exception {
		collection().insertOne(new Document("label", "keep the collection alive"));
		engine(IndexModelEngineChanged.class).synchronize();

		Assertions.assertTrue(realIndexNames().contains("_id_"));
		// Even a plan never proposes to touch it.
		final IndexPlan plan = engine(IndexModelEngineChanged.class).plan();
		Assertions.assertTrue(plan.actions().stream().noneMatch(action -> "_id_".equals(action.indexName())));
	}

	@Test
	public void testHandMadeIndexIsDroppedByDefault() throws Exception {
		collection().createIndex(new Document("label", 1), new com.mongodb.client.model.IndexOptions().name("by_hand"));
		Assertions.assertTrue(realIndexNames().contains("by_hand"));

		engine(IndexModelEngine.class).synchronize();

		// Strict by default: the database matches the code, nothing else.
		Assertions.assertFalse(realIndexNames().contains("by_hand"));
	}

	@Test
	public void testHandMadeIndexIsKeptWhenNotStrict() throws Exception {
		collection().createIndex(new Document("label", 1), new com.mongodb.client.model.IndexOptions().name("by_hand"));
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.setDropUnmanaged(false);

		final IndexPlan plan = engine.plan();
		engine.synchronize();

		Assertions.assertEquals(Set.of("by_hand"), namesOf(plan, IndexAction.Kind.PRESERVE));
		Assertions.assertTrue(realIndexNames().contains("by_hand"));
	}

	@Test
	public void testRedefinedIndexIsReplaced() throws Exception {
		// An index carrying the managed name, but with the wrong definition (as an older version
		// of the code would have left it).
		collection().createIndex(new Document("companyId", 1).append("createdOn", 1),
				new com.mongodb.client.model.IndexOptions().name("kar_companyId_1_createdOn_-1"));

		final IndexEngine engine = engine(IndexModelEngine.class);
		final IndexPlan plan = engine.plan();
		final IndexReport report = engine.synchronize();

		Assertions.assertEquals(Set.of("kar_companyId_1_createdOn_-1"), namesOf(plan, IndexAction.Kind.REPLACE));
		Assertions.assertTrue(report.isSuccess());
		final Document replaced = realIndex("kar_companyId_1_createdOn_-1");
		Assertions.assertEquals(new Document("companyId", 1).append("createdOn", -1), replaced.get("key"));
	}

	@Test
	public void testUnmanagedCollectionIsNeverTouched() throws Exception {
		final MongoCollection<Document> other = ConfigureDb.da.getInterface().getDatabase()
				.getCollection("IndexEngineUnmanaged");
		other.drop();
		other.createIndex(new Document("whatever", 1), new com.mongodb.client.model.IndexOptions().name("untouched"));

		engine(IndexModelEngine.class).synchronize();

		final List<String> names = new ArrayList<>();
		for (final Document index : other.listIndexes()) {
			names.add(index.getString("name"));
		}
		Assertions.assertTrue(names.contains("untouched"));
		other.drop();
	}

	// ========== failures ==========

	@Test
	public void testUniqueIndexOnDuplicatedDataFailsFast() throws Exception {
		collection().insertOne(new Document("reference", "same"));
		collection().insertOne(new Document("reference", "same"));

		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class,
				() -> engine(IndexModelEngine.class).synchronize());
		Assertions.assertTrue(ex.getMessage().contains("IndexEngineTable"), ex.getMessage());
	}

	@Test
	public void testFailureIsReportedWhenNotFailingFast() throws Exception {
		collection().insertOne(new Document("reference", "same"));
		collection().insertOne(new Document("reference", "same"));
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.setFailFast(false);

		final IndexReport report = engine.synchronize();

		Assertions.assertFalse(report.isSuccess());
		Assertions.assertEquals(1, report.failures().size());
		// The other index of the collection was still created.
		Assertions.assertTrue(realIndexNames().contains("kar_companyId_1_createdOn_-1"));
	}

	// ========== cache ==========

	@Test
	public void testCacheSkipsAnAlreadySynchronizedCollection() throws Exception {
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.synchronize();

		final IndexReport second = engine.synchronize();

		Assertions.assertEquals(List.of(COLLECTION), second.skippedCollections());
		Assertions.assertEquals(0, second.createdCount());
	}

	@Test
	public void testCacheIsInvalidatedByAChangedDeclaration() throws Exception {
		engine(IndexModelEngine.class).synchronize();

		// Different declarations on the same collection: the fingerprint differs, no skip.
		final IndexReport report = engine(IndexModelEngineChanged.class).synchronize();

		Assertions.assertTrue(report.skippedCollections().isEmpty());
		Assertions.assertEquals(1, report.createdCount());
	}

	@Test
	public void testStrictModeChangeInvalidatesTheCache() throws Exception {
		final IndexEngine strict = engine(IndexModelEngine.class);
		strict.synchronize();

		final IndexEngine tolerant = engine(IndexModelEngine.class);
		tolerant.setDropUnmanaged(false);
		final IndexReport report = tolerant.synchronize();

		// The option belongs to the fingerprint: the collection is re-checked, not skipped.
		Assertions.assertTrue(report.skippedCollections().isEmpty());
	}

	@Test
	public void testForceCheckIgnoresTheCache() throws Exception {
		final IndexEngine engine = engine(IndexModelEngine.class);
		engine.synchronize();
		// An index removed behind the engine's back: the cache alone would miss it.
		collection().dropIndex("kar_companyId_1_createdOn_-1");

		engine.setForceCheck(true);
		final IndexReport report = engine.synchronize();

		Assertions.assertEquals(1, report.createdCount());
		Assertions.assertTrue(realIndexNames().contains("kar_companyId_1_createdOn_-1"));
	}

	@Test
	public void testCacheCollectionIsNeverManaged() throws Exception {
		final IndexEngine engine = new IndexEngine();
		engine.addClass(IndexModelEngine.class);
		Assertions.assertEquals(List.of(COLLECTION), engine.getCollections());
	}
}
