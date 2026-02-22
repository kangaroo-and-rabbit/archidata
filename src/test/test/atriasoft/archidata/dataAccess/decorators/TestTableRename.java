package test.atriasoft.archidata.dataAccess.decorators;

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
import jakarta.persistence.Table;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestTableRename {

	@Table(name = "my_custom_collection")
	public static class Model {
		@Id
		public ObjectId _id = null;
		public String data;
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
		test.data = "in_custom_collection";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("in_custom_collection", retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.data = "initial";
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = "updated";
		ConfigureDb.da.updateById(inserted, inserted._id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted._id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("updated", retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted._id);
	}
}
