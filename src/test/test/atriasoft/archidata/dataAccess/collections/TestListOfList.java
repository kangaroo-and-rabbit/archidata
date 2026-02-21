package test.atriasoft.archidata.dataAccess.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
class TestListOfList {

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;
		public List<List<String>> data;
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
		test.data = new ArrayList<>();
		test.data.add(new ArrayList<>(List.of("a", "b")));
		test.data.add(new ArrayList<>(List.of("c", "d", "e")));

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
		test.data = new ArrayList<>();
		test.data.add(new ArrayList<>(List.of("old")));
		final Model inserted = ConfigureDb.da.insert(test);

		inserted.data = new ArrayList<>();
		inserted.data.add(new ArrayList<>(List.of("new1", "new2")));
		inserted.data.add(new ArrayList<>(List.of("new3")));
		ConfigureDb.da.updateById(inserted, inserted.id);

		final Model retrieved = ConfigureDb.da.getById(Model.class, inserted.id);
		Assertions.assertNotNull(retrieved);
		final List<List<String>> expected = new ArrayList<>();
		expected.add(new ArrayList<>(List.of("new1", "new2")));
		expected.add(new ArrayList<>(List.of("new3")));
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
