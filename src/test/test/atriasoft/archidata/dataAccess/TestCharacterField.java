package test.atriasoft.archidata.dataAccess;

import java.io.IOException;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Id;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** A {@code Character} or {@code char} field is written and read back. */
public class TestCharacterField {
	private static final String COLLECTION = "characterField";

	/** Entity with a boxed and a primitive character. */
	@Collection(COLLECTION)
	public static class WithCharacters {
		@Id
		public ObjectId _id = null;
		public Character grade;
		public char initial;
	}

	@BeforeAll
	public static void configure() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void clear() throws IOException {
		ConfigureDb.clear();
	}

	private static Document stored(final ObjectId id) {
		return ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION).find(new Document("_id", id))
				.first();
	}

	@Test
	public void testCharacterRoundTrip() throws Exception {
		final WithCharacters value = new WithCharacters();
		value.grade = 'A';
		value.initial = 'z';
		final ObjectId id = ConfigureDb.da.insert(value)._id;
		final WithCharacters read = ConfigureDb.da.getById(WithCharacters.class, id);
		Assertions.assertEquals(Character.valueOf('A'), read.grade);
		Assertions.assertEquals('z', read.initial);
	}

	@Test
	public void testNullCharacterStaysNull() throws Exception {
		final ObjectId id = ConfigureDb.da.insert(new WithCharacters())._id;
		final WithCharacters read = ConfigureDb.da.getById(WithCharacters.class, id);
		Assertions.assertNull(read.grade);
	}

	@Test
	public void testCharacterStoredAsAnIntegerIsStillRead() throws Exception {
		final ObjectId id = new ObjectId();
		ConfigureDb.da.getInterface().getDatabase().getCollection(COLLECTION)
				.insertOne(new Document("_id", id).append("grade", (int) 'B').append("initial", (int) 'y'));
		Assertions.assertNotNull(stored(id));
		final WithCharacters read = ConfigureDb.da.getById(WithCharacters.class, id);
		Assertions.assertEquals(Character.valueOf('B'), read.grade);
		Assertions.assertEquals('y', read.initial);
	}
}
