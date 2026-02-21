package test.atriasoft.archidata.dataAccess.decorators;

import java.io.IOException;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.dataAccess.options.ForceReadOnlyField;
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
class TestApiReadOnlyField {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public String editable;
		@ApiReadOnly
		public String readOnly;
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
	void testInsertBothFields() throws Exception {
		final Model test = new Model();
		test.editable = "editable_initial";
		test.readOnly = "readonly_initial";
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertEquals("editable_initial", retrieved.editable);
		Assertions.assertEquals("readonly_initial", retrieved.readOnly);
		idOfTheObject = retrieved.id;
	}

	@Order(2)
	@Test
	void testUpdateReadOnlyIgnored() throws Exception {
		final Model update = new Model();
		update.editable = "editable_modified";
		update.readOnly = "readonly_modified";
		ConfigureDb.da.updateById(update, idOfTheObject);

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertEquals("editable_modified", retrieved.editable);
		Assertions.assertEquals("readonly_initial", retrieved.readOnly);
	}

	@Order(3)
	@Test
	void testUpdateReadOnlyWithForce() throws Exception {
		final Model update = new Model();
		update.editable = "editable_forced";
		update.readOnly = "readonly_forced";
		ConfigureDb.da.updateById(update, idOfTheObject, new ForceReadOnlyField());

		final Model retrieved = ConfigureDb.da.getById(Model.class, idOfTheObject);
		Assertions.assertEquals("editable_forced", retrieved.editable);
		Assertions.assertEquals("readonly_forced", retrieved.readOnly);
	}
}
