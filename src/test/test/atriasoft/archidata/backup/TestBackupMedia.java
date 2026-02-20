package test.atriasoft.archidata.backup;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.backup.model.DataStoreWithUpdate;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBackupMedia {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestBackupMedia.class);

	private Path tempMediaDir;
	private Path tempBackupDir;

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

	@BeforeEach
	public void setUpMediaFolder() throws IOException {
		tempMediaDir = Files.createTempDirectory("test_media_");
		tempBackupDir = Files.createTempDirectory("test_backup_media_");
		ConfigBaseVariable.dataFolder = tempMediaDir.toString();
	}

	@AfterEach
	public void tearDownMediaFolder() throws IOException {
		ConfigBaseVariable.dataFolder = null;
		// Cleanup temp directories
		deleteRecursive(tempMediaDir);
		deleteRecursive(tempBackupDir);
	}

	private void deleteRecursive(final Path dir) throws IOException {
		if (dir != null && Files.exists(dir)) {
			try (var stream = Files.walk(dir)) {
				stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (final IOException e) {
						// ignore cleanup errors
					}
				});
			}
		}
	}

	private Map<String, byte[]> extractTarGzToByteMap(final Path inputPath) throws IOException {
		final Map<String, byte[]> result = new HashMap<>();
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
				result.put(entry.getName(), out.toByteArray());
			}
		}
		return result;
	}

	@Test
	@Order(1)
	public void testBackupIncludesMediaFiles() throws Exception {
		// Create media files in the configured data folder
		final Path subDir = tempMediaDir.resolve("images");
		Files.createDirectories(subDir);
		Files.writeString(tempMediaDir.resolve("readme.txt"), "hello world");
		Files.writeString(subDir.resolve("photo.txt"), "fake image data");

		// Also insert a DB document so the backup has both DB and media content
		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 1L;
		d.dataDoubles = List.of(1.0);
		DataAccess.insert(d);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "media_test", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		// enableStoreOrRestoreData is true by default → media files included
		engine.store(null);

		final Path archivePath = tempBackupDir.resolve("media_test.tar.gz");
		Assertions.assertTrue(Files.exists(archivePath));

		// Extract and verify the archive contains both JSON collection and media files
		final Map<String, byte[]> entries = extractTarGzToByteMap(archivePath);
		Assertions.assertTrue(entries.containsKey("DataStoreWithUpdate.json"), "Should contain DB collection");
		Assertions.assertTrue(entries.containsKey("data/readme.txt"), "Should contain media file at root");
		Assertions.assertTrue(entries.containsKey("data/images/photo.txt"),
				"Should contain media file in subdirectory");

		// Verify media content
		Assertions.assertEquals("hello world", new String(entries.get("data/readme.txt"), StandardCharsets.UTF_8));
		Assertions.assertEquals("fake image data",
				new String(entries.get("data/images/photo.txt"), StandardCharsets.UTF_8));
	}

	@Test
	@Order(2)
	public void testBackupSkipsHistoryRestoreFolder() throws Exception {
		// The history_restore folder at the top level of media should be skipped
		final Path historyDir = tempMediaDir.resolve("history_restore");
		Files.createDirectories(historyDir);
		Files.writeString(historyDir.resolve("old_backup.txt"), "should be skipped");
		Files.writeString(tempMediaDir.resolve("keep.txt"), "should be included");

		DataAccess.drop(DataStoreWithUpdate.class);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "skip_history", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.store(null);

		final Path archivePath = tempBackupDir.resolve("skip_history.tar.gz");
		final Map<String, byte[]> entries = extractTarGzToByteMap(archivePath);

		Assertions.assertTrue(entries.containsKey("data/keep.txt"), "Regular media files should be included");
		Assertions.assertFalse(entries.containsKey("data/history_restore/old_backup.txt"),
				"history_restore content should be skipped");
		// Also check that no entry starts with "data/history_restore"
		for (final String key : entries.keySet()) {
			Assertions.assertFalse(key.startsWith("data/history_restore"),
					"No entry should start with data/history_restore: " + key);
		}
	}

	@Test
	@Order(3)
	public void testBackupWithNoMediaFolder() throws Exception {
		// If the media folder does not exist, backup should still succeed (just no data/ entries)
		deleteRecursive(tempMediaDir);

		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 5L;
		d.dataDoubles = List.of(5.0);
		DataAccess.insert(d);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "no_media", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.store(null);

		final Path archivePath = tempBackupDir.resolve("no_media.tar.gz");
		Assertions.assertTrue(Files.exists(archivePath));

		final Map<String, byte[]> entries = extractTarGzToByteMap(archivePath);
		Assertions.assertTrue(entries.containsKey("DataStoreWithUpdate.json"));
		// No data/ entries since media folder doesn't exist
		for (final String key : entries.keySet()) {
			Assertions.assertFalse(key.startsWith("data/"), "No media entries expected: " + key);
		}
	}

	@Test
	@Order(4)
	public void testRestoreMediaFiles() throws Exception {
		// Create media files, backup, then delete and restore
		Files.writeString(tempMediaDir.resolve("original.txt"), "original content");
		final Path subDir = tempMediaDir.resolve("sub");
		Files.createDirectories(subDir);
		Files.writeString(subDir.resolve("nested.txt"), "nested content");

		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 10L;
		d.dataDoubles = List.of(10.0);
		DataAccess.insert(d);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "restore_media", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.store("snap1");

		// Delete media files and DB
		deleteRecursive(tempMediaDir);
		Files.createDirectories(tempMediaDir);
		DataAccess.drop(DataStoreWithUpdate.class);

		// Restore
		engine.restore("snap1");

		// Verify DB restored
		final List<DataStoreWithUpdate> restored = DataAccess.gets(DataStoreWithUpdate.class, new AccessDeletedItems(),
				new ReadAllColumn());
		Assertions.assertEquals(1, restored.size());
		Assertions.assertEquals(10L, restored.get(0).dataLong);

		// Verify media files restored
		Assertions.assertTrue(Files.exists(tempMediaDir.resolve("original.txt")));
		Assertions.assertEquals("original content", Files.readString(tempMediaDir.resolve("original.txt")));
		Assertions.assertTrue(Files.exists(tempMediaDir.resolve("sub").resolve("nested.txt")));
		Assertions.assertEquals("nested content", Files.readString(tempMediaDir.resolve("sub").resolve("nested.txt")));
	}

	@Test
	@Order(5)
	public void testRestoreMovesExistingMediaToHistory() throws Exception {
		// Create initial media
		Files.writeString(tempMediaDir.resolve("old_file.txt"), "old data");

		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 20L;
		d.dataDoubles = List.of(20.0);
		DataAccess.insert(d);

		// Backup
		final BackupEngine engine = new BackupEngine(tempBackupDir, "history_test", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.store("v1");

		// Now add new files that should be moved to history_restore on restore
		DataAccess.drop(DataStoreWithUpdate.class);
		Files.writeString(tempMediaDir.resolve("new_file.txt"), "new data that should go to history");

		// Restore — existing media should be moved to history_restore/
		engine.restore("v1");

		// The history_restore directory should exist and contain the moved files
		Assertions.assertTrue(Files.isDirectory(tempMediaDir.resolve("history_restore")),
				"history_restore folder should be created");

		// There should be a timestamped sub-directory inside history_restore
		try (var historyStream = Files.list(tempMediaDir.resolve("history_restore"))) {
			final List<Path> historyDirs = historyStream.filter(Files::isDirectory).toList();
			Assertions.assertFalse(historyDirs.isEmpty(), "Should have at least one timestamped history directory");

			// The moved file should be in one of the history directories
			boolean foundMovedFile = false;
			for (final Path histDir : historyDirs) {
				if (Files.exists(histDir.resolve("new_file.txt"))) {
					foundMovedFile = true;
					Assertions.assertEquals("new data that should go to history",
							Files.readString(histDir.resolve("new_file.txt")));
				}
			}
			Assertions.assertTrue(foundMovedFile, "new_file.txt should have been moved to history_restore");
		}

		// The restored file should be present
		Assertions.assertTrue(Files.exists(tempMediaDir.resolve("old_file.txt")));
		Assertions.assertEquals("old data", Files.readString(tempMediaDir.resolve("old_file.txt")));
	}

	@Test
	@Order(6)
	public void testBackupDisabledDataExcludesMedia() throws Exception {
		// When enableStoreOrRestoreData is false, media files should not be in the archive
		Files.writeString(tempMediaDir.resolve("should_not_appear.txt"), "excluded");

		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 30L;
		d.dataDoubles = List.of(30.0);
		DataAccess.insert(d);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "no_data", EngineBackupType.JSON_EXTENDED);
		engine.addClass(DataStoreWithUpdate.class);
		engine.setEnableStoreOrRestoreData(false);
		engine.store(null);

		final Path archivePath = tempBackupDir.resolve("no_data.tar.gz");
		final Map<String, byte[]> entries = extractTarGzToByteMap(archivePath);

		Assertions.assertTrue(entries.containsKey("DataStoreWithUpdate.json"), "DB collection should be included");
		for (final String key : entries.keySet()) {
			Assertions.assertFalse(key.startsWith("data/"), "No media entries expected when data is disabled: " + key);
		}
	}

	@Test
	@Order(7)
	public void testStoreAllIncludesMediaFiles() throws Exception {
		// storeAll should also include media files when enabled
		Files.writeString(tempMediaDir.resolve("storeall_media.txt"), "storeall content");

		DataAccess.drop(DataStoreWithUpdate.class);
		final DataStoreWithUpdate d = new DataStoreWithUpdate();
		d.dataLong = 40L;
		d.dataDoubles = List.of(40.0);
		DataAccess.insert(d);

		final BackupEngine engine = new BackupEngine(tempBackupDir, "storeall_media", EngineBackupType.JSON_EXTENDED);
		// No addClass — storeAll discovers automatically
		engine.storeAll(null);

		final Path archivePath = tempBackupDir.resolve("storeall_media.tar.gz");
		final Map<String, byte[]> entries = extractTarGzToByteMap(archivePath);

		Assertions.assertTrue(entries.containsKey("DataStoreWithUpdate.json"), "Should contain DB collection");
		Assertions.assertTrue(entries.containsKey("data/storeall_media.txt"), "Should contain media file");
		Assertions.assertEquals("storeall content",
				new String(entries.get("data/storeall_media.txt"), StandardCharsets.UTF_8));
	}
}
