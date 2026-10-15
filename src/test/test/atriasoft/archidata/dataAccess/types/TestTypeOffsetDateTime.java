package test.atriasoft.archidata.dataAccess.types;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestTypeOffsetDateTime {

	public static class Model {
		@Id
		public ObjectId _id = null;
		public OffsetDateTime data;
	}

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testInsertAndRetrieveUtc() throws Exception {
		final Model test = new Model();
		// Truncate to millis since MongoDB Date has millisecond precision
		test.data = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(test.data, retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(2)
	@Test
	void testOffsetIsNormalizedToUtc() throws Exception {
		final Model test = new Model();
		test.data = OffsetDateTime.parse("2025-06-15T14:30:00.123+02:00");
		final Model inserted = ConfigureDb.da.insert(test);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		// Same instant, read back in UTC (the offset itself is not stored)
		Assertions.assertTrue(test.data.isEqual(retrieved.data));
		Assertions.assertEquals(ZoneOffset.UTC, retrieved.data.getOffset());
		Assertions.assertEquals(OffsetDateTime.parse("2025-06-15T12:30:00.123Z"), retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(3)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.data = OffsetDateTime.parse("2020-01-01T00:00:00Z");
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = OffsetDateTime.parse("2025-06-15T12:30:00Z");
		ConfigureDb.da.updateById(inserted, inserted._id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(OffsetDateTime.parse("2025-06-15T12:30:00Z"), retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(4)
	@Test
	void testNullValue() throws Exception {
		final Model test = new Model();
		test.data = null;
		final Model inserted = ConfigureDb.da.insert(test);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertNull(retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}
}
