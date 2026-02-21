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

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestMapOfMap {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public Map<String, Map<String, Integer>> data;
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
		final Map<String, Integer> inner1 = new HashMap<>();
		inner1.put("x", 1);
		inner1.put("y", 2);
		final Map<String, Integer> inner2 = new HashMap<>();
		inner2.put("z", 3);
		test.data.put("first", inner1);
		test.data.put("second", inner2);

		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.data);
		Assertions.assertEquals(2, inserted.data.size());

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
		final Map<String, Integer> inner = new HashMap<>();
		inner.put("a", 10);
		test.data.put("old", inner);
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = new HashMap<>();
		final Map<String, Integer> newInner = new HashMap<>();
		newInner.put("b", 20);
		newInner.put("c", 30);
		inserted.data.put("new", newInner);
		ConfigureDb.da.updateById(inserted, inserted.id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		final Map<String, Map<String, Integer>> expected = new HashMap<>();
		final Map<String, Integer> expectedInner = new HashMap<>();
		expectedInner.put("b", 20);
		expectedInner.put("c", 30);
		expected.put("new", expectedInner);
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
