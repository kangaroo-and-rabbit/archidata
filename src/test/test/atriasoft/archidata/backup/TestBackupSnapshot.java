package test.atriasoft.archidata.backup;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.backup.BackupSnapshot;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.backup.model.DataStoreWithUpdate;
import test.atriasoft.archidata.backup.model.DataStoreWithoutUpdate;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBackupSnapshot {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestBackupSnapshot.class);

	@BeforeAll
	public static void setUp() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private void insertTestData() throws Exception {
		DataAccess.drop(DataStoreWithUpdate.class);
		DataAccess.drop(DataStoreWithoutUpdate.class);

		final DataStoreWithUpdate d1 = new DataStoreWithUpdate();
		d1.dataLong = 42L;
		d1.dataDoubles = List.of(3.14, 2.71);
		DataAccess.insert(d1);

		final DataStoreWithUpdate d2 = new DataStoreWithUpdate();
		d2.dataLong = 99L;
		d2.dataDoubles = List.of(1.0);
		DataAccess.insert(d2);

		final DataStoreWithoutUpdate d3 = new DataStoreWithoutUpdate();
		d3.dataString = "snapshot test";
		d3.dataTime = Date.from(LocalDateTime.of(2025, 7, 1, 12, 0, 0).atZone(ZoneOffset.UTC).toInstant());
		DataAccess.insert(d3);
	}

	@Test
	@Order(1)
	public void testSnapshotAndRestore() throws Exception {
		insertTestData();

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);

		// Take a snapshot of the initial state
		final BackupSnapshot snapshot = engine.snapshot();
		Assertions.assertNotNull(snapshot);
		Assertions.assertEquals(2, snapshot.size());
		Assertions.assertFalse(snapshot.isEmpty());

		// Verify snapshot contains the expected collections
		Assertions.assertTrue(snapshot.collections().containsKey("DataStoreWithUpdate"));
		Assertions.assertTrue(snapshot.collections().containsKey("DataStoreWithoutUpdate"));

		// Modify the database: delete all and insert different data
		DataAccess.drop(DataStoreWithUpdate.class);
		DataAccess.drop(DataStoreWithoutUpdate.class);

		final DataStoreWithUpdate modified = new DataStoreWithUpdate();
		modified.dataLong = 999L;
		modified.dataDoubles = List.of(0.0);
		DataAccess.insert(modified);

		// Verify the DB has changed
		final List<DataStoreWithUpdate> beforeRestore = DataAccess.gets(DataStoreWithUpdate.class,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(1, beforeRestore.size());
		Assertions.assertEquals(999L, beforeRestore.get(0).dataLong);

		// Restore from snapshot
		engine.restore(snapshot);

		// Verify the original state is back
		final List<DataStoreWithUpdate> afterRestore = DataAccess.gets(DataStoreWithUpdate.class,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(2, afterRestore.size());
		Assertions.assertEquals(42L, afterRestore.get(0).dataLong);
		Assertions.assertEquals(2, afterRestore.get(0).dataDoubles.size());
		Assertions.assertEquals(3.14, afterRestore.get(0).dataDoubles.get(0), 0.0001);
		Assertions.assertEquals(99L, afterRestore.get(1).dataLong);

		final List<DataStoreWithoutUpdate> afterRestore2 = DataAccess.gets(DataStoreWithoutUpdate.class);
		Assertions.assertEquals(1, afterRestore2.size());
		Assertions.assertEquals("snapshot test", afterRestore2.get(0).dataString);
	}

	@Test
	@Order(2)
	public void testSnapshotAllAndRestore() throws Exception {
		insertTestData();

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_all",
				EngineBackupType.JSON_EXTENDED);
		// No addClass — snapshotAll discovers collections automatically

		final BackupSnapshot snapshot = engine.snapshotAll();
		Assertions.assertNotNull(snapshot);
		// Should contain at least our 2 test collections
		Assertions.assertTrue(snapshot.size() >= 2);
		Assertions.assertTrue(snapshot.collections().containsKey("DataStoreWithUpdate"));
		Assertions.assertTrue(snapshot.collections().containsKey("DataStoreWithoutUpdate"));

		// Modify the database
		DataAccess.drop(DataStoreWithUpdate.class);
		final List<DataStoreWithUpdate> empty = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(0, empty.size());

		// Restore from snapshot
		engine.restore(snapshot);

		// Verify the original state is back
		final List<DataStoreWithUpdate> afterRestore = DataAccess.gets(DataStoreWithUpdate.class,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(2, afterRestore.size());
		Assertions.assertEquals(42L, afterRestore.get(0).dataLong);
	}

	@Test
	@Order(3)
	public void testSnapshotEmptyCollections() throws Exception {
		// Ensure collections are empty
		DataAccess.drop(DataStoreWithUpdate.class);
		DataAccess.drop(DataStoreWithoutUpdate.class);

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_empty",
				EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);

		// Snapshot of empty collections should not fail
		final BackupSnapshot snapshot = engine.snapshot();
		Assertions.assertNotNull(snapshot);
		Assertions.assertEquals(2, snapshot.size());

		// Each collection should have empty (or near-empty) byte content
		for (final byte[] data : snapshot.collections().values()) {
			Assertions.assertNotNull(data);
			// Empty collection produces 0 bytes (no documents serialized)
			Assertions.assertEquals(0, data.length);
		}

		// Restoring an empty snapshot should succeed without error
		engine.restore(snapshot);

		// Collections should still be empty after restore
		final List<DataStoreWithUpdate> result = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(0, result.size());
	}

	@Test
	@Order(4)
	public void testSnapshotWithAddCollection() throws Exception {
		insertTestData();

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_addcol",
				EngineBackupType.JSON_EXTENDED);
		// Use addCollection instead of addClass
		engine.addCollection("DataStoreWithUpdate");

		final BackupSnapshot snapshot = engine.snapshot();
		Assertions.assertNotNull(snapshot);
		// Only one collection registered
		Assertions.assertEquals(1, snapshot.size());
		Assertions.assertTrue(snapshot.collections().containsKey("DataStoreWithUpdate"));
		Assertions.assertFalse(snapshot.collections().containsKey("DataStoreWithoutUpdate"));

		// Modify the database
		DataAccess.drop(DataStoreWithUpdate.class);

		// Restore — only DataStoreWithUpdate should come back
		engine.restore(snapshot);

		final List<DataStoreWithUpdate> afterRestore = DataAccess.gets(DataStoreWithUpdate.class,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(2, afterRestore.size());
		Assertions.assertEquals(42L, afterRestore.get(0).dataLong);
	}

	@Test
	@Order(5)
	public void testSnapshotRestoreDoesNotTouchOtherCollections() throws Exception {
		insertTestData();

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_partial",
				EngineBackupType.JSON_EXTENDED);
		// Only snapshot one collection
		engine.addClass(DataStoreWithUpdate.class);

		final BackupSnapshot snapshot = engine.snapshot();

		// Modify DataStoreWithoutUpdate (not in snapshot)
		DataAccess.drop(DataStoreWithoutUpdate.class);
		final DataStoreWithoutUpdate newData = new DataStoreWithoutUpdate();
		newData.dataString = "modified after snapshot";
		newData.dataTime = new Date();
		DataAccess.insert(newData);

		// Also modify DataStoreWithUpdate
		DataAccess.drop(DataStoreWithUpdate.class);

		// Restore from snapshot — should only restore DataStoreWithUpdate
		engine.restore(snapshot);

		// DataStoreWithUpdate should be restored
		final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(2, restored.size());

		// DataStoreWithoutUpdate should keep the modified data (not touched by restore)
		final List<DataStoreWithoutUpdate> untouched = DataAccess.gets(DataStoreWithoutUpdate.class);
		Assertions.assertEquals(1, untouched.size());
		Assertions.assertEquals("modified after snapshot", untouched.get(0).dataString);
	}

	@Test
	@Order(6)
	public void testSnapshotRestoreLargeBatch() throws Exception {
		// Test the batch insert path (>1000 documents triggers insertMany in batches)
		DataAccess.drop(DataStoreWithUpdate.class);
		DataAccess.drop(DataStoreWithoutUpdate.class);

		// Insert 1050 documents to exceed the 1000 batch threshold
		for (int i = 0; i < 1050; i++) {
			final DataStoreWithUpdate d = new DataStoreWithUpdate();
			d.dataLong = Long.valueOf(i);
			d.dataDoubles = List.of((double) i);
			DataAccess.insert(d);
		}

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_large",
				EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);

		final BackupSnapshot snapshot = engine.snapshot();
		Assertions.assertNotNull(snapshot);
		// Serialized data should be non-empty
		Assertions.assertTrue(snapshot.collections().get("DataStoreWithUpdate").length > 0);

		// Drop and restore
		DataAccess.drop(DataStoreWithUpdate.class);
		engine.restore(snapshot);

		final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(1050, restored.size());
	}

	@Test
	@Order(7)
	public void testSnapshotIsEmptyReturnsTrue() throws Exception {
		// A snapshot with no collections should return isEmpty() == true
		final BackupSnapshot emptySnapshot = new BackupSnapshot(Map.of());
		Assertions.assertTrue(emptySnapshot.isEmpty());
		Assertions.assertEquals(0, emptySnapshot.size());
	}

	@Test
	@Order(8)
	public void testMultipleSnapshotsAreIndependent() throws Exception {
		insertTestData();

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_snapshot_multi",
				EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);

		// Take first snapshot
		final BackupSnapshot snapshot1 = engine.snapshot();

		// Modify data
		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate modified = new DataStoreWithUpdate();
		modified.dataLong = 777L;
		modified.dataDoubles = List.of(7.77);
		DataAccess.insert(modified);

		// Take second snapshot
		final BackupSnapshot snapshot2 = engine.snapshot();

		// Verify snapshots are different
		Assertions.assertNotEquals(snapshot1.collections().get("DataStoreWithUpdate").length,
				snapshot2.collections().get("DataStoreWithUpdate").length);

		// Restore snapshot1 — should get original data
		engine.restore(snapshot1);
		List<DataStoreWithUpdate> data = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(2, data.size());
		Assertions.assertEquals(42L, data.get(0).dataLong);

		// Restore snapshot2 — should get modified data
		engine.restore(snapshot2);
		data = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(1, data.size());
		Assertions.assertEquals(777L, data.get(0).dataLong);
	}
}
