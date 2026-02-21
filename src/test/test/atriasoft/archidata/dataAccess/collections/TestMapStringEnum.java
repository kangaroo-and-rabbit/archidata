package test.atriasoft.archidata.dataAccess.collections;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import test.atriasoft.archidata.dataAccess.model.Enum2ForTest;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestMapStringEnum {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public Map<String, Enum2ForTest> data;
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
		test.data = new HashMap<>();
		test.data.put("x", Enum2ForTest.ENUM_VALUE_1);
		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.data);
		Assertions.assertEquals(1, inserted.data.size());

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertNotNull(retrieved.data);
		Assertions.assertEquals(test.data, retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.data = new HashMap<>();
		test.data.put("a", Enum2ForTest.ENUM_VALUE_2);
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = new HashMap<>();
		inserted.data.put("b", Enum2ForTest.ENUM_VALUE_3);
		inserted.data.put("c", Enum2ForTest.ENUM_VALUE_4);
		ConfigureDb.da.updateById(inserted, inserted.id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		final Map<String, Enum2ForTest> expected = new HashMap<>();
		expected.put("b", Enum2ForTest.ENUM_VALUE_3);
		expected.put("c", Enum2ForTest.ENUM_VALUE_4);
		Assertions.assertEquals(expected, retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}

	@Order(3)
	@Test
	void testNullValue() throws Exception {
		final Model test = new Model();
		test.data = null;
		final Model inserted = ConfigureDb.da.insert(test);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertNull(retrieved.data);

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}
}
