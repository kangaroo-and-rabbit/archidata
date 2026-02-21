package test.atriasoft.archidata.dataAccess.options;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;
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
class TestOptionRenameColumn {

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
	void testInsertAndRetrieveWithRename() throws Exception {
		final Model test = new Model();
		test.data = "renamed_column_test";
		final OptionRenameColumn rename = new OptionRenameColumn("data", "renamed_data_col");
		final Model inserted = ConfigureDb.da.insert(test, rename);
		Assertions.assertNotNull(inserted);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id, rename);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("renamed_column_test", retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted.id, rename);
	}
}
