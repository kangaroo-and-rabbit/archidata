package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.AllowMassOperation;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-17: a write with a missing or empty condition is refused unless AllowMassOperation. */
public class TestMassOperationGuard {
	private static final String COLLECTION = "massGuard";

	/** Simple entity. */
	@Collection(COLLECTION)
	public static class Item {
		@Id
		public ObjectId _id = null;
		public String name;
		public String status;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		for (final String name : List.of("a", "b", "c")) {
			final Item item = new Item();
			item.name = name;
			item.status = "new";
			ConfigureDb.da.insert(item);
		}
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private static Item patch(final String status) {
		final Item out = new Item();
		out.status = status;
		return out;
	}

	@Test
	public void testEmptyConditionsAreRefused() throws Exception {
		final FilterValue onlyStatus = new FilterValue("status");
		Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.update(patch("x"), new Condition(), onlyStatus));
		Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.update(patch("x"), new Condition(Filters.empty()), onlyStatus));
		Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.update(patch("x"), new Condition(), new Condition(), onlyStatus));
		Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.deleteHard(Item.class, new Condition()));
		Assertions.assertThrows(DataAccessException.class, () -> ConfigureDb.da.updateBsonDocuments(COLLECTION,
				new Document("$set", new Document("status", "x"))));
		Assertions.assertThrows(DataAccessException.class, () -> ConfigureDb.da.updateBsonDocuments(COLLECTION,
				new Document("$set", new Document("status", "x")), new Condition()));
		Assertions.assertEquals(3, ConfigureDb.da.count(Item.class, new Condition(Filters.eq("status", "new"))));
	}

	@Test
	public void testNonEmptyConditionStillWorks() throws Exception {
		// An empty condition combined with a real one keeps the real one.
		Assertions.assertEquals(1, ConfigureDb.da.update(patch("one"), new Condition(),
				new Condition(Filters.eq("name", "a")), new FilterValue("status")));
		Assertions.assertEquals(1, ConfigureDb.da.count(Item.class, new Condition(Filters.eq("status", "one"))));
		ConfigureDb.da.update(patch("new"), new Condition(Filters.eq("name", "a")), new FilterValue("status"));
	}

	@Test
	public void testAllowMassOperation() throws Exception {
		Assertions.assertEquals(3, ConfigureDb.da.updateBsonDocuments(COLLECTION,
				new Document("$set", new Document("status", "mass")), new AllowMassOperation()));
		Assertions.assertEquals(3, ConfigureDb.da.update(patch("mass2"), new Condition(), new FilterValue("status"),
				new AllowMassOperation()));
		Assertions.assertEquals(3, ConfigureDb.da.count(Item.class, new Condition(Filters.eq("status", "mass2"))));
		ConfigureDb.da.update(patch("new"), new FilterValue("status"), new AllowMassOperation());
	}
}
