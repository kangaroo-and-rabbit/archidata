package test.atriasoft.archidata.dataAccess.decorators;

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

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestDataNotRead {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public String visible;
		@DataNotRead
		public String hidden;
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
		test.visible = "visible_value";
		test.hidden = "hidden_value";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		idOfTheObject = inserted.id;
	}

	@Order(2)
	@Test
	void testGetByIdDefault() throws Exception {
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("visible_value", retrieved.visible);
		// @DataNotRead fields are not read by default
		Assertions.assertNull(retrieved.hidden);
	}

	@Order(3)
	@Test
	void testGetByIdWithReadAllColumn() throws Exception {
		// @DataNotRead alone (without @CreationTimestamp/@UpdateTimestamp/@DataDeleted)
		// means the field is never written to DB, so it remains null even with ReadAllColumn
		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject, new ReadAllColumn());
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("visible_value", retrieved.visible);
		Assertions.assertNull(retrieved.hidden);
	}
}
