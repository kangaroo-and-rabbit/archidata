package test.atriasoft.archidata.dataAccess.options;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
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
class TestOverrideTableName {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
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
	void testInsertAndRetrieveWithOverride() throws Exception {
		final OverrideTableName override = new OverrideTableName("runtime_custom_table");
		final Model test = new Model();
		test.data = "in_runtime_table";
		final Model inserted = ConfigureDb.da.insert(test, override);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id, override);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("in_runtime_table", retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted.id, override);
	}
}
