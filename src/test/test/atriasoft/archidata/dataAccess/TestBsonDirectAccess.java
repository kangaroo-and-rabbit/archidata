package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.bson.Document;
import org.bson.types.ObjectId;
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

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBsonDirectAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestBsonDirectAccess.class);
	private static final String TEST_COLLECTION = "test_bson_collection";
	private static ObjectId insertedId1 = null;
	private static ObjectId insertedId2 = null;
	private static ObjectId insertedId3 = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		// Clear the static test:
		insertedId1 = null;
		insertedId2 = null;
		insertedId3 = null;
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testInsertBsonDocument_SimpleTypes() throws Exception {
		LOGGER.info("Test insert BSON document with simple types");

		final Document doc = new Document().append("name", "John Doe").append("age", 30)
				.append("email", "john@example.com").append("active", true).append("score", 95.5);

		final ObjectId id = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);

		Assertions.assertNotNull(id);
		insertedId1 = id;

		// Verify insertion
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", id)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("John Doe", retrieved.getString("name"));
		Assertions.assertEquals(30, retrieved.getInteger("age"));
		Assertions.assertEquals("john@example.com", retrieved.getString("email"));
		Assertions.assertEquals(true, retrieved.getBoolean("active"));
		Assertions.assertEquals(95.5, retrieved.getDouble("score"), 0.001);
	}

	@Order(2)
	@Test
	public void testInsertBsonDocument_DateTypes() throws Exception {
		LOGGER.info("Test insert BSON document with Date types");

		final Date now = new Date();
		final Instant instant = Instant.now();
		final OffsetDateTime offsetDateTime = OffsetDateTime.now();
		final LocalDate localDate = LocalDate.now();
		final LocalTime localTime = LocalTime.now();

		final Document doc = new Document().append("name", "Jane Doe").append("age", 28).append("dateField", now)
				.append("instantField", Date.from(instant))
				.append("offsetDateTimeField", Date.from(offsetDateTime.toInstant()))
				.append("localDateString", localDate.toString()).append("localTimeString", localTime.toString());

		final ObjectId id = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);

		Assertions.assertNotNull(id);
		insertedId2 = id;

		// Verify insertion
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", id)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("Jane Doe", retrieved.getString("name"));
		Assertions.assertNotNull(retrieved.getDate("dateField"));
		Assertions.assertNotNull(retrieved.getDate("instantField"));
		Assertions.assertNotNull(retrieved.getDate("offsetDateTimeField"));
		Assertions.assertEquals(localDate.toString(), retrieved.getString("localDateString"));
		Assertions.assertEquals(localTime.toString(), retrieved.getString("localTimeString"));

		// Check date equality with tolerance
		final long timeDiff = Math.abs(now.getTime() - retrieved.getDate("dateField").getTime());
		Assertions.assertTrue(timeDiff < 1000, "Date should be equal within 1s tolerance");
	}

	@Order(3)
	@Test
	public void testInsertBsonDocument_ComplexTypes() throws Exception {
		LOGGER.info("Test insert BSON document with complex types");

		// Create nested document
		final Document address = new Document().append("street", "123 Main St").append("city", "New York")
				.append("zipCode", "10001").append("country", "USA");

		// Create array of documents
		final List<Document> hobbies = new ArrayList<>();
		hobbies.add(new Document().append("name", "Reading").append("level", "Expert"));
		hobbies.add(new Document().append("name", "Coding").append("level", "Advanced"));
		hobbies.add(new Document().append("name", "Gaming").append("level", "Intermediate"));

		// Create array of simple values
		final List<String> tags = List.of("developer", "java", "mongodb", "opensource");
		final List<Integer> scores = List.of(100, 95, 88, 92, 97);

		final Document doc = new Document().append("name", "Bob Smith").append("age", 35).append("address", address)
				.append("hobbies", hobbies).append("tags", tags).append("scores", scores);

		final ObjectId id = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);

		Assertions.assertNotNull(id);
		insertedId3 = id;

		// Verify insertion
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", id)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("Bob Smith", retrieved.getString("name"));
		Assertions.assertEquals(35, retrieved.getInteger("age"));

		// Verify nested document
		final Document retrievedAddress = (Document) retrieved.get("address");
		Assertions.assertNotNull(retrievedAddress);
		Assertions.assertEquals("123 Main St", retrievedAddress.getString("street"));
		Assertions.assertEquals("New York", retrievedAddress.getString("city"));
		Assertions.assertEquals("10001", retrievedAddress.getString("zipCode"));
		Assertions.assertEquals("USA", retrievedAddress.getString("country"));

		// Verify array of documents
		@SuppressWarnings("unchecked")
		final List<Document> retrievedHobbies = (List<Document>) retrieved.get("hobbies");
		Assertions.assertNotNull(retrievedHobbies);
		Assertions.assertEquals(3, retrievedHobbies.size());
		Assertions.assertEquals("Reading", retrievedHobbies.get(0).getString("name"));
		Assertions.assertEquals("Expert", retrievedHobbies.get(0).getString("level"));
		Assertions.assertEquals("Coding", retrievedHobbies.get(1).getString("name"));
		Assertions.assertEquals("Advanced", retrievedHobbies.get(1).getString("level"));

		// Verify array of simple values
		@SuppressWarnings("unchecked")
		final List<String> retrievedTags = (List<String>) retrieved.get("tags");
		Assertions.assertNotNull(retrievedTags);
		Assertions.assertEquals(4, retrievedTags.size());
		Assertions.assertTrue(retrievedTags.contains("developer"));
		Assertions.assertTrue(retrievedTags.contains("mongodb"));

		@SuppressWarnings("unchecked")
		final List<Integer> retrievedScores = (List<Integer>) retrieved.get("scores");
		Assertions.assertNotNull(retrievedScores);
		Assertions.assertEquals(5, retrievedScores.size());
		Assertions.assertEquals(100, retrievedScores.get(0));
		Assertions.assertEquals(97, retrievedScores.get(4));
	}

	@Order(4)
	@Test
	public void testGetBsonDocuments_WithFilters() throws Exception {
		LOGGER.info("Test get multiple BSON documents with filters");

		// Get all documents
		final List<Document> allDocs = DataAccess.getBsonDocuments(TEST_COLLECTION);
		Assertions.assertNotNull(allDocs);
		Assertions.assertEquals(3, allDocs.size());

		// Get documents where age > 30
		final List<Document> filteredDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new Condition(new QueryCondition("age", ">", 30)));
		Assertions.assertNotNull(filteredDocs);
		Assertions.assertEquals(1, filteredDocs.size());
		Assertions.assertEquals("Bob Smith", filteredDocs.get(0).getString("name"));
	}

	@Order(5)
	@Test
	public void testGetBsonDocuments_WithOrderBy() throws Exception {
		LOGGER.info("Test get multiple BSON documents with ordering");

		// Get all documents ordered by age ascending
		final List<Document> ascDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new OrderBy(new OrderItem("age", OrderItem.Order.ASC)));
		Assertions.assertNotNull(ascDocs);
		Assertions.assertEquals(3, ascDocs.size());
		Assertions.assertEquals("Jane Doe", ascDocs.get(0).getString("name"));
		Assertions.assertEquals(28, ascDocs.get(0).getInteger("age"));
		Assertions.assertEquals("Bob Smith", ascDocs.get(2).getString("name"));
		Assertions.assertEquals(35, ascDocs.get(2).getInteger("age"));

		// Get all documents ordered by age descending
		final List<Document> descDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new OrderBy(new OrderItem("age", OrderItem.Order.DESC)));
		Assertions.assertNotNull(descDocs);
		Assertions.assertEquals(3, descDocs.size());
		Assertions.assertEquals("Bob Smith", descDocs.get(0).getString("name"));
		Assertions.assertEquals(35, descDocs.get(0).getInteger("age"));

		// Get all documents ordered by name ascending
		final List<Document> nameAscDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new OrderBy(new OrderItem("name", OrderItem.Order.ASC)));
		Assertions.assertNotNull(nameAscDocs);
		Assertions.assertEquals(3, nameAscDocs.size());
		Assertions.assertEquals("Bob Smith", nameAscDocs.get(0).getString("name"));
		Assertions.assertEquals("Jane Doe", nameAscDocs.get(1).getString("name"));
		Assertions.assertEquals("John Doe", nameAscDocs.get(2).getString("name"));
	}

	@Order(6)
	@Test
	public void testGetBsonDocuments_WithLimit() throws Exception {
		LOGGER.info("Test get multiple BSON documents with limit");

		// Get only 2 documents
		final List<Document> limitedDocs = DataAccess.getBsonDocuments(TEST_COLLECTION, new Limit(2));
		Assertions.assertNotNull(limitedDocs);
		Assertions.assertEquals(2, limitedDocs.size());

		// Get 1 document
		final List<Document> oneDocs = DataAccess.getBsonDocuments(TEST_COLLECTION, new Limit(1));
		Assertions.assertNotNull(oneDocs);
		Assertions.assertEquals(1, oneDocs.size());
	}

	@Order(7)
	@Test
	public void testGetBsonDocuments_Combined() throws Exception {
		LOGGER.info("Test get multiple BSON documents with combined filters, ordering, and limit");

		// Get documents where age >= 30, ordered by age descending, limited to 2
		final List<Document> combinedDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new Condition(new QueryCondition("age", ">=", 30)),
				new OrderBy(new OrderItem("age", OrderItem.Order.DESC)), new Limit(2));
		Assertions.assertNotNull(combinedDocs);
		Assertions.assertEquals(2, combinedDocs.size());
		Assertions.assertEquals("Bob Smith", combinedDocs.get(0).getString("name"));
		Assertions.assertEquals(35, combinedDocs.get(0).getInteger("age"));
		Assertions.assertEquals("John Doe", combinedDocs.get(1).getString("name"));
		Assertions.assertEquals(30, combinedDocs.get(1).getInteger("age"));
	}

	@Order(8)
	@Test
	public void testUpdateBsonDocuments_SetOperator() throws Exception {
		LOGGER.info("Test update BSON documents with $set operator");

		// Update John Doe's age and email
		final Document updateOps = new Document("$set",
				new Document("age", 31).append("email", "john.doe@example.com").append("updatedAt", new Date()));

		final long updateCount = DataAccess.updateBsonDocuments(TEST_COLLECTION, updateOps,
				new Condition(new QueryCondition("name", "=", "John Doe")));

		Assertions.assertEquals(1, updateCount);

		// Verify update
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", insertedId1)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(31, retrieved.getInteger("age"));
		Assertions.assertEquals("john.doe@example.com", retrieved.getString("email"));
		Assertions.assertNotNull(retrieved.getDate("updatedAt"));
	}

	@Order(9)
	@Test
	public void testUpdateBsonDocuments_IncOperator() throws Exception {
		LOGGER.info("Test update BSON documents with $inc operator");

		// Increment Bob Smith's age by 5
		final Document updateOps = new Document("$inc", new Document("age", 5));

		final long updateCount = DataAccess.updateBsonDocuments(TEST_COLLECTION, updateOps,
				new Condition(new QueryCondition("name", "=", "Bob Smith")));

		Assertions.assertEquals(1, updateCount);

		// Verify update
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", insertedId3)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals(40, retrieved.getInteger("age")); // Was 35, now 40
	}

	@Order(10)
	@Test
	public void testUpdateBsonDocuments_PushOperator() throws Exception {
		LOGGER.info("Test update BSON documents with $push operator");

		// Add a new hobby to Bob Smith
		final Document newHobby = new Document().append("name", "Photography").append("level", "Beginner");
		final Document updateOps = new Document("$push", new Document("hobbies", newHobby));

		final long updateCount = DataAccess.updateBsonDocuments(TEST_COLLECTION, updateOps,
				new Condition(new QueryCondition("name", "=", "Bob Smith")));

		Assertions.assertEquals(1, updateCount);

		// Verify update
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", insertedId3)));
		Assertions.assertNotNull(retrieved);

		@SuppressWarnings("unchecked")
		final List<Document> hobbies = (List<Document>) retrieved.get("hobbies");
		Assertions.assertNotNull(hobbies);
		Assertions.assertEquals(4, hobbies.size()); // Was 3, now 4
		Assertions.assertEquals("Photography", hobbies.get(3).getString("name"));
		Assertions.assertEquals("Beginner", hobbies.get(3).getString("level"));
	}

	@Order(11)
	@Test
	public void testUpdateBsonDocuments_UnsetOperator() throws Exception {
		LOGGER.info("Test update BSON documents with $unset operator");

		// Remove the score field from John Doe
		final Document updateOps = new Document("$unset", new Document("score", ""));

		final long updateCount = DataAccess.updateBsonDocuments(TEST_COLLECTION, updateOps,
				new Condition(new QueryCondition("name", "=", "John Doe")));

		Assertions.assertEquals(1, updateCount);

		// Verify update
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", insertedId1)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertFalse(retrieved.containsKey("score"));
	}

	@Order(12)
	@Test
	public void testUpdateBsonDocuments_MultipleDocuments() throws Exception {
		LOGGER.info("Test update multiple BSON documents");

		// Set status to "reviewed" for all documents where age >= 30
		final Document updateOps = new Document("$set", new Document("status", "reviewed"));

		final long updateCount = DataAccess.updateBsonDocuments(TEST_COLLECTION, updateOps,
				new Condition(new QueryCondition("age", ">=", 30)));

		Assertions.assertEquals(2, updateCount); // Should update Jane Doe and Bob Smith

		// Verify updates
		final List<Document> reviewedDocs = DataAccess.getBsonDocuments(TEST_COLLECTION,
				new Condition(new QueryCondition("status", "=", "reviewed")));
		Assertions.assertNotNull(reviewedDocs);
		Assertions.assertEquals(2, reviewedDocs.size());
	}

	@Order(13)
	@Test
	public void testGetBsonDocument_NotFound() throws Exception {
		LOGGER.info("Test get BSON document that doesn't exist");

		final Document notFound = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("name", "=", "Non Existent User")));

		Assertions.assertNull(notFound);
	}

	@Order(14)
	@Test
	public void testInsertBsonDocument_WithSpecifiedId() throws Exception {
		LOGGER.info("Test insert BSON document with specified ObjectId");

		final ObjectId customId = new ObjectId();
		final Document doc = new Document().append("_id", customId).append("name", "Custom ID User").append("age", 25);

		final ObjectId returnedId = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);

		Assertions.assertNotNull(returnedId);
		Assertions.assertEquals(customId, returnedId);

		// Verify insertion
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", customId)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("Custom ID User", retrieved.getString("name"));
		Assertions.assertEquals(25, retrieved.getInteger("age"));
	}

	@Order(15)
	@Test
	public void testBsonDocument_NullValues() throws Exception {
		LOGGER.info("Test BSON document with null values");

		final Document doc = new Document().append("name", "Null Test User").append("age", 28).append("email", null)
				.append("phone", null);

		final ObjectId id = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);
		Assertions.assertNotNull(id);

		// Verify insertion
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", id)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("Null Test User", retrieved.getString("name"));
		Assertions.assertTrue(retrieved.containsKey("email"));
		Assertions.assertNull(retrieved.get("email"));
		Assertions.assertTrue(retrieved.containsKey("phone"));
		Assertions.assertNull(retrieved.get("phone"));
	}

	@Order(16)
	@Test
	public void testBsonDocument_DeepNestedObjects() throws Exception {
		LOGGER.info("Test BSON document with deeply nested objects");

		final Document level3 = new Document().append("value", "level3").append("number", 3);
		final Document level2 = new Document().append("value", "level2").append("number", 2).append("nested", level3);
		final Document level1 = new Document().append("value", "level1").append("number", 1).append("nested", level2);

		final Document doc = new Document().append("name", "Deep Nested User").append("data", level1);

		final ObjectId id = DataAccess.insertBsonDocument(TEST_COLLECTION, doc);
		Assertions.assertNotNull(id);

		// Verify insertion and nested access
		final Document retrieved = DataAccess.getBsonDocument(TEST_COLLECTION,
				new Condition(new QueryCondition("_id", "=", id)));
		Assertions.assertNotNull(retrieved);
		Assertions.assertEquals("Deep Nested User", retrieved.getString("name"));

		final Document retrievedLevel1 = (Document) retrieved.get("data");
		Assertions.assertNotNull(retrievedLevel1);
		Assertions.assertEquals("level1", retrievedLevel1.getString("value"));

		final Document retrievedLevel2 = (Document) retrievedLevel1.get("nested");
		Assertions.assertNotNull(retrievedLevel2);
		Assertions.assertEquals("level2", retrievedLevel2.getString("value"));

		final Document retrievedLevel3 = (Document) retrievedLevel2.get("nested");
		Assertions.assertNotNull(retrievedLevel3);
		Assertions.assertEquals("level3", retrievedLevel3.getString("value"));
		Assertions.assertEquals(3, retrievedLevel3.getInteger("number"));
	}
}
