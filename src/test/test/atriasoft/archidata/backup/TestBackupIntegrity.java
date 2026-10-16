package test.atriasoft.archidata.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import test.atriasoft.archidata.ConfigureDb;

/** D-04: archives are private (0600) and carry a SHA-256 sum checked at restore. */
public class TestBackupIntegrity {
	private static final String COLLECTION = "backupIntegrity";

	private Path tempRoot;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@BeforeEach
	public void setUpFolders() throws IOException {
		this.tempRoot = Files.createTempDirectory("test_backup_integrity_");
		collection().drop();
		collection().insertOne(new Document("name", "one"));
		collection().insertOne(new Document("name", "two"));
	}

	@AfterEach
	public void tearDownFolders() throws IOException {
		BackupTestTools.deleteRecursive(this.tempRoot);
	}

	private static MongoCollection<Document> collection() {
		final MongoDatabase db = ConfigureDb.da.getInterface().getDatabase();
		return db.getCollection(COLLECTION);
	}

	private BackupEngine engine(final Path store) {
		final BackupEngine engine = new BackupEngine(store, "integrity", EngineBackupType.JSON_EXTENDED);
		engine.addCollection(COLLECTION);
		engine.setEnableStoreOrRestoreData(false);
		return engine;
	}

	private static String sha256(final Path file) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
	}

	@Test
	public void testArchiveIsPrivateAndSummed() throws Exception {
		final Path store = this.tempRoot.resolve("new-store");
		final BackupEngine engine = engine(store);
		engine.store("2026-10-16");
		final Path archive = store.resolve("integrity_2026-10-16.tar.gz");
		final Path sidecar = BackupEngine.checksumPath(archive);
		Assertions.assertTrue(Files.exists(archive));
		Assertions.assertTrue(Files.exists(sidecar));
		Assertions.assertTrue(Files.readString(sidecar).startsWith(sha256(archive) + "  integrity_2026-10-16.tar.gz"));
		if (archive.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			Assertions.assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(archive)));
			Assertions.assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(sidecar)));
			Assertions.assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store)));
		}
		// The listing used by the retention ignores the sidecar.
		Assertions.assertEquals(1, engine.getLatestRestoreChain().size());

		collection().drop();
		Assertions.assertTrue(engine.restoreFile(archive, COLLECTION));
		Assertions.assertEquals(2, collection().countDocuments());
	}

	@Test
	public void testTamperedArchiveIsRefused() throws Exception {
		final BackupEngine engine = engine(this.tempRoot);
		engine.store("2026-10-16");
		final Path archive = this.tempRoot.resolve("integrity_2026-10-16.tar.gz");
		Files.write(archive, new byte[] { 0 }, StandardOpenOption.APPEND);
		collection().drop();
		final IOException ex = Assertions.assertThrows(IOException.class,
				() -> engine.restoreFile(archive, COLLECTION));
		Assertions.assertTrue(ex.getMessage().contains("Checksum mismatch"), ex.getMessage());
		Assertions.assertEquals(0, collection().countDocuments());
	}

	@Test
	public void testMissingSidecar() throws Exception {
		final BackupEngine engine = engine(this.tempRoot);
		engine.store("2026-10-16");
		final Path archive = this.tempRoot.resolve("integrity_2026-10-16.tar.gz");
		Files.delete(BackupEngine.checksumPath(archive));
		collection().drop();
		engine.setRequireChecksum(true);
		Assertions.assertThrows(IOException.class, () -> engine.restoreFile(archive, COLLECTION));
		Assertions.assertEquals(0, collection().countDocuments());
		// Archives of older versions (no sidecar) stay restorable by default, with a warning.
		engine.setRequireChecksum(false);
		Assertions.assertTrue(engine.restoreFile(archive, COLLECTION));
		Assertions.assertEquals(2, collection().countDocuments());
	}
}
