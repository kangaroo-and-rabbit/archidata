package test.atriasoft.archidata.dataAccess.records;

import java.io.IOException;

import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
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
class TestRecordWithNotRead {

	public static class Model {
		@Id
		public ObjectId _id = null;
		public String visible;
		@DataNotRead
		public String hidden;
	}

	private static ObjectId idOfTheObject = null;

	@BeforeAll
	static void setup() throws Exception {
		idOfTheObject = null;
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testInsert() throws Exception {
		final Model test = new Model();
		test.visible = "I am visible";
		test.hidden = "I am hidden";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted._id;
	}

	@Order(2)
	@Test
	void testGetByIdDefault() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("I am visible", retrieved.visible);
		Assertions.assertNull(retrieved.hidden);
	}

	@Order(3)
	@Test
	void testGetByIdWithReadAllColumn() throws Exception {
		// @DataNotRead fields are written to DB but not read by default.
		// With ReadAllColumn, they should be retrieved.
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("I am visible", retrieved.visible);
		Assertions.assertEquals("I am hidden", retrieved.hidden);
	}

	@Order(4)
	@Test
	void testUpdateNotReadField() throws Exception {
		final Model update = new Model();
		update.visible = "visible updated";
		update.hidden = "hidden updated";
		ConfigureDb.da.updateById(update, idOfTheObject);
		// Default read should not return the hidden field
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("visible updated", retrieved.visible);
		Assertions.assertNull(retrieved.hidden);
		// With ReadAllColumn, the updated hidden field should be returned
		final Model retrievedAll = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrievedAll);
		Assertions.assertEquals("visible updated", retrievedAll.visible);
		Assertions.assertEquals("hidden updated", retrievedAll.hidden);
	}
}
