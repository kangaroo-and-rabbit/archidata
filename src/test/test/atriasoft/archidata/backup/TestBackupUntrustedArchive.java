package test.atriasoft.archidata.backup;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-05: restoring an untrusted archive cannot escape the media folder, nor exhaust the disk or the memory. */
public class TestBackupUntrustedArchive {
	private Path root;
	private Path media;
	private Path store;

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
		this.root = Files.createTempDirectory("test_backup_untrusted_");
		this.media = this.root.resolve("media");
		this.store = this.root.resolve("store");
		Files.createDirectories(this.media);
		Files.createDirectories(this.store);
		ConfigBaseVariable.setDataFolder(this.media.toString());
	}

	@AfterEach
	public void tearDownFolders() throws IOException {
		ConfigBaseVariable.setDataFolder(null);
		BackupTestTools.deleteRecursive(this.root);
	}

	private BackupEngine engine() {
		return new BackupEngine(this.store, "untrusted", EngineBackupType.JSON_EXTENDED);
	}

	/** Write an archive: name to content; a content starting with "->" makes a symbolic link entry. */
	private Path archive(final Map<String, String> entries) throws IOException {
		final Path out = this.store.resolve("untrusted_2026-10-16_delta.tar.gz");
		try (OutputStream fileOut = Files.newOutputStream(out);
				GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(fileOut);
				TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)) {
			tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			for (final Map.Entry<String, String> entry : entries.entrySet()) {
				if (entry.getValue().startsWith("->")) {
					final TarArchiveEntry link = new TarArchiveEntry(entry.getKey(), TarConstants.LF_SYMLINK);
					link.setLinkName(entry.getValue().substring(2));
					tarOut.putArchiveEntry(link);
					tarOut.closeArchiveEntry();
					continue;
				}
				final byte[] content = entry.getValue().getBytes(StandardCharsets.UTF_8);
				final TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
				tarEntry.setSize(content.length);
				tarOut.putArchiveEntry(tarEntry);
				tarOut.write(content);
				tarOut.closeArchiveEntry();
			}
		}
		return out;
	}

	@Test
	public void testTraversalAndLinksAreRefused() throws Exception {
		final Path outside = this.root.resolve("outside");
		Files.createDirectories(outside);
		// A link already present in the media folder, pointing outside of it.
		Files.createSymbolicLink(this.media.resolve("out"), outside);
		final Map<String, String> entries = new LinkedHashMap<>();
		entries.put("data/ok/file.txt", "fine");
		// Sibling directory sharing the prefix: "/x/media-other" starts with "/x/media" as a string.
		entries.put("data/../media-other/evil.txt", "evil");
		entries.put("data/../../escape.txt", "evil");
		entries.put("data/out/pwn.txt", "evil");
		entries.put("data/link", "->/etc/passwd");
		engine().restoreDeltaFile(archive(entries));

		Assertions.assertEquals("fine", Files.readString(this.media.resolve("ok/file.txt")));
		Assertions.assertFalse(Files.exists(this.root.resolve("media-other/evil.txt")));
		Assertions.assertFalse(Files.exists(this.root.getParent().resolve("escape.txt")));
		Assertions.assertFalse(Files.exists(outside.resolve("pwn.txt")));
		Assertions.assertFalse(Files.exists(this.media.resolve("link"), LinkOption.NOFOLLOW_LINKS));
	}

	@Test
	public void testSizesAreBounded() throws Exception {
		final Map<String, String> entries = Map.of("data/big.bin", "x".repeat(100));
		final BackupEngine perEntry = engine();
		perEntry.setRestoreMaxEntryBytes(10);
		Assertions.assertThrows(IOException.class, () -> perEntry.restoreDeltaFile(archive(entries)));
		Assertions.assertFalse(Files.exists(this.media.resolve("big.bin")));

		final Map<String, String> many = new LinkedHashMap<>();
		many.put("data/a.bin", "x".repeat(60));
		many.put("data/b.bin", "x".repeat(60));
		final BackupEngine total = engine();
		total.setRestoreMaxTotalBytes(100);
		Assertions.assertThrows(IOException.class, () -> total.restoreDeltaFile(archive(many)));

		final BackupEngine line = engine();
		line.setRestoreMaxLineChars(10);
		Assertions.assertThrows(IOException.class, () -> line
				.restoreDeltaFile(archive(Map.of("untrustedLines.json", "{\"name\": \"" + "y".repeat(50) + "\"}\n"))));
	}

	@Test
	public void testLongLinesAcrossReadBlocks() throws Exception {
		// Lines longer than the read block, CRLF and LF terminators, a last line without terminator.
		final String big = "z".repeat(20_000);
		final String content = "{\"_id\": 1, \"v\": \"" + big + "\"}\r\n" + "{\"_id\": 2, \"v\": \"b\"}\n"
				+ "{\"_id\": 3, \"v\": \"" + big + big + "\"}";
		final BackupEngine engine = engine();
		engine.setRestoreMaxLineChars(50_000);
		engine.restoreDeltaFile(archive(Map.of("longLines.json", content)));
		final com.mongodb.client.MongoCollection<org.bson.Document> collection = ConfigureDb.da.getInterface()
				.getDatabase().getCollection("longLines");
		try {
			Assertions.assertEquals(3, collection.countDocuments());
			Assertions.assertEquals(big, collection.find(new org.bson.Document("_id", 1)).first().getString("v"));
			Assertions.assertEquals("b", collection.find(new org.bson.Document("_id", 2)).first().getString("v"));
			Assertions.assertEquals(big + big, collection.find(new org.bson.Document("_id", 3)).first().getString("v"));
		} finally {
			collection.drop();
		}
		final BackupEngine tooShort = engine();
		tooShort.setRestoreMaxLineChars(30_000);
		Assertions.assertThrows(IOException.class,
				() -> tooShort.restoreDeltaFile(archive(Map.of("longLines.json", content))));
		ConfigureDb.da.getInterface().getDatabase().getCollection("longLines").drop();
	}

	@Test
	public void testUnsafeCollectionNamesAreRefused() throws Exception {
		for (final String name : new String[] { "system.js.json", "bad$name.json", ".hidden.json" }) {
			final Path file = archive(Map.of(name, "{\"_id\": 1}\n"));
			final IOException ex = Assertions.assertThrows(IOException.class, () -> engine().restoreDeltaFile(file),
					name);
			Assertions.assertTrue(ex.getMessage().contains("collection name refused"), ex.getMessage());
		}
		Assertions.assertThrows(IllegalArgumentException.class, () -> engine().restore("../../etc/x"));
	}
}
