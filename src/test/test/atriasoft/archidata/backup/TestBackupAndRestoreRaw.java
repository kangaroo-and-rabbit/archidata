package test.atriasoft.archidata.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.backup.model.DataStoreWithUpdate;
import test.atriasoft.archidata.backup.model.DataStoreWithoutUpdate;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBackupAndRestoreRaw {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestBackupAndRestoreRaw.class);

	@BeforeAll
	public static void setUp() {
		// Set default timezone to UTC
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Extracts all files from a .tar.gz archive into a Map where keys are file
	 * names and values are their textual content.
	 *
	 * @param inputPath Path to the .tar.gz archive
	 * @return Map of filename to file content as String
	 * @throws IOException if reading the archive fails
	 */
	public Map<String, String> extractTarGzToMap(final Path inputPath) throws IOException {
		final Map<String, String> result = new HashMap<>();

		try (InputStream fileIn = Files.newInputStream(inputPath);
				BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
				GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bufferedIn);
				TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

			TarArchiveEntry entry;
			while ((entry = tarIn.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					continue;
				}
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				tarIn.transferTo(out);
				final String content = out.toString(StandardCharsets.UTF_8);
				result.put(entry.getName(), content);
			}
		}
		return result;
	}

	/**
	 * Write a Map<String, String> into a .tar.gz archive. Each entry becomes a file
	 * with its content.
	 *
	 * @param data   Map with file name as key and content as value
	 * @param output Path to the .tar.gz file to create
	 * @throws IOException if writing fails
	 */
	public void writeMapToTarGz(final Map<String, String> data, final Path output) throws IOException {
		try (OutputStream fileOut = Files.newOutputStream(output);
				BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
				GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(bufferedOut);
				TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {

			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

			for (final Map.Entry<String, String> entry : data.entrySet()) {
				final byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
				final TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
				tarEntry.setSize(content.length);
				tarOut.putArchiveEntry(tarEntry);
				tarOut.write(content);
				tarOut.closeArchiveEntry();
			}
		}
	}

	String revoveDateAndObjectId(final String data, final boolean ignoreDate) {
		String tmp = data //
				.replaceAll(":\\s*\"[a-f0-9]{24}\"", ":\"IGNORE\""); // objectID
		if (ignoreDate) {
			return tmp;
		}
		tmp = tmp.replaceAll(":\\s*\"[0-9]{13}\"", ":\"IGNORE\"");
		return tmp.replaceAll("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,6}Z", "IGNORE"); // timestamp ISO 8601
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	private void insertData() throws Exception {
		// Clean the collection:
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);
		// Insert the data in the collection:
		final DataStoreWithUpdate dataInsert = new DataStoreWithUpdate();
		dataInsert.dataLong = 9953L;
		dataInsert.dataDoubles = List.of(1.25, -1.66);
		final DataStoreWithUpdate data11 = DataAccess.insert(dataInsert);
		dataInsert.dataLong = null;
		dataInsert.dataDoubles = List.of(152.8, 5213546345.0);
		final DataStoreWithUpdate data12 = DataAccess.insert(dataInsert);
		final DataStoreWithoutUpdate dataInsert2 = new DataStoreWithoutUpdate();
		dataInsert2.dataString = "my test String";
		dataInsert2.dataTime = Date
				.from(LocalDateTime.of(2523, 12, 22, 15, 32, 0, 254_000_000).atZone(ZoneOffset.UTC).toInstant());
		final DataStoreWithoutUpdate data21 = DataAccess.insert(dataInsert2);
		dataInsert2.dataString = "my second test string";
		dataInsert2.dataTime = Date
				.from(LocalDateTime.of(2523, 05, 1, 05, 59, 24, 241_000_000).atZone(ZoneOffset.UTC).toInstant());
		final DataStoreWithoutUpdate data22 = DataAccess.insert(dataInsert2);
	}

	@Test
	public void testBackupCollectionRawData_JSON_EXTERNAL() throws Exception {
		insertData();
		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base",
				EngineBackupType.JSON_EXTERNAL);
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);
		engine.store(null);
		final Map<String, String> dataExtract = extractTarGzToMap(
				Paths.get("./").resolve("test_store_data_base.tar.gz"));
		Assertions.assertNotNull(dataExtract);
		Assertions.assertEquals(2, dataExtract.size());
		Assertions.assertEquals("""
				{"_id":"IGNORE","dataLong":9953,"dataDoubles":[1.25,-1.66],"createdAt":"IGNORE","updatedAt":"IGNORE"}
				{"_id":"IGNORE","dataDoubles":[152.8,5.213546345E9],"createdAt":"IGNORE","updatedAt":"IGNORE"}
				""", revoveDateAndObjectId(dataExtract.get("DataStoreWithUpdate.json"), false));
		Assertions.assertEquals("""
				{"_id":"IGNORE","dataString":"my test String","dataTime":"2523-12-22T15:32:00.254Z"}
				{"_id":"IGNORE","dataString":"my second test string","dataTime":"2523-05-01T05:59:24.241Z"}
				""", revoveDateAndObjectId(dataExtract.get("DataStoreWithoutUpdate.json"), true));
	}

	@Test
	public void testBackupCollectionRawData_JSON_STANDARD() throws Exception {
		insertData();
		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base",
				EngineBackupType.JSON_STANDARD);
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);
		engine.store(null);
		final Map<String, String> dataExtract = extractTarGzToMap(
				Paths.get("./").resolve("test_store_data_base.tar.gz"));
		Assertions.assertNotNull(dataExtract);
		Assertions.assertEquals(2, dataExtract.size());
		Assertions.assertEquals(
				"""
						{"_id": {"$oid":"IGNORE"}, "dataLong": 9953, "dataDoubles": [1.25, -1.66], "createdAt": {"$date": "IGNORE"}, "updatedAt": {"$date": "IGNORE"}}
						{"_id": {"$oid":"IGNORE"}, "dataDoubles": [152.8, 5.213546345E9], "createdAt": {"$date": "IGNORE"}, "updatedAt": {"$date": "IGNORE"}}
						""",
				revoveDateAndObjectId(dataExtract.get("DataStoreWithUpdate.json"), false));
		Assertions.assertEquals(
				"""
						{"_id": {"$oid":"IGNORE"}, "dataString": "my test String", "dataTime": {"$date": "2523-12-22T15:32:00.254Z"}}
						{"_id": {"$oid":"IGNORE"}, "dataString": "my second test string", "dataTime": {"$date": "2523-05-01T05:59:24.241Z"}}
						""",
				revoveDateAndObjectId(dataExtract.get("DataStoreWithoutUpdate.json"), true));
	}

	@Test
	public void testBackupCollectionRawData_JSON_EXTENDED() throws Exception {
		insertData();
		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base",
				EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.addClass(DataStoreWithoutUpdate.class);
		engine.store(null);
		final Map<String, String> dataExtract = extractTarGzToMap(
				Paths.get("./").resolve("test_store_data_base.tar.gz"));
		Assertions.assertNotNull(dataExtract);
		Assertions.assertEquals(2, dataExtract.size());
		Assertions.assertEquals(
				"""
						{"_id": {"$oid":"IGNORE"}, "dataLong": {"$numberLong": "9953"}, "dataDoubles": [{"$numberDouble": "1.25"}, {"$numberDouble": "-1.66"}], "createdAt": {"$date": {"$numberLong":"IGNORE"}}, "updatedAt": {"$date": {"$numberLong":"IGNORE"}}}
						{"_id": {"$oid":"IGNORE"}, "dataDoubles": [{"$numberDouble": "152.8"}, {"$numberDouble": "5.213546345E9"}], "createdAt": {"$date": {"$numberLong":"IGNORE"}}, "updatedAt": {"$date": {"$numberLong":"IGNORE"}}}
						""",
				revoveDateAndObjectId(dataExtract.get("DataStoreWithUpdate.json"), false));
		Assertions.assertEquals(
				"""
						{"_id": {"$oid":"IGNORE"}, "dataString": "my test String", "dataTime": {"$date": {"$numberLong": "17481713520254"}}}
						{"_id": {"$oid":"IGNORE"}, "dataString": "my second test string", "dataTime": {"$date": {"$numberLong": "17461375164241"}}}
						""",
				revoveDateAndObjectId(dataExtract.get("DataStoreWithoutUpdate.json"), true));
	}

	@Test
	public void testRestoreCollectionRawData_JSON_STANDARD() throws Exception {
		// Clean the collection
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);
		// Generate the Tag.gz file for test:
		final Map<String, String> data = Map.of( //
				"DataStoreWithUpdate.json",
				"""
						{"_id": {"$oid": "6855248a4345497e60f63c05"}, "dataLong": 9953, "dataDoubles": [1.25, -1.66], "createdAt": {"$date": "2025-06-20T12:08:18.788Z"}, "updatedAt": {"$date": "2025-06-10T09:09:18.212Z"}}
						{"_id": {"$oid": "6855248a4345497e60f63c07"}, "dataDoubles": [152.8, 5.213546345E9], "createdAt": {"$date": "2025-06-20T09:01:22.864Z"}, "updatedAt": {"$date": "2025-06-25T09:06:18.542Z"}}
						""", //
				"DataStoreWithoutUpdate.json",
				"""
						{"_id": {"$oid": "6855248a4345497e60f63c09"}, "dataString": "my test String", "dataTime": {"$date": "2523-12-22T01:32:00.254Z"}}
						{"_id": {"$oid": "6855248a4345497e60f63c0b"}, "dataString": "my second test string", "dataTime": {"$date": "2523-05-07T07:59:24.241Z"}}
						""");
		final Path fileTestPath = Paths.get("./").resolve("test_store_data_base.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base",
				EngineBackupType.JSON_STANDARD);
		engine.restore((String) null);
		{
			final List<DataStoreWithUpdate> testData = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
			Assertions.assertEquals("6855248a4345497e60f63c05", testData.get(0).oid.toString());
			Assertions.assertEquals("Fri Jun 20 12:08:18 UTC 2025", testData.get(0).createdAt.toString());
			Assertions.assertEquals("Tue Jun 10 09:09:18 UTC 2025", testData.get(0).updatedAt.toString());
			Assertions.assertEquals(9953, testData.get(0).dataLong);
			Assertions.assertEquals(2, testData.get(0).dataDoubles.size());
			Assertions.assertEquals(1.25, testData.get(0).dataDoubles.get(0), 0.0001);
			Assertions.assertEquals(-1.66, testData.get(0).dataDoubles.get(1), 0.0001);

			Assertions.assertEquals("6855248a4345497e60f63c07", testData.get(1).oid.toString());
			Assertions.assertEquals("Fri Jun 20 09:01:22 UTC 2025", testData.get(1).createdAt.toString());
			Assertions.assertEquals("Wed Jun 25 09:06:18 UTC 2025", testData.get(1).updatedAt.toString());
			Assertions.assertNull(testData.get(1).dataLong);
			Assertions.assertEquals(2, testData.get(1).dataDoubles.size());
			Assertions.assertEquals(152.8, testData.get(1).dataDoubles.get(0), 0.0001);
			Assertions.assertEquals(5213546345.0, testData.get(1).dataDoubles.get(1), 0.0001);
		}
		{
			final List<DataStoreWithoutUpdate> testData = DataAccess.gets(DataStoreWithoutUpdate.class);
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
			Assertions.assertEquals("6855248a4345497e60f63c09", testData.get(0).oid.toString());
			Assertions.assertEquals("my test String", testData.get(0).dataString);
			Assertions.assertEquals("Wed Dec 22 01:32:00 UTC 2523", testData.get(0).dataTime.toString());

			Assertions.assertEquals("6855248a4345497e60f63c0b", testData.get(1).oid.toString());
			Assertions.assertEquals("my second test string", testData.get(1).dataString);
			Assertions.assertEquals("Fri May 07 07:59:24 UTC 2523", testData.get(1).dataTime.toString());
		}
	}

	@Test
	public void testBackupWithSequence() throws Exception {
		insertData();
		final Path backupDir = Files.createTempDirectory("backup_seq_test");
		try {
			final BackupEngine engine = new BackupEngine(backupDir, "myapp", EngineBackupType.JSON_EXTENDED);
			engine.addClass(DataStoreWithUpdate.class);
			engine.addClass(DataStoreWithoutUpdate.class);
			final Date started = engine.store("2025-06-20");
			Assertions.assertNotNull(started);

			// Verify the file is named correctly with the sequence
			final Path expectedFile = backupDir.resolve("myapp_2025-06-20.tar.gz");
			Assertions.assertTrue(Files.exists(expectedFile), "Archive file should exist with sequence in name");

			// Verify archive content
			final Map<String, String> dataExtract = extractTarGzToMap(expectedFile);
			Assertions.assertEquals(2, dataExtract.size());
			Assertions.assertTrue(dataExtract.containsKey("DataStoreWithUpdate.json"));
			Assertions.assertTrue(dataExtract.containsKey("DataStoreWithoutUpdate.json"));
		} finally {
			// Cleanup
			try (var stream = Files.walk(backupDir)) {
				stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						// ignore cleanup errors
					}
				});
			}
		}
	}

	@Test
	public void testBackupWithAddCollection() throws Exception {
		insertData();
		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_addcol", EngineBackupType.JSON_EXTENDED);
		// Use addCollection instead of addClass
		engine.addCollection("DataStoreWithUpdate", "DataStoreWithoutUpdate");
		engine.store(null);

		final Map<String, String> dataExtract = extractTarGzToMap(Paths.get("./").resolve("test_addcol.tar.gz"));
		Assertions.assertNotNull(dataExtract);
		Assertions.assertEquals(2, dataExtract.size());
		Assertions.assertTrue(dataExtract.containsKey("DataStoreWithUpdate.json"));
		Assertions.assertTrue(dataExtract.containsKey("DataStoreWithoutUpdate.json"));

		// Cleanup
		Files.deleteIfExists(Paths.get("./").resolve("test_addcol.tar.gz"));
	}

	@Test
	public void testStoreAllAndRestoreRoundTrip() throws Exception {
		insertData();
		final Path backupDir = Files.createTempDirectory("backup_storeall_test");
		try {
			final BackupEngine storeEngine = new BackupEngine(backupDir, "allcol", EngineBackupType.JSON_EXTENDED);
			storeEngine.setEnableStoreOrRestoreData(false);
			storeEngine.storeAll("round-trip");

			final Path archivePath = backupDir.resolve("allcol_round-trip.tar.gz");
			Assertions.assertTrue(Files.exists(archivePath));

			// Verify archive has at least our test collections
			final Map<String, String> dataExtract = extractTarGzToMap(archivePath);
			Assertions.assertTrue(dataExtract.containsKey("DataStoreWithUpdate.json"));
			Assertions.assertTrue(dataExtract.containsKey("DataStoreWithoutUpdate.json"));

			// Drop collections and restore from file
			DataAccess.drop(DataStoreWithUpdate.class);
			DataAccess.drop(DataStoreWithoutUpdate.class);

			final BackupEngine restoreEngine = new BackupEngine(backupDir, "allcol", EngineBackupType.JSON_EXTENDED);
			restoreEngine.setEnableStoreOrRestoreData(false);
			final boolean result = restoreEngine.restoreFile(archivePath, null);
			Assertions.assertTrue(result);

			// Verify data is restored
			final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertEquals(2, restored.size());
			Assertions.assertEquals(9953, restored.get(0).dataLong);
		} finally {
			try (var stream = Files.walk(backupDir)) {
				stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						// ignore
					}
				});
			}
		}
	}

	@Test
	public void testRestoreWithSequenceRoundTrip() throws Exception {
		insertData();
		final Path backupDir = Files.createTempDirectory("backup_restore_seq_test");
		try {
			final BackupEngine engine = new BackupEngine(backupDir, "seqtest", EngineBackupType.JSON_EXTENDED);
			engine.addClass(DataStoreWithUpdate.class);
			engine.addClass(DataStoreWithoutUpdate.class);
			engine.setEnableStoreOrRestoreData(false);
			engine.store("2025-07-01");

			// Drop and restore using sequence
			DataAccess.drop(DataStoreWithUpdate.class);
			DataAccess.drop(DataStoreWithoutUpdate.class);

			engine.restore("2025-07-01");

			final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertEquals(2, restored.size());
		} finally {
			try (var stream = Files.walk(backupDir)) {
				stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						// ignore
					}
				});
			}
		}
	}

	@Test
	public void testRestoreFileReturnsFalseWhenDbNotEmpty() throws Exception {
		insertData();
		// Create a valid archive
		final Map<String, String> data = Map.of("DataStoreWithUpdate.json", """
				{"_id": {"$oid": "6855248a4345497e60f63c05"}, "dataLong": 1}
				""");
		final Path fileTestPath = Paths.get("./").resolve("test_notempty.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		try {
			// DB is not empty (insertData was called) — restoreFile should return false
			final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_notempty",
					EngineBackupType.JSON_EXTENDED);
			engine.setEnableStoreOrRestoreData(false);
			final boolean result = engine.restoreFile(fileTestPath, null);
			Assertions.assertFalse(result, "restoreFile should return false when DB is not empty");
		} finally {
			Files.deleteIfExists(fileTestPath);
		}
	}

	@Test
	public void testRestoreFileSingleCollection() throws Exception {
		// Clean both collections
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);

		// Insert data only in DataStoreWithUpdate (so DataStoreWithoutUpdate is empty)
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 123L;
		d.dataDoubles = List.of(1.0);
		DataAccess.insert(d);

		// Create an archive with both collections
		final Map<String, String> data = Map.of("DataStoreWithUpdate.json", """
				{"_id": {"$oid": "aaaaaaaaaaaaaaaaaaaaaaaa"}, "dataLong": 555}
				""", "DataStoreWithoutUpdate.json", """
				{"_id": {"$oid": "bbbbbbbbbbbbbbbbbbbbbbbb"}, "dataString": "filtered restore"}
				""");
		final Path fileTestPath = Paths.get("./").resolve("test_single_col.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		try {
			// Restore only DataStoreWithoutUpdate (which is empty), skipping DataStoreWithUpdate
			final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_single_col",
					EngineBackupType.JSON_EXTENDED);
			engine.setEnableStoreOrRestoreData(false);
			final boolean result = engine.restoreFile(fileTestPath, "DataStoreWithoutUpdate");
			Assertions.assertTrue(result);

			// DataStoreWithoutUpdate should have the restored data
			final List<DataStoreWithoutUpdate> restored = DataAccess.gets(DataStoreWithoutUpdate.class);
			Assertions.assertEquals(1, restored.size());
			Assertions.assertEquals("filtered restore", restored.get(0).dataString);

			// DataStoreWithUpdate should still have the original data (not overwritten)
			final List<DataStoreWithUpdate> untouched = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertEquals(1, untouched.size());
			Assertions.assertEquals(123L, untouched.get(0).dataLong);
		} finally {
			Files.deleteIfExists(fileTestPath);
		}
	}

	@Test
	public void testRestoreFailsWhenCollectionNotEmpty() throws Exception {
		// Insert data so the collection is not empty
		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 1L;
		d.dataDoubles = List.of(1.0);
		DataAccess.insert(d);

		// Create an archive with the same collection
		final Map<String, String> data = Map.of("DataStoreWithUpdate.json", """
				{"_id": {"$oid": "cccccccccccccccccccccccc"}, "dataLong": 999}
				""");
		final Path fileTestPath = Paths.get("./").resolve("test_conflict.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		try {
			final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_conflict",
					EngineBackupType.JSON_EXTENDED);
			engine.setEnableStoreOrRestoreData(false);
			// Restoring a specific collection that is not empty should throw
			Assertions.assertThrows(IOException.class, () -> {
				engine.restoreFile(fileTestPath, "DataStoreWithUpdate");
			});
		} finally {
			Files.deleteIfExists(fileTestPath);
		}
	}

	@Test
	public void testRestoreSkipsNonJsonEntries() throws Exception {
		// Non-.json entries in a tar archive should be silently skipped during collection restore
		DataAccess.drop(DataStoreWithUpdate.class);
		DataAccess.drop(DataStoreWithoutUpdate.class);

		// Build an archive with a .json entry and a non-.json entry (e.g. a media file)
		final Map<String, String> data = Map.of( //
				"DataStoreWithUpdate.json",
				"""
						{"_id": {"$oid": "aaaaaaaaaaaaaaaaaaaaaaaa"}, "dataLong": {"$numberLong": "42"}, "dataDoubles": [{"$numberDouble": "1.0"}]}
						""", //
				"data/some-media-file.png", "not real image data", //
				"README.txt", "this is not JSON");
		final Path fileTestPath = Paths.get("./").resolve("test_skip_nonjson.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		try {
			final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_skip_nonjson",
					EngineBackupType.JSON_EXTENDED);
			engine.setEnableStoreOrRestoreData(false);
			final boolean result = engine.restoreFile(fileTestPath, null);
			Assertions.assertTrue(result);

			// Only the .json entry should have been restored
			final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertEquals(1, restored.size());
			Assertions.assertEquals(42L, restored.get(0).dataLong);
		} finally {
			Files.deleteIfExists(fileTestPath);
		}
	}

	@Test
	public void testRestoreFileLargeBatch() throws Exception {
		// Test the batch insertMany path (>=1000 documents) via file-based restore
		DataAccess.drop(DataStoreWithUpdate.class);

		// Build an archive with 1050 documents
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1050; i++) {
			sb.append(String.format(
					"{\"_id\": {\"$oid\": \"%024x\"}, \"dataLong\": {\"$numberLong\": \"%d\"}, \"dataDoubles\": [{\"$numberDouble\": \"%d.0\"}]}%n",
					i + 1, i, i));
		}
		final Map<String, String> data = Map.of("DataStoreWithUpdate.json", sb.toString());
		final Path fileTestPath = Paths.get("./").resolve("test_large_batch.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		try {
			final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_large_batch",
					EngineBackupType.JSON_EXTENDED);
			engine.setEnableStoreOrRestoreData(false);
			// Use specific collection name to avoid "DB not empty" check (other tests may leave collections)
			final boolean result = engine.restoreFile(fileTestPath, "DataStoreWithUpdate");
			Assertions.assertTrue(result);

			final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertEquals(1050, restored.size());
		} finally {
			Files.deleteIfExists(fileTestPath);
		}
	}

	@Test
	public void testRestoreCollectionRawData_JSON_EXTENDED() throws Exception {
		// Clean the collection
		DataAccess.drop(DataStoreWithoutUpdate.class);
		DataAccess.drop(DataStoreWithUpdate.class);
		// Generate the Tag.gz file for test:
		final Map<String, String> data = Map.of( //
				"DataStoreWithUpdate.json",
				"""
						{"_id": {"$oid": "685524d44345497e60f63c15"}, "dataLong": {"$numberLong": "9953"}, "dataDoubles": [{"$numberDouble": "1.25"}, {"$numberDouble": "-1.66"}], "createdAt": {"$date": {"$numberLong": "1750410458756"}}, "updatedAt": {"$date": {"$numberLong": "1750410685442"}}}
						{"_id": {"$oid": "685524d44345497e60f63c17"}, "dataDoubles": [{"$numberDouble": "152.8"}, {"$numberDouble": "5.213546345E9"}], "createdAt": {"$date": {"$numberLong": "1750410455264"}}, "updatedAt": {"$date": {"$numberLong": "1750410548562"}}}
						""", //
				"DataStoreWithoutUpdate.json",
				"""
						{"_id": {"$oid": "685524d44345497e60f63c19"}, "dataString": "my test String", "dataTime": {"$date": {"$numberLong": "17481713520254"}}}
						{"_id": {"$oid": "685524d44345497e60f63c1b"}, "dataString": "my second test string", "dataTime": {"$date": {"$numberLong": "17461375164241"}}}
						""");
		final Path fileTestPath = Paths.get("./").resolve("test_store_data_base.tar.gz");
		writeMapToTarGz(data, fileTestPath);

		final BackupEngine engine = new BackupEngine(Paths.get("./"), "test_store_data_base",
				EngineBackupType.JSON_EXTENDED);
		engine.restore((String) null);
		{
			final List<DataStoreWithUpdate> testData = DataAccess.gets(DataStoreWithUpdate.class,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
			Assertions.assertEquals("685524d44345497e60f63c15", testData.get(0).oid.toString());
			Assertions.assertEquals("Fri Jun 20 09:07:38 UTC 2025", testData.get(0).createdAt.toString());
			Assertions.assertEquals("Fri Jun 20 09:11:25 UTC 2025", testData.get(0).updatedAt.toString());
			Assertions.assertEquals(9953, testData.get(0).dataLong);
			Assertions.assertEquals(2, testData.get(0).dataDoubles.size());
			Assertions.assertEquals(1.25, testData.get(0).dataDoubles.get(0), 0.0001);
			Assertions.assertEquals(-1.66, testData.get(0).dataDoubles.get(1), 0.0001);

			Assertions.assertEquals("685524d44345497e60f63c17", testData.get(1).oid.toString());
			Assertions.assertEquals("Fri Jun 20 09:07:35 UTC 2025", testData.get(1).createdAt.toString());
			Assertions.assertEquals("Fri Jun 20 09:09:08 UTC 2025", testData.get(1).updatedAt.toString());
			Assertions.assertNull(testData.get(1).dataLong);
			Assertions.assertEquals(2, testData.get(1).dataDoubles.size());
			Assertions.assertEquals(152.8, testData.get(1).dataDoubles.get(0), 0.0001);
			Assertions.assertEquals(5213546345.0, testData.get(1).dataDoubles.get(1), 0.0001);
		}
		{
			final List<DataStoreWithoutUpdate> testData = DataAccess.gets(DataStoreWithoutUpdate.class);
			Assertions.assertNotNull(testData);
			Assertions.assertEquals(2, testData.size());
			Assertions.assertEquals("685524d44345497e60f63c19", testData.get(0).oid.toString());
			Assertions.assertEquals("my test String", testData.get(0).dataString);
			Assertions.assertEquals("Wed Dec 22 15:32:00 UTC 2523", testData.get(0).dataTime.toString());

			Assertions.assertEquals("685524d44345497e60f63c1b", testData.get(1).oid.toString());
			Assertions.assertEquals("my second test string", testData.get(1).dataString);
			Assertions.assertEquals("Sat May 01 05:59:24 UTC 2523", testData.get(1).dataTime.toString());
		}
	}
}
