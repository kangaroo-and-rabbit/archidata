package test.atriasoft.archidata.dataAccess.collections;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
class TestMapOfSet {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public Map<String, Set<String>> data;
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
		test.data.put("tags", new HashSet<>(Set.of("alpha", "beta")));
		test.data.put("labels", new HashSet<>(Set.of("one")));

		final Model inserted = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(inserted);
		Assertions.assertNotNull(inserted.data);
		Assertions.assertEquals(2, inserted.data.size());

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertNotNull(retrieved.data);
		Assertions.assertEquals(2, retrieved.data.size());
		Assertions.assertEquals(2, retrieved.data.get("tags").size());
		Assertions.assertTrue(retrieved.data.get("tags").containsAll(Set.of("alpha", "beta")));
		Assertions.assertEquals(1, retrieved.data.get("labels").size());
		Assertions.assertTrue(retrieved.data.get("labels").containsAll(Set.of("one")));

		ConfigureDb.da.deleteById(Model.class, inserted.id);
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model test = new Model();
		test.data = new HashMap<>();
		test.data.put("old", new HashSet<>(Set.of("x")));
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = new HashMap<>();
		inserted.data.put("new", new HashSet<>(Set.of("y", "z")));
		ConfigureDb.da.updateById(inserted, inserted.id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(1, retrieved.data.size());
		Assertions.assertEquals(2, retrieved.data.get("new").size());
		Assertions.assertTrue(retrieved.data.get("new").containsAll(Set.of("y", "z")));

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
