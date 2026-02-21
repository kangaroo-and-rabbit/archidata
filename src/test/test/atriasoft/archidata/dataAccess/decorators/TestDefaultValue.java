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

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.ws.rs.DefaultValue;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestDefaultValue {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		@DefaultValue("'default_text'")
		public String textData = null;
		@DefaultValue("'42'")
		public Integer intData = null;
		@DefaultValue("'0'")
		public Boolean boolData = null;
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
	void testDefaultsApplied() throws Exception {
		final Model test = new Model();
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("default_text", retrieved.textData);
		Assertions.assertEquals(42, retrieved.intData);
		Assertions.assertEquals(false, retrieved.boolData);

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}

	@Order(2)
	@Test
	void testExplicitValuesOverrideDefaults() throws Exception {
		final Model test = new Model();
		test.textData = "explicit";
		test.intData = 99;
		test.boolData = true;
		final Model inserted = ConfigureDb.da.insert(test);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("explicit", retrieved.textData);
		Assertions.assertEquals(99, retrieved.intData);
		Assertions.assertEquals(true, retrieved.boolData);

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}
}
