package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.AllowMassOperation;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.dataAccess.model.SimpleTable;
import test.atriasoft.archidata.dataAccess.model.SimpleTableSoftDelete;
import test.atriasoft.archidata.dataAccess.model.SoftDeleteNoDefault;

/**
 * Characterization of the {@code DBAccessMongo} operations that no other test calls: {@code existsById},
 * {@code insertMultiple}, {@code getRaw}, {@code listCollections}, {@code renameCollection}, {@code deleteSoft} with a
 * condition, and the static {@code DataAccess.inTransaction}.
 */
public class TestDbAccessOperations {
	private static final String RENAME_SOURCE = "renameSource";
	private static final String RENAME_TARGET = "renameTarget";

	/** Entity of a collection that gets renamed. */
	@Collection(RENAME_SOURCE)
	public static class Renamable {
		@Id
		public ObjectId _id = null;
		public String name;
	}

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDatabase() throws IOException {
		ConfigureDb.clear();
	}

	private static SimpleTable simple(final String data) {
		final SimpleTable out = new SimpleTable();
		out.data = data;
		return out;
	}

	private static SimpleTableSoftDelete soft(final String data) {
		final SimpleTableSoftDelete out = new SimpleTableSoftDelete();
		out.data = data;
		return out;
	}

	private static Condition dataIs(final String data) {
		return new Condition(Filters.eq("data", data));
	}

	// ---------------------------------------------------------------- existsById

	@Test
	public void testExistsByIdWithALongKey() throws Exception {
		final SimpleTable inserted = ConfigureDb.da.insert(simple("exists-long"));
		Assertions.assertTrue(ConfigureDb.da.existsById(SimpleTable.class, inserted.getId()));
		Assertions.assertFalse(ConfigureDb.da.existsById(SimpleTable.class, inserted.getId() + 100000L));
		Assertions.assertTrue(DataAccess.existsById(SimpleTable.class, inserted.getId()), "static facade");
		ConfigureDb.da.deleteHardById(SimpleTable.class, inserted.getId());
		Assertions.assertFalse(ConfigureDb.da.existsById(SimpleTable.class, inserted.getId()));
	}

	@Test
	public void testExistsByIdWithAnObjectIdKey() throws Exception {
		final SoftDeleteNoDefault data = new SoftDeleteNoDefault();
		data.data = "exists-oid";
		final SoftDeleteNoDefault inserted = ConfigureDb.da.insert(data);
		Assertions.assertTrue(ConfigureDb.da.existsById(SoftDeleteNoDefault.class, inserted.getOid()));
		Assertions.assertFalse(ConfigureDb.da.existsById(SoftDeleteNoDefault.class, new ObjectId()));
	}

	@Test
	public void testExistsByIdHidesTheSoftDeletedUnlessAsked() throws Exception {
		final SimpleTableSoftDelete inserted = ConfigureDb.da.insert(soft("exists-soft"));
		Assertions.assertTrue(ConfigureDb.da.existsById(SimpleTableSoftDelete.class, inserted.getId()));
		Assertions.assertEquals(1, ConfigureDb.da.deleteSoftById(SimpleTableSoftDelete.class, inserted.getId()));
		Assertions.assertFalse(ConfigureDb.da.existsById(SimpleTableSoftDelete.class, inserted.getId()));
		Assertions.assertTrue(
				ConfigureDb.da.existsById(SimpleTableSoftDelete.class, inserted.getId(), new AccessDeletedItems()));
	}

