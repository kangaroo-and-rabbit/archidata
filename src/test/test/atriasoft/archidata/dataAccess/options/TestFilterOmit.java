package test.atriasoft.archidata.dataAccess.options;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestFilterOmit {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public String field1;
		public String field2;
		public Integer field3;
	}

	private static Long idOfTheObject = null;

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
		test.field1 = "original1";
		test.field2 = "original2";
		test.field3 = 100;
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted.id;
	}

	@Order(2)
	@Test
	void testPartialUpdateBlacklist() throws Exception {
		final Model update = new Model();
		update.field1 = "modified1";
		update.field2 = "modified2";
		update.field3 = 999;
		ConfigureDb.da.updateById(update, idOfTheObject, new FilterOmit("field2"));

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("modified1", retrieved.field1);
		Assertions.assertEquals("original2", retrieved.field2);
		Assertions.assertEquals(999, retrieved.field3);
	}
}
