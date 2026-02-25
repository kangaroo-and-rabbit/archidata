package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.DataWithBsonDocument;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBsonDocumentField {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestBsonDocumentField.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testInsertAndRetrieveSimpleDocument() throws Exception {
		final DataWithBsonDocument test = new DataWithBsonDocument();
		test.documentData = new Document().append("name", "test-value").append("count", 42).append("active", true);

		final DataWithBsonDocument insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.getOid());

		final DataWithBsonDocument retrieve = ConfigureDb.da.getById(DataWithBsonDocument.class, insertedData.getOid());
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.documentData);
		Assertions.assertEquals("test-value", retrieve.documentData.getString("name"));
		Assertions.assertEquals(42, retrieve.documentData.getInteger("count"));
		Assertions.assertEquals(true, retrieve.documentData.getBoolean("active"));
	}

	@Order(2)
	@Test
	public void testInsertAndRetrieveNestedDocument() throws Exception {
		final DataWithBsonDocument test = new DataWithBsonDocument();
		test.documentData = new Document().append("level1", "value1").append("nested", new Document()
				.append("level2", "value2").append("deepNested", new Document().append("level3", "value3")));

		final DataWithBsonDocument insertedData = ConfigureDb.da.insert(test);
		final DataWithBsonDocument retrieve = ConfigureDb.da.getById(DataWithBsonDocument.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve.documentData);
		Assertions.assertEquals("value1", retrieve.documentData.getString("level1"));

		final Document nested = (Document) retrieve.documentData.get("nested");
		Assertions.assertNotNull(nested);
		Assertions.assertEquals("value2", nested.getString("level2"));

		final Document deepNested = (Document) nested.get("deepNested");
		Assertions.assertNotNull(deepNested);
		Assertions.assertEquals("value3", deepNested.getString("level3"));
	}

	@Order(3)
	@Test
	public void testInsertAndRetrieveDocumentWithList() throws Exception {
		final DataWithBsonDocument test = new DataWithBsonDocument();
		test.documentData = new Document().append("tags", List.of("alpha", "beta", "gamma")).append("numbers",
				List.of(1, 2, 3));

		final DataWithBsonDocument insertedData = ConfigureDb.da.insert(test);
		final DataWithBsonDocument retrieve = ConfigureDb.da.getById(DataWithBsonDocument.class, insertedData.getOid());

		Assertions.assertNotNull(retrieve.documentData);
		@SuppressWarnings("unchecked")
		final List<String> tags = (List<String>) retrieve.documentData.get("tags");
		Assertions.assertEquals(3, tags.size());
		Assertions.assertEquals("alpha", tags.get(0));
		Assertions.assertEquals("beta", tags.get(1));
		Assertions.assertEquals("gamma", tags.get(2));
	}

	@Order(4)
	@Test
	public void testInsertAndRetrieveNullDocument() throws Exception {
		final DataWithBsonDocument test = new DataWithBsonDocument();
		test.documentData = null;

		final DataWithBsonDocument insertedData = ConfigureDb.da.insert(test);
		final DataWithBsonDocument retrieve = ConfigureDb.da.getById(DataWithBsonDocument.class, insertedData.getOid());

		Assertions.assertNull(retrieve.documentData);
	}
}
