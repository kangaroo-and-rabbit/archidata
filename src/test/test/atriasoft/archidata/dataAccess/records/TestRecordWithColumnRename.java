package test.atriasoft.archidata.dataAccess.records;

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

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestRecordWithColumnRename {

	public static class Model {
		@Id
		public ObjectId _id = null;
		@Column(name = "custom_name")
		public String name;
		public int value;
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
		test.name = "renamed-field";
		test.value = 77;
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("renamed-field", retrieved.name);
		Assertions.assertEquals(77, retrieved.value);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.name = "original";
		test.value = 1;
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.name = "updated-renamed";
		inserted.value = 2;
		ConfigureDb.da.updateById(inserted, inserted._id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("updated-renamed", retrieved.name);
		Assertions.assertEquals(2, retrieved.value);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}
}
