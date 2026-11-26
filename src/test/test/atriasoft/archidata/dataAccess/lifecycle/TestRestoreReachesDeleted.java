package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.SoftDeleteNoDefault;

/** A restore acts on soft-deleted documents: no overload needs {@code AccessDeletedItems} to reach them. */
class TestRestoreReachesDeleted {

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	private static MongoCollection<Document> collection() throws Exception {
		return ConfigureDb.da.getInterface().getDatabase().getCollection("SoftDeleteNoDefault");
	}

	private static ObjectId insertDeleted(final String data) throws Exception {
		final SoftDeleteNoDefault test = new SoftDeleteNoDefault();
		test.data = data;
		final ObjectId id = ConfigureDb.da.insert(test).getOid();
		ConfigureDb.da.deleteSoftById(SoftDeleteNoDefault.class, id);
		return id;
	}

	private static Object deletedFlag(final ObjectId id) throws Exception {
		return collection().find(new Document("_id", id)).first().get("deleted");
	}

	@Test
	void testRestoreByIdWithoutOption() throws Exception {
		final ObjectId id = insertDeleted("by-id-no-option");
		Assertions.assertEquals(1, ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id));
		Assertions.assertEquals(false, deletedFlag(id));
		Assertions.assertNotNull(ConfigureDb.da.getById(SoftDeleteNoDefault.class, id));
	}

	@Test
	void testRestoreByIdWithAnotherOptionOnly() throws Exception {
		final ObjectId id = insertDeleted("by-id-other-option");
		Assertions.assertEquals(1, ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id, new ReadAllColumn()));
		Assertions.assertEquals(false, deletedFlag(id));
	}

	@Test
	void testRestoreWithAConditionOnly() throws Exception {
		final ObjectId id = insertDeleted("by-condition");
		Assertions.assertEquals(1,
				ConfigureDb.da.restore(SoftDeleteNoDefault.class, new Condition(Filters.eq("_id", id))));
		Assertions.assertEquals(false, deletedFlag(id));
	}

	@Test
	void testRestoreWithAccessDeletedItemsStillWorks() throws Exception {
		final ObjectId id = insertDeleted("by-id-explicit");
		Assertions.assertEquals(1, ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id, new AccessDeletedItems()));
		Assertions.assertEquals(false, deletedFlag(id));
	}

	@Test
	void testRestoreLeavesALivingDocumentUntouched() throws Exception {
		final SoftDeleteNoDefault test = new SoftDeleteNoDefault();
		test.data = "alive";
		final ObjectId id = ConfigureDb.da.insert(test).getOid();
		final Date old = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
		collection().updateOne(new Document("_id", id), new Document("$set", new Document("updatedAt", old)));

		Assertions.assertEquals(0, ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id));

		Assertions.assertEquals(old, collection().find(new Document("_id", id)).first().getDate("updatedAt"));
	}
}
