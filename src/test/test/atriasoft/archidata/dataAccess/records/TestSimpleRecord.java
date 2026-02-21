package test.atriasoft.archidata.dataAccess.records;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;

@Disabled("Records as sub-objects not yet supported by the codec/ClassModel")
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestSimpleRecord {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestSimpleRecord.class);

	public record SimpleRec(String name, int value) {}

	public static class Model {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(nullable = false, unique = true)
		public Long id = null;

		public SimpleRec data;
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
	void testInsertAndRetrieve() throws Exception {
		final Model test = new Model();
		test.data = new SimpleRec("hello", 42);

		final Model insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);

		final Model retrieve = ConfigureDb.da.getById(Model.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals("hello", retrieve.data.name());
		Assertions.assertEquals(42, retrieve.data.value());

		idOfTheObject = retrieve.id;
	}

	@Order(2)
	@Test
	void testUpdate() throws Exception {
		final Model updateData = new Model();
		updateData.data = new SimpleRec("updated", 99);

		ConfigureDb.da.updateById(updateData, idOfTheObject);

		final Model retrieve = ConfigureDb.da.getById(Model.class, idOfTheObject);

		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(idOfTheObject, retrieve.id);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals("updated", retrieve.data.name());
		Assertions.assertEquals(99, retrieve.data.value());
	}
}
