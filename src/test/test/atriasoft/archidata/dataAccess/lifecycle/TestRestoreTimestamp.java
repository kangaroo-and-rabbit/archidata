package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.bson.Document;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.SoftDeleteNoDefault;

/** Undoing a soft delete is a change like the soft delete itself: what follows {@code updatedAt} has to see it. */
class TestRestoreTimestamp {

	/** A soft delete on a model that has no {@code @UpdateTimestamp}. */
	public static class ModelWithoutTiming {
		@Id
		@BsonId
		@Field("_id")
		public ObjectId oid;
		public String data;
		@DataDeleted
		public Boolean deleted = null;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	private static MongoCollection<Document> collection(final String name) throws Exception {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(name);
	}

	@Test
	void testARestoreSetsUpdatedAt() throws Exception {
		final SoftDeleteNoDefault test = new SoftDeleteNoDefault();
		test.data = "restore";
		final ObjectId id = ConfigureDb.da.insert(test).getOid();
		ConfigureDb.da.deleteSoftById(SoftDeleteNoDefault.class, id);
		final Date old = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
		final MongoCollection<Document> documents = collection("SoftDeleteNoDefault");
		documents.updateOne(new Document("_id", id), new Document("$set", new Document("updatedAt", old)));

		Assertions.assertEquals(1, ConfigureDb.da.restoreById(SoftDeleteNoDefault.class, id, new AccessDeletedItems()));

		final Document restored = documents.find(new Document("_id", id)).first();
		Assertions.assertEquals(false, restored.get("deleted"));
		Assertions.assertTrue(restored.getDate("updatedAt").after(old), restored.toString());
	}

	@Test
	void testARestoreOfAModelWithoutUpdateTimestamp() throws Exception {
		final ModelWithoutTiming test = new ModelWithoutTiming();
		test.data = "restore";
		final ObjectId id = ConfigureDb.da.insert(test).oid;
		ConfigureDb.da.deleteSoftById(ModelWithoutTiming.class, id);

		Assertions.assertEquals(1, ConfigureDb.da.restoreById(ModelWithoutTiming.class, id, new AccessDeletedItems()));

		final Document restored = collection("ModelWithoutTiming").find(new Document("_id", id)).first();
		Assertions.assertEquals(false, restored.get("deleted"));
		Assertions.assertFalse(restored.containsKey("updatedAt"), restored.toString());
	}
}
