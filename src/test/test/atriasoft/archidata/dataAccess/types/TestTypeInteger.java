package test.atriasoft.archidata.dataAccess.types;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.bson.types.ObjectId;

import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestTypeInteger {

	public static class Model {
		@Id
		public ObjectId _id = null;
		public Integer data;
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
	void testInsertAndRetrieve() throws Exception {
		final Model test = new Model();
		test.data = 256;
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(256, retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.data = 10;
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = 999;
		ConfigureDb.da.updateById(inserted, inserted._id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(999, retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(3)
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

	@Order(4)
	@Test
	void testDelete() throws Exception {
		final Model test = new Model();
		test.data = 42;
		final Model inserted = ConfigureDb.da.insert(test);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNull(retrieved);
	}
}
