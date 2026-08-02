package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;
import java.util.TimeZone;

import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.SoftDeleteNoDefault;

/**
 * Tests of the soft-delete read filter: a plain equality on {@code false}, which is what makes a
 * partial index usable — and what requires the field to always be written.
 */
public class TestSoftDeleteFilter {

	private static final String COLLECTION = "SoftDeleteNoDefault";

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
	public void cleanCollection() {
		collection().drop();
	}

	private static MongoCollection<Document> collection() {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION);
	}

	@Test
	public void testDeletedIsWrittenEvenWithoutADeclaredDefault() throws Exception {
		final SoftDeleteNoDefault data = new SoftDeleteNoDefault();
		data.data = "hello";
		ConfigureDb.da.insert(data);

		final Document stored = collection().find().first();
		Assertions.assertNotNull(stored);
		// Without this, the document would be invisible to every read: the filter is an equality.
		Assertions.assertEquals(Boolean.FALSE, stored.getBoolean("deleted"));
	}

	@Test
	public void testFilterIsAPlainEquality() {
		final Document filter = Document
				.parse(new Condition().getFilter(COLLECTION, null, "deleted").toBsonDocument().toJson());

		// A '$or' would make any partial index on the living documents unusable.
		Assertions.assertEquals("{\"deleted\": false}", filter.toJson());
	}

	@Test
	public void testSoftDeletedItemsAreHiddenAndRestorable() throws Exception {
		final SoftDeleteNoDefault data = new SoftDeleteNoDefault();
		data.data = "hello";
		final ObjectId id = ConfigureDb.da.insert(data).getOid();

		Assertions.assertEquals(1, ConfigureDb.da.gets(SoftDeleteNoDefault.class).size());
		ConfigureDb.da.deleteSoftById(SoftDeleteNoDefault.class, id);
		Assertions.assertEquals(0, ConfigureDb.da.gets(SoftDeleteNoDefault.class).size());
		Assertions.assertEquals(1, ConfigureDb.da.gets(SoftDeleteNoDefault.class, new AccessDeletedItems()).size());

		// AccessDeletedItems is required: without it the restore filter excludes the very documents
		// it is meant to bring back.
		ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id, new AccessDeletedItems());
		Assertions.assertEquals(1, ConfigureDb.da.gets(SoftDeleteNoDefault.class).size());
	}

	@Test
	public void testDocumentWithoutTheFieldIsInvisible() throws Exception {
		// A document written outside of archidata, without the soft-delete field: it is now out of
		// reach of a normal read. Documented behaviour, and the reason the field is always written.
		collection().insertOne(new Document("data", "written by hand"));

		Assertions.assertEquals(0, ConfigureDb.da.gets(SoftDeleteNoDefault.class).size());
		Assertions.assertEquals(1, ConfigureDb.da.gets(SoftDeleteNoDefault.class, new AccessDeletedItems()).size());
	}

	@Test
	public void testPartialIndexOnLivingDocumentsIsUsed() throws Exception {
		for (int iii = 0; iii < 200; iii++) {
			final SoftDeleteNoDefault data = new SoftDeleteNoDefault();
			data.data = "value-" + iii % 10;
			ConfigureDb.da.insert(data);
		}
		// The index only holds the living documents: smaller, and exactly what the reads need.
		collection().createIndex(new Document("data", 1),
				new IndexOptions().name("kar_partial_living").partialFilterExpression(new Document("deleted", false)));

		final Document filter = Document.parse(new Condition(Filters.eq("data", "value-3"))
				.getFilter(COLLECTION, null, "deleted").toBsonDocument().toJson());
		final Document explain = collection().find(filter).explain();
		final String plan = explain.get("queryPlanner", Document.class).get("winningPlan", Document.class).toJson();

		// This is the whole point of the equality filter: with a '$or' this was a COLLSCAN.
		Assertions.assertTrue(plan.contains("kar_partial_living"), plan);
		Assertions.assertEquals(20,
				ConfigureDb.da.gets(SoftDeleteNoDefault.class, new Condition(Filters.eq("data", "value-3"))).size());
	}
}
