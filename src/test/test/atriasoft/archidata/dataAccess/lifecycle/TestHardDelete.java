package test.atriasoft.archidata.dataAccess.lifecycle;

import java.io.IOException;

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
class TestHardDelete {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public String data;
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
		test.data = "hard_delete_test";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.id);
		idOfTheObject = inserted.id;
	}

	@Order(2)
	@Test
	void testRetrieveBeforeDelete() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("hard_delete_test", retrieved.data);
	}

	@Order(3)
	@Test
	void testDeleteAndVerifyGone() throws Exception {
		final long count = ConfigureDb.da.deleteById(Model.class, idOfTheObject);
		Assertions.assertEquals(1, count);

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNull(retrieved);
	}
}