	@Test
	public void testExistsByIdOfNullIsRefused() {
		final DataAccessException exception = Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.existsById(SimpleTable.class, null));
		Assertions.assertEquals("Try to identify the ID type and object was null.", exception.getMessage());
	}

	// ---------------------------------------------------------------- insertMultiple

	@Test
	public void testInsertMultipleKeepsTheOrderAndGeneratesTheKeys() throws Exception {
		final List<SimpleTable> inserted = ConfigureDb.da
				.insertMultiple(List.of(simple("multi-a"), simple("multi-b"), simple("multi-c")));
		Assertions.assertEquals(3, inserted.size());
		final List<String> data = new ArrayList<>();
		final Set<Long> ids = new TreeSet<>();
		for (final SimpleTable element : inserted) {
			Assertions.assertNotNull(element.getId());
			data.add(element.data);
			ids.add(element.getId());
			Assertions.assertTrue(ConfigureDb.da.existsById(SimpleTable.class, element.getId()));
		}
		Assertions.assertEquals(List.of("multi-a", "multi-b", "multi-c"), data);
		Assertions.assertEquals(3, ids.size(), "the keys are distinct");
		Assertions.assertEquals(3,
				ConfigureDb.da.count(SimpleTable.class, new Condition(Filters.regex("data", "^multi-"))));
	}

	@Test
	public void testInsertMultipleOfNothingInsertsNothing() throws Exception {
		final long before = ConfigureDb.da.count(SimpleTable.class);
		Assertions.assertEquals(List.of(), ConfigureDb.da.insertMultiple(List.of()));
		Assertions.assertEquals(before, ConfigureDb.da.count(SimpleTable.class));
	}

	@Test
	public void testInsertMultipleThroughTheStaticFacade() throws Exception {
		final List<SimpleTable> inserted = DataAccess.insertMultiple(List.of(simple("facade-a"), simple("facade-b")));
		Assertions.assertEquals(2, inserted.size());
		Assertions.assertEquals("facade-a", inserted.get(0).data);
		Assertions.assertEquals("facade-b", inserted.get(1).data);
		Assertions.assertNotEquals(inserted.get(0).getId(), inserted.get(1).getId());
	}

	// ---------------------------------------------------------------- getRaw

	@Test
	public void testGetRawReturnsTheEntityOrNull() throws Exception {
		final SimpleTable inserted = ConfigureDb.da.insert(simple("raw-one"));
		final Object raw = ConfigureDb.da.getRaw(SimpleTable.class, dataIs("raw-one"));
		Assertions.assertInstanceOf(SimpleTable.class, raw);
		Assertions.assertEquals(inserted.getId(), ((SimpleTable) raw).getId());
		Assertions.assertEquals("raw-one", ((SimpleTable) raw).data);
		Assertions.assertNull(ConfigureDb.da.getRaw(SimpleTable.class, dataIs("raw-nothing")));
	}

	@Test
	public void testGetRawReturnsTheFirstOfSeveralInTheRequestedOrder() throws Exception {
		ConfigureDb.da.insertMultiple(List.of(simple("raw-many-b"), simple("raw-many-a"), simple("raw-many-c")));
		final Condition many = new Condition(Filters.regex("data", "^raw-many-"));
		final SimpleTable first = (SimpleTable) ConfigureDb.da.getRaw(SimpleTable.class, many,
				new OrderBy(new OrderItem("data", OrderItem.Order.ASC)));
		Assertions.assertEquals("raw-many-a", first.data);
		final SimpleTable last = (SimpleTable) ConfigureDb.da.getRaw(SimpleTable.class, many,
				new OrderBy(new OrderItem("data", OrderItem.Order.DESC)));
		Assertions.assertEquals("raw-many-c", last.data);
	}

	@Test
	public void testGetRawHidesTheSoftDeletedUnlessAsked() throws Exception {
		final SimpleTableSoftDelete inserted = ConfigureDb.da.insert(soft("raw-soft"));
		ConfigureDb.da.deleteSoftById(SimpleTableSoftDelete.class, inserted.getId());
		Assertions.assertNull(ConfigureDb.da.getRaw(SimpleTableSoftDelete.class, dataIs("raw-soft")));
		Assertions.assertNotNull(
				ConfigureDb.da.getRaw(SimpleTableSoftDelete.class, dataIs("raw-soft"), new AccessDeletedItems()));
	}

	@Test
	public void testGetAndGetRawLeaveTheOptionsOfTheCallerUntouched() throws Exception {
		ConfigureDb.da.insertMultiple(List.of(simple("reuse-a"), simple("reuse-b"), simple("reuse-c")));
		final QueryOptions options = new QueryOptions(new Condition(Filters.regex("data", "^reuse-")));

		Assertions.assertNotNull(ConfigureDb.da.get(SimpleTable.class, options));
		Assertions.assertNotNull(ConfigureDb.da.getRaw(SimpleTable.class, options));
		// the same object, used again: no Limit(1) was left in it
		Assertions.assertEquals(0, options.get(Limit.class).size());
		Assertions.assertEquals(3, ConfigureDb.da.gets(SimpleTable.class, options).size());
		Assertions.assertNotNull(ConfigureDb.da.get(SimpleTable.class, options));
	}

	// ---------------------------------------------------------------- listCollections

	@Test
	public void testListCollectionsNamesTheCollectionsOfTheDatabase() throws Exception {
		ConfigureDb.da.insert(simple("list-collections"));
		final List<String> names = ConfigureDb.da.listCollections("any");
		Assertions.assertTrue(names.contains("SimpleTable"), names.toString());
		Assertions.assertEquals(names.size(), new TreeSet<>(names).size(), "no duplicate");
		// the name and the options are ignored
		Assertions.assertEquals(new TreeSet<>(names), new TreeSet<>(ConfigureDb.da.listCollections("another name")));
		Assertions.assertEquals(new TreeSet<>(names), new TreeSet<>(ConfigureDb.da.listCollections("")));
		Assertions.assertEquals(new TreeSet<>(names), new TreeSet<>(DataAccess.listCollections("any")),
				"static facade");
	}

	// ---------------------------------------------------------------- renameCollection

	@Test
	public void testRenameCollectionMovesTheDocuments() throws Exception {
		final Renamable first = new Renamable();
		first.name = "renamed-a";
		ConfigureDb.da.insert(first);
		final Renamable second = new Renamable();
		second.name = "renamed-b";
		ConfigureDb.da.insert(second);
		Assertions.assertTrue(ConfigureDb.da.listCollections("").contains(RENAME_SOURCE));
		ConfigureDb.da.renameCollection(RENAME_SOURCE, RENAME_TARGET);
		final List<String> names = ConfigureDb.da.listCollections("");
		Assertions.assertFalse(names.contains(RENAME_SOURCE), names.toString());
		Assertions.assertTrue(names.contains(RENAME_TARGET), names.toString());
		Assertions.assertEquals(2,
				ConfigureDb.da.getInterface().getDatabase().getCollection(RENAME_TARGET).countDocuments());
		// the entity looks in its own collection: it is empty now
		Assertions.assertEquals(0, ConfigureDb.da.count(Renamable.class));
		// and back
		ConfigureDb.da.renameCollection(RENAME_TARGET, RENAME_SOURCE);
		Assertions.assertEquals(2, ConfigureDb.da.count(Renamable.class));
	}

	@Test
	public void testRenameCollectionRefusesAMissingSourceAndAnExistingTarget() throws Exception {
		Assertions.assertThrows(MongoException.class,
				() -> ConfigureDb.da.renameCollection("collection-nowhere", "other"));
		ConfigureDb.da.insert(simple("rename-clash"));
		final DBAccessMongo da = ConfigureDb.da;
		da.getInterface().getDatabase().getCollection("rename-clash-target").insertOne(new org.bson.Document("a", 1));
		Assertions.assertThrows(MongoException.class, () -> da.renameCollection("rename-clash-target", "SimpleTable"));
		Assertions.assertEquals(1,
				da.getInterface().getDatabase().getCollection("rename-clash-target").countDocuments());
	}

	// ---------------------------------------------------------------- deleteSoft

	@Test
	public void testDeleteSoftOnlyTouchesTheMatchingEntities() throws Exception {
		ConfigureDb.da
				.insertMultiple(List.of(soft("soft-keep"), soft("soft-gone"), soft("soft-gone"), soft("soft-keep")));
		Assertions.assertEquals(2, ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class, dataIs("soft-gone")));
		Assertions.assertEquals(0, ConfigureDb.da.count(SimpleTableSoftDelete.class, dataIs("soft-gone")));
		Assertions.assertEquals(2, ConfigureDb.da.count(SimpleTableSoftDelete.class, dataIs("soft-keep")));
		// the documents are still there for who asks
		Assertions.assertEquals(2,
				ConfigureDb.da.count(SimpleTableSoftDelete.class, dataIs("soft-gone"), new AccessDeletedItems()));
		// a second pass finds nothing left to delete
		Assertions.assertEquals(0, ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class, dataIs("soft-gone")));
		Assertions.assertEquals(0,
				ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class, dataIs("soft-never-inserted")));
	}

	@Test
	public void testDeleteSoftRefreshesTheUpdateTimestamp() throws Exception {
		final SimpleTableSoftDelete inserted = ConfigureDb.da.insert(soft("soft-timestamp"), new ReadAllColumn());
		final Date before = inserted.getUpdatedAt();
		Assertions.assertNotNull(before);
		Assertions.assertEquals(1, ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class, dataIs("soft-timestamp")));
		final SimpleTableSoftDelete deleted = ConfigureDb.da.getById(SimpleTableSoftDelete.class, inserted.getId(),
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertTrue(deleted.getDeleted());
		Assertions.assertFalse(deleted.getUpdatedAt().before(before));
		Assertions.assertEquals(inserted.getCreatedAt(), deleted.getCreatedAt());
	}

	@Test
	public void testDeleteSoftWithoutConditionIsRefusedUnlessMassOperation() throws Exception {
		ConfigureDb.da.insertMultiple(List.of(soft("mass-a"), soft("mass-b")));
		final DataAccessException exception = Assertions.assertThrows(DataAccessException.class,
				() -> ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class));
		Assertions.assertEquals(
				"request a write without any condition (or with an empty one): add a filter, or the option AllowMassOperation to target the whole collection",
				exception.getMessage());
		Assertions.assertEquals(2,
				ConfigureDb.da.count(SimpleTableSoftDelete.class, new Condition(Filters.regex("data", "^mass-"))));
		final long all = ConfigureDb.da.count(SimpleTableSoftDelete.class);
		Assertions.assertEquals(all, ConfigureDb.da.deleteSoft(SimpleTableSoftDelete.class, new AllowMassOperation()));
		Assertions.assertEquals(0, ConfigureDb.da.count(SimpleTableSoftDelete.class));
	}

	@Test
	public void testDeleteSoftThroughTheStaticFacade() throws Exception {
		DataAccess.insert(soft("soft-facade"));
		Assertions.assertEquals(1, DataAccess.deleteSoft(SimpleTableSoftDelete.class, dataIs("soft-facade")));
		Assertions.assertEquals(0, DataAccess.count(SimpleTableSoftDelete.class, dataIs("soft-facade")));
	}

	// ---------------------------------------------------------------- inTransaction

	@Test
	public void testInTransactionCommitsWhatTheActionWrote() throws Exception {
		final boolean[] activeInside = new boolean[1];
		DataAccess.inTransaction(db -> {
			activeInside[0] = db.isTransactionActive();
			final SimpleTable parent = db.insert(simple("tx-commit-parent"));
			db.insert(simple("tx-commit-child-of-" + parent.getId()));
		});
		Assertions.assertTrue(activeInside[0]);
		Assertions.assertEquals(1, ConfigureDb.da.count(SimpleTable.class, dataIs("tx-commit-parent")));
		Assertions.assertEquals(1,
				ConfigureDb.da.count(SimpleTable.class, new Condition(Filters.regex("data", "^tx-commit-child-of-"))));
	}

	@Test
	public void testInTransactionRollsBackAndRethrowsWhenTheActionFails() throws Exception {
		final IllegalStateException failure = new IllegalStateException("the action failed");
		final IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class,
				() -> DataAccess.inTransaction(db -> {
					db.insert(simple("tx-rollback"));
					throw failure;
				}));
		Assertions.assertSame(failure, thrown);
		Assertions.assertEquals(0, ConfigureDb.da.count(SimpleTable.class, dataIs("tx-rollback")));
	}

	@Test
	public void testInTransactionRollsBackASoftDeleteToo() throws Exception {
		ConfigureDb.da.insert(soft("tx-soft"));
		Assertions.assertThrows(IllegalStateException.class, () -> DataAccess.inTransaction(db -> {
			Assertions.assertEquals(1, db.deleteSoft(SimpleTableSoftDelete.class, dataIs("tx-soft")));
			throw new IllegalStateException("undo");
		}));
		Assertions.assertEquals(1, ConfigureDb.da.count(SimpleTableSoftDelete.class, dataIs("tx-soft")));
	}
}
