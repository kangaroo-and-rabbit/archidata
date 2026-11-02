package org.atriasoft.archidata.backup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.atriasoft.archidata.tools.DateTools;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;

/**
 * Engine for backing up and restoring a MongoDB database to/from tar.gz archives.
 * <p>
 * Collections to back up can be registered explicitly via {@link #addClass} or {@link #addCollection},
 * or discovered automatically via {@link #storeAll}. Media files from the configured data folder are
 * also included unless disabled with {@link #setEnableStoreOrRestoreData}.
 * <p>
 * Archives are stored in a configurable directory with a naming convention based on a base name
 * and a caller-provided sequence string (typically a date such as {@code "2025-06-20"}).
 * A {@link RetentionPolicy} can be applied via {@link #clean} to automatically remove old backups.
 * <p>
 * <b>Security.</b> An archive holds the whole database <b>in clear</b> (every field, {@code @DataNotRead}
 * ones included; only the {@code @DataEncrypt} fields stay encrypted) and the media files. It is created
 * readable by its owner only ({@code rw-------} on a POSIX file system, as is a store directory created by
 * the engine), and a SHA-256 sum is written next to it ({@code <archive>.sha256}) and checked before any
 * restore. Keep the archives on a storage reserved to the service, and encrypt them outside archidata if
 * they leave it (no archive encryption yet).
 */
public class BackupEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackupEngine.class);

	/** JSON serialization mode used when writing collection documents to the archive. */
	public static enum EngineBackupType {
		/** Serialize via Jackson ObjectMapper: dates become ISO strings, ObjectId become plain strings. Lossy. */
		JSON_EXTERNAL,
		/** Serialize via BSON relaxed JSON mode. */
		JSON_STANDARD,
		/** Serialize via BSON extended JSON mode. Lossless round-trip with MongoDB types. Recommended. */
		JSON_EXTENDED,
	}

	private static final String SUFFIX_TAR_GZ = ".tar.gz";
	private static final String SUFFIX_PARTIAL = "_partial";
	private static final String SUFFIX_DELTA = "_delta";
	/** Part of the name of a delta that holds the date of its full: {@code base<yyyyMMddHHmmssSSS>}. */
	private static final String DELTA_BASE_PREFIX = "base";
	private static final Pattern DELTA_BASE = Pattern.compile("_" + DELTA_BASE_PREFIX + "([0-9]{17})(?:_|$)");
	private static final Pattern DELTA_BASE_PART = Pattern.compile(DELTA_BASE_PREFIX + "[0-9]{17}");
	/**
	 * How far after the current UTC time an archive stays valid for a restore. An application can name its sequences in
	 * local time, up to a day ahead of UTC: its latest archives are no clock that jumped. The {@code _base} of a delta
	 * remains what protects the deltas of {@link BackupService} inside this day.
	 */
	private static final Duration RESTORE_FUTURE_TOLERANCE = Duration.ofDays(1);
	private static final DateTimeFormatter DELTA_BASE_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS",
			Locale.ROOT);
	private static final String SUFFIX_JSON = ".json";
	/** Suffix of a file being written, with a leading dot on its name: {@code .<archive>_tmp}. */
	private static final String SUFFIX_TMP = "_tmp";
	/** Suffix of the SHA-256 sidecar file written next to every archive. */
	public static final String SUFFIX_SHA256 = ".sha256";
	/**
	 * Collection names accepted at restore: letters, digits, {@code _ - .}, not starting with a dot, at most
	 * 120 characters. {@code system.*} is refused separately.
	 */
	private static final Pattern SAFE_COLLECTION_NAME = Pattern.compile("^[A-Za-z0-9_-][A-Za-z0-9_.-]{0,119}$");
	/** Default maximum declared size of a single archive entry accepted at restore (64 GiB, media files). */
	public static final long DEFAULT_RESTORE_MAX_ENTRY_BYTES = 64L * 1024 * 1024 * 1024;
	/** Default maximum total declared size of the entries of an archive accepted at restore (1 TiB). */
	public static final long DEFAULT_RESTORE_MAX_TOTAL_BYTES = 1024L * 1024 * 1024 * 1024;
	/** Default maximum length (in characters) of one NDJSON document line at restore (64 Mi). */
	public static final int DEFAULT_RESTORE_MAX_LINE_CHARS = 64 * 1024 * 1024;
	/** Size of the blocks read by {@link #readBoundedLine} (a line is never read character by character). */
	private static final int READ_LINE_CHUNK = 8192;
	private static final Set<PosixFilePermission> OWNER_FILE = PosixFilePermissions.fromString("rw-------");
	private static final Set<PosixFilePermission> OWNER_DIRECTORY = PosixFilePermissions.fromString("rwx------");
	/** Matches the chunk suffix appended to secondary archive entries of a collection (e.g. {@code __000001}). */
	private static final Pattern CHUNK_SUFFIX_PATTERN = Pattern.compile("__[0-9]+$");
	/** Number of documents fetched per round-trip when iterating a collection cursor. */
	private static final int CURSOR_BATCH_SIZE = 1000;

	/** Default maximum size (in bytes) of a single collection chunk in the archive. */
	public static final int DEFAULT_CHUNK_MAX_BYTES = 32 * 1024 * 1024;
	/** Default maximum number of documents in a single collection chunk in the archive. */
	public static final int DEFAULT_CHUNK_MAX_DOCUMENTS = 100_000;
	/** Default maximum accumulated size (in bytes) of an insertMany batch at restore. */
	public static final int DEFAULT_RESTORE_BATCH_MAX_BYTES = 32 * 1024 * 1024;
	/** Default maximum number of documents in an insertMany batch at restore. */
	public static final int DEFAULT_RESTORE_BATCH_MAX_DOCUMENTS = 10_000;

	private final Path pathStore;
	private final String baseName;
	private final EngineBackupType type;
	/** A set: the same collection twice in an archive fails at restore. */
	private final Set<String> collectionNames = new LinkedHashSet<>();
	private boolean enableStoreOrRestoreData = true;
	private int chunkMaxBytes = DEFAULT_CHUNK_MAX_BYTES;
	private int chunkMaxDocuments = DEFAULT_CHUNK_MAX_DOCUMENTS;
	private int restoreBatchMaxBytes = DEFAULT_RESTORE_BATCH_MAX_BYTES;
	private int restoreBatchMaxDocuments = DEFAULT_RESTORE_BATCH_MAX_DOCUMENTS;
	private boolean restoreRelaxedWriteConcern = true;
	private boolean requireChecksum = false;
	private long restoreMaxEntryBytes = DEFAULT_RESTORE_MAX_ENTRY_BYTES;
	private long restoreMaxTotalBytes = DEFAULT_RESTORE_MAX_TOTAL_BYTES;
	private int restoreMaxLineChars = DEFAULT_RESTORE_MAX_LINE_CHARS;

	/**
	 * Create a new backup engine.
	 * @param pathToStoreDB directory where archive files are stored
	 * @param baseName prefix used for archive filenames and for identifying the target database
	 * @param type JSON serialization mode for collection documents
	 */
	public BackupEngine(final Path pathToStoreDB, final String baseName, final EngineBackupType type) {
		this.pathStore = pathToStoreDB;
		this.baseName = baseName;
		this.type = type;
	}

	/** @return the directory of the archives */
	Path getPathStore() {
		return this.pathStore;
	}

	/**
	 * Enable or disable backup/restore of media files (the data folder).
	 * When disabled, only MongoDB collections are included in the archive.
	 * Useful when media data is too large or managed separately.
	 * @param value {@code true} to include media files (default), {@code false} to skip them
	 */
	public void setEnableStoreOrRestoreData(boolean value) {
		this.enableStoreOrRestoreData = value;
	}

	/**
	 * Set the maximum size (in bytes) of a single collection chunk in the archive.
	 * When a collection exceeds this size, it is split into multiple archive entries:
	 * {@code <collection>.json}, {@code <collection>__000001.json}, {@code <collection>__000002.json}, ...
	 * Bounding the chunk size bounds the backup memory usage whatever the collection size is.
	 * @param value maximum chunk size in bytes (default {@link #DEFAULT_CHUNK_MAX_BYTES})
	 */
	public void setChunkMaxBytes(final int value) {
		this.chunkMaxBytes = value;
	}

	/**
	 * Set the maximum number of documents of a single collection chunk in the archive.
	 * See {@link #setChunkMaxBytes(int)} — the first threshold reached triggers the chunk flush.
	 * @param value maximum number of documents per chunk (default {@link #DEFAULT_CHUNK_MAX_DOCUMENTS})
	 */
	public void setChunkMaxDocuments(final int value) {
		this.chunkMaxDocuments = value;
	}

	/**
	 * Set the maximum accumulated size (in bytes) of a single insertMany batch at restore.
	 * @param value maximum batch size in bytes (default {@link #DEFAULT_RESTORE_BATCH_MAX_BYTES})
	 */
	public void setRestoreBatchMaxBytes(final int value) {
		this.restoreBatchMaxBytes = value;
	}

	/**
	 * Set the maximum number of documents of a single insertMany batch at restore.
	 * The first threshold reached (bytes or documents) triggers the insert.
	 * @param value maximum number of documents per batch (default {@link #DEFAULT_RESTORE_BATCH_MAX_DOCUMENTS})
	 */
	public void setRestoreBatchMaxDocuments(final int value) {
		this.restoreBatchMaxDocuments = value;
	}

	/**
	 * Enable or disable the relaxed write concern during restore.
	 * When enabled (default), documents are inserted with {@code w:1, journal:false} and unordered
	 * bulk writes, which is significantly faster. Safe for a restore: the target collection must be
	 * empty beforehand, so a failed restore is simply re-run from scratch.
	 * @param value {@code true} to restore with relaxed write concern (default), {@code false} to
	 *              use the connection default write concern and ordered inserts
	 */
	public void setRestoreRelaxedWriteConcern(final boolean value) {
		this.restoreRelaxedWriteConcern = value;
	}

	/**
	 * Require the SHA-256 sidecar file at restore. By default an archive without sidecar (written by an
	 * older version) is restored with a warning; a sidecar that does not match always fails the restore.
	 * @param value {@code true} to refuse an archive without {@code .sha256} file
	 */
	public void setRequireChecksum(final boolean value) {
		this.requireChecksum = value;
	}

	/**
	 * Set the maximum declared size of a single archive entry accepted at restore. A larger entry aborts the
	 * restore (protection against an archive that would fill the disk).
	 * @param value maximum entry size in bytes (default {@link #DEFAULT_RESTORE_MAX_ENTRY_BYTES})
	 */
	public void setRestoreMaxEntryBytes(final long value) {
		this.restoreMaxEntryBytes = value;
	}

	/**
	 * Set the maximum total declared size of the entries of an archive accepted at restore (checked on each
	 * pass over the archive). A larger archive aborts the restore.
	 * @param value maximum total size in bytes (default {@link #DEFAULT_RESTORE_MAX_TOTAL_BYTES})
	 */
	public void setRestoreMaxTotalBytes(final long value) {
		this.restoreMaxTotalBytes = value;
	}

	/**
	 * Set the maximum length of one NDJSON document line at restore. A longer line aborts the restore
	 * (protection against a memory exhaustion).
	 * @param value maximum line length in characters (default {@link #DEFAULT_RESTORE_MAX_LINE_CHARS})
	 */
	public void setRestoreMaxLineChars(final int value) {
		this.restoreMaxLineChars = value;
	}

	/**
	 * Returns the path of the SHA-256 sidecar file of an archive ({@code <archive>.sha256}).
	 * @param archive the archive path
	 * @return the sidecar path, next to the archive
	 * @throws IllegalArgumentException if the path has no file name (a file system root)
	 */
	public static Path checksumPath(final Path archive) {
		final Path name = archive.getFileName();
		if (name == null) {
			throw new IllegalArgumentException("Not the path of an archive: " + archive);
		}
		return archive.resolveSibling(name.toString() + SUFFIX_SHA256);
	}

	/**
	 * Register model classes for backup. The MongoDB collection name is resolved from class annotations.
	 * A collection already registered stays registered once.
	 * @param classes the model classes to register
	 * @throws DataAccessException if a collection name cannot be resolved from the class annotations
	 */
	public void addClass(final Class<?>... classes) throws DataAccessException {
		for (final Class<?> clazz : classes) {
			this.collectionNames.add(AnnotationTools.getTableName(clazz, null));
		}
	}

	/**
	 * Register collection names for backup. A collection already registered stays registered once.
	 * @param names the MongoDB collection names to backup
	 */
	public void addCollection(final String... names) {
		for (final String name : names) {
			this.collectionNames.add(name);
		}
	}

	private static boolean isPosix(final Path path) {
		return path.getFileSystem().supportedFileAttributeViews().contains("posix");
	}

	/** Create an empty file readable and writable by its owner only (replacing any previous one). */
	private static void createPrivateFile(final Path path) throws IOException {
		Files.deleteIfExists(path);
		if (isPosix(path)) {
			Files.createFile(path, PosixFilePermissions.asFileAttribute(OWNER_FILE));
			return;
		}
		Files.createFile(path);
		final File file = path.toFile();
		if (!(file.setReadable(false, false) && file.setReadable(true, true) && file.setWritable(false, false)
				&& file.setWritable(true, true))) {
			LOGGER.warn("Cannot restrict the permissions of the backup file: {}", path);
		}
	}

	/** Create the store directory when missing, accessible by its owner only. */
	static void createPrivateDirectories(final Path dir) throws IOException {
		if (Files.isDirectory(dir)) {
			return;
		}
		if (isPosix(dir)) {
			Files.createDirectories(dir, PosixFilePermissions.asFileAttribute(OWNER_DIRECTORY));
		} else {
			Files.createDirectories(dir);
		}
	}

	/**
	 * Leave a directory made by hand to its owner alone: what it holds is in clear. Only for a directory of archives:
	 * a restore can run from the working directory of the application.
	 */
	static void restrictDirectory(final Path dir) {
		if (!isPosix(dir)) {
			return;
		}
		try {
			if (!OWNER_DIRECTORY.equals(Files.getPosixFilePermissions(dir))) {
				Files.setPosixFilePermissions(dir, OWNER_DIRECTORY);
			}
		} catch (final IOException ex) {
			LOGGER.warn("Cannot restrict the permissions of the backup folder: {}", dir);
		}
	}

	private static MessageDigest newSha256() throws IOException {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (final NoSuchAlgorithmException ex) {
			throw new IOException("SHA-256 not available", ex);
		}
	}

	private static String toHex(final byte[] digest) {
		return HexFormat.of().formatHex(digest);
	}

	/** Compute the SHA-256 of a file, as lower-case hexadecimal. */
	static String sha256Of(final Path file) throws IOException {
		final MessageDigest digest = newSha256();
		try (InputStream in = new DigestInputStream(Files.newInputStream(file), digest)) {
			in.transferTo(OutputStream.nullOutputStream());
		}
		return toHex(digest.digest());
	}

	/**
	 * Check an archive against its SHA-256 sidecar before restoring anything from it.
	 * @throws IOException if the sums differ, or if the sidecar is missing while {@link #setRequireChecksum} is set
	 */
	private void verifyChecksum(final Path archive) throws IOException {
		final Path sidecar = checksumPath(archive);
		if (!Files.exists(sidecar)) {
			if (this.requireChecksum) {
				throw new IOException("Missing checksum file " + sidecar.getFileName() + ": archive refused");
			}
			LOGGER.warn("No checksum file for {}: the integrity of the archive is not verified", archive.getFileName());
			return;
		}
		final String content = Files.readString(sidecar, StandardCharsets.US_ASCII).trim();
		final String expected = content.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
		final String actual = sha256Of(archive);
		if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
				actual.getBytes(StandardCharsets.US_ASCII))) {
			throw new IOException("Checksum mismatch for " + archive.getFileName() + ": archive refused");
		}
		LOGGER.info("Checksum verified for {}", archive.getFileName());
	}

	private TarArchiveOutputStream openTarGzOutputStream(final Path tarGzPath, final MessageDigest digest)
			throws IOException {
		createPrivateFile(tarGzPath);
		final OutputStream fos = new DigestOutputStream(
				Files.newOutputStream(tarGzPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
				digest);
		try {
			final GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
			final TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos);
			taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			// Allow entries bigger than 8GiB (media files) with POSIX extended headers
			taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			return taos;
		} catch (final IOException e) {
			fos.close();
			throw e;
		}
	}

	private TarArchiveInputStream openTarGzInputStream(final Path tarGzPath) throws IOException {
		final InputStream fis = Files.newInputStream(tarGzPath);
		try {
			final GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
			return new TarArchiveInputStream(gcis);
		} catch (final IOException e) {
			fis.close();
			throw e;
		}
	}

	private JsonWriterSettings createJsonWriterSettings() {
		return JsonWriterSettings.builder()//
				.outputMode(this.type == EngineBackupType.JSON_EXTENDED ? JsonMode.EXTENDED : JsonMode.RELAXED) //
				.build();
	}

	private String serializeDocument(final Document doc, final ObjectMapper mapper, final JsonWriterSettings settings)
			throws IOException {
		if (this.type == EngineBackupType.JSON_EXTERNAL) {
			return mapper.writeValueAsString(doc);
		}
		return doc.toJson(settings);
	}

	/**
	 * Serialize collections fully in memory. Only used by the snapshot API
	 * ({@link #snapshot()} / {@link #snapshotAll()}): the whole content is materialized as byte
	 * arrays, so it must not be used on large collections. The archive path streams chunk by chunk
	 * instead (see {@link #backupCollectionToStream}).
	 */
	private Map<String, byte[]> serializeCollections(final MongoDatabase db, final Collection<String> names)
			throws IOException {
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
		final JsonWriterSettings settings = createJsonWriterSettings();

		// Use LinkedHashMap to preserve insertion order
		final Map<String, byte[]> result = new LinkedHashMap<>();
		for (final String collectionName : names) {
			final ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
			final MongoCollection<Document> collection = db.getCollection(collectionName);
			int count = 0;
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jsonOut, StandardCharsets.UTF_8))) {
				for (final Document doc : collection.find().batchSize(CURSOR_BATCH_SIZE)) {
					count++;
					writer.write(serializeDocument(doc, mapper, settings));
					writer.newLine();
				}
				writer.flush();
			}

			final byte[] data = jsonOut.toByteArray();
			final String colNameFormatted = String.format("%-25s", collectionName);
			final String countFormatted = String.format("%5d", count);
			LOGGER.info("Backup collection:  [ END ] {} {} document(s), {} Bytes", colNameFormatted, countFormatted,
					data.length);
			result.put(collectionName, data);
		}
		return result;
	}

	/**
	 * Build the MongoDB filter selecting the documents to include in a delta backup:
	 * documents created or updated since the given date, plus documents carrying neither
	 * {@code createdAt} nor {@code updatedAt} (they cannot be dated, so they are always
	 * included — the delta restore upserts by {@code _id}, duplicates are impossible).
	 * <p>
	 * Limitation: a document without {@code updatedAt} that is modified in place is not
	 * detected; deletions are never detected (a delta restore never removes documents).
	 */
	private static Document buildDeltaFilter(final Date since) {
		return new Document("$or", List.of(//
				new Document("updatedAt", new Document("$gte", since)), //
				new Document("createdAt", new Document("$gte", since)), //
				new Document("updatedAt", new Document("$exists", false)).append("createdAt",
						new Document("$exists", false))));
	}

	private static String chunkEntryName(final String collectionName, final int chunkIndex) {
		if (chunkIndex == 0) {
			return collectionName + SUFFIX_JSON;
		}
		return collectionName + String.format("__%06d", chunkIndex) + SUFFIX_JSON;
	}

	/** Strip the chunk suffix ({@code __<digits>}) from an archive entry base name, if present. */
	static String baseCollectionName(final String entryBaseName) {
		return CHUNK_SUFFIX_PATTERN.matcher(entryBaseName).replaceFirst("");
	}

	private void writeChunkEntry(
			final TarArchiveOutputStream tarOut,
			final String collectionName,
			final int chunkIndex,
			final ByteArrayOutputStream chunkBuffer) throws IOException {
		final String entryName = chunkEntryName(collectionName, chunkIndex);
		final TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
		tarEntry.setSize(chunkBuffer.size());
		// The same documents give the same bytes, the same sum: storeDeltaIfChanged relies on it.
		tarEntry.setModTime(0L);
		tarOut.putArchiveEntry(tarEntry);
		chunkBuffer.writeTo(tarOut);
		tarOut.closeArchiveEntry();
		LOGGER.debug("Backup collection:  [CHUNK] {} {} Bytes", entryName, chunkBuffer.size());
	}

	/**
	 * Stream one collection into the archive, chunk by chunk. Memory usage is bounded by the chunk
	 * thresholds ({@link #setChunkMaxBytes(int)} / {@link #setChunkMaxDocuments(int)}) whatever the
	 * collection size is. The first chunk keeps the historical entry name {@code <collection>.json}
	 * so archives with collections below the thresholds are byte-compatible with the previous format.
	 */
	private void backupCollectionToStream(
			final MongoDatabase db,
			final String collectionName,
			final TarArchiveOutputStream tarOut,
			final ObjectMapper mapper,
			final JsonWriterSettings settings,
			final Document filter) throws IOException {
		final MongoCollection<Document> collection = db.getCollection(collectionName);
		final ByteArrayOutputStream chunkBuffer = new ByteArrayOutputStream();
		int chunkIndex = 0;
		int chunkDocumentCount = 0;
		long totalDocumentCount = 0;
		long totalBytes = 0;
		for (final Document doc : collection.find(filter != null ? filter : new Document())
				.batchSize(CURSOR_BATCH_SIZE)) {
			chunkBuffer.write(serializeDocument(doc, mapper, settings).getBytes(StandardCharsets.UTF_8));
			chunkBuffer.write('\n');
			chunkDocumentCount++;
			totalDocumentCount++;
			if (chunkBuffer.size() >= this.chunkMaxBytes || chunkDocumentCount >= this.chunkMaxDocuments) {
				totalBytes += chunkBuffer.size();
				writeChunkEntry(tarOut, collectionName, chunkIndex, chunkBuffer);
				chunkBuffer.reset();
				chunkDocumentCount = 0;
				chunkIndex++;
			}
		}
		// Always write the first chunk (even empty, to keep the historical layout);
		// write the last one only if it contains data.
		if (chunkIndex == 0 || chunkBuffer.size() > 0) {
			totalBytes += chunkBuffer.size();
			writeChunkEntry(tarOut, collectionName, chunkIndex, chunkBuffer);
			chunkIndex++;
		}
		final String colNameFormatted = String.format("%-25s", collectionName);
		final String countFormatted = String.format("%5d", totalDocumentCount);
		LOGGER.info("Backup collection:  [ END ] {} {} document(s), {} Bytes, {} chunk(s)", colNameFormatted,
				countFormatted, totalBytes, chunkIndex);
	}

	private void backupCollectionListToStream(
			final MongoDatabase db,
			final Collection<String> names,
			final TarArchiveOutputStream tarOut,
			final Document filter) throws IOException {
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
		final JsonWriterSettings settings = createJsonWriterSettings();
		for (final String collectionName : names) {
			backupCollectionToStream(db, collectionName, tarOut, mapper, settings, filter);
		}
	}

	private void backupCollectionsToStream(
			final DBAccessMongo dbMongo,
			final TarArchiveOutputStream tarOut,
			final Document filter) throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		backupCollectionListToStream(db, this.collectionNames, tarOut, filter);
	}

	private void backupAllCollectionsToStream(
			final DBAccessMongo dbMongo,
			final TarArchiveOutputStream tarOut,
			final Document filter) throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		final List<String> allNames = db.listCollectionNames().into(new ArrayList<>());
		// Filter out system collections
		final List<String> filtered = allNames.stream().filter(name -> !name.startsWith("system.")).sorted().toList();
		LOGGER.info("Discovered {} collections for backup (filtered from {} total)", filtered.size(), allNames.size());
		backupCollectionListToStream(db, filtered, tarOut, filter);
	}

	private MongoCollection<Document> collectionForRestore(final MongoDatabase db, final String collectionName) {
		final MongoCollection<Document> collection = db.getCollection(collectionName);
		if (this.restoreRelaxedWriteConcern) {
			return collection.withWriteConcern(WriteConcern.W1.withJournal(false));
		}
		return collection;
	}

	/**
	 * Write all NDJSON lines from the reader into the collection, by batches bounded both in
	 * bytes and in number of documents. In insert mode (full restore) each document is inserted;
	 * in upsert mode (delta restore) each document replaces the existing one with the same
	 * {@code _id}, or is inserted when absent. Writes are unordered when the relaxed write
	 * concern is enabled (the server can parallelize them).
	 * @return the number of written documents
	 */
	private long writeDocumentsFromReader(
			final MongoCollection<Document> collection,
			final BufferedReader reader,
			final boolean upsertMode) throws IOException {
		final BulkWriteOptions bulkOptions = new BulkWriteOptions().ordered(!this.restoreRelaxedWriteConcern);
		final ReplaceOptions replaceOptions = new ReplaceOptions().upsert(true);
		final List<WriteModel<Document>> buffer = new ArrayList<>();
		long batchBytes = 0;
		long count = 0;
		String line;
		while ((line = readBoundedLine(reader, this.restoreMaxLineChars)) != null) {
			if (line.isBlank()) {
				continue;
			}
			final Document doc = Document.parse(line);
			buffer.add(upsertMode ? new ReplaceOneModel<>(new Document("_id", doc.get("_id")), doc, replaceOptions)
					: new InsertOneModel<>(doc));
			batchBytes += line.length();
			count++;
			if (buffer.size() >= this.restoreBatchMaxDocuments || batchBytes >= this.restoreBatchMaxBytes) {
				collection.bulkWrite(buffer, bulkOptions);
				buffer.clear();
				batchBytes = 0;
			}
		}
		if (!buffer.isEmpty()) {
			collection.bulkWrite(buffer, bulkOptions);
		}
		return count;
	}

	/**
	 * Read one line (without its terminator) with a bounded length.
	 *
	 * <p>The reader is consumed by blocks of characters: each block is marked, scanned
	 * for the end of line, then the reader is reset and moved just after the terminator.
	 * @return the line, or {@code null} at the end of the stream
	 * @throws IOException if the line exceeds {@code maxChars}
	 */
	static String readBoundedLine(final BufferedReader reader, final int maxChars) throws IOException {
		final char[] chunk = new char[READ_LINE_CHUNK];
		final StringBuilder out = new StringBuilder();
		boolean any = false;
		while (true) {
			reader.mark(chunk.length);
			final int read = reader.read(chunk, 0, chunk.length);
			if (read == -1) {
				break;
			}
			any = true;
			int end = 0;
			while (end < read && chunk[end] != '\n') {
				end++;
			}
			if (out.length() + end > maxChars) {
				throw new IOException("Archive line longer than " + maxChars + " characters: restore aborted");
			}
			out.append(chunk, 0, end);
			if (end < read) {
				// End of line found: give back what follows the terminator.
				reader.reset();
				long toSkip = end + 1L;
				while (toSkip > 0) {
					final long skipped = reader.skip(toSkip);
					if (skipped <= 0) {
						throw new IOException("Can not move after the end of line in the archive");
					}
					toSkip -= skipped;
				}
				break;
			}
		}
		if (!any) {
			return null;
		}
		final int last = out.length() - 1;
		if (last >= 0 && out.charAt(last) == '\r') {
			out.setLength(last);
		}
		return out.toString();
	}

	/**
	 * Check the declared size of an archive entry against the per-entry and total bounds.
	 * @param entry the entry about to be read (or skipped)
	 * @param totalBefore the total declared size of the entries met before it on this pass
	 * @return the new total
	 * @throws IOException if a bound is exceeded
	 */
	private long checkEntrySize(final TarArchiveEntry entry, final long totalBefore) throws IOException {
		final long size = Math.max(0L, entry.getSize());
		if (size > this.restoreMaxEntryBytes) {
			throw new IOException("Archive entry '" + entry.getName() + "' is too large (" + size + " > "
					+ this.restoreMaxEntryBytes + " bytes): restore aborted");
		}
		final long total = totalBefore + size;
		if (total > this.restoreMaxTotalBytes) {
			throw new IOException(
					"Archive too large (more than " + this.restoreMaxTotalBytes + " bytes): restore aborted");
		}
		return total;
	}

	/**
	 * Check a collection name read from an archive: safe characters only, and never a {@code system.*}
	 * collection ({@code system.js} holds functions run by {@code $where}).
	 * @param name the collection name
	 * @throws IOException if the name is refused
	 */
	static void checkRestoredCollectionName(final String name) throws IOException {
		if (name == null || !SAFE_COLLECTION_NAME.matcher(name).matches() || name.startsWith("system.")) {
			throw new IOException("Archive collection name refused: '" + name + "' (restore aborted)");
		}
	}

	/**
	 * Check that a restore would accept every registered collection, while it is still time to fix the configuration.
	 * @throws IOException if a name is refused
	 */
	void checkRegisteredCollectionNames() throws IOException {
		for (final String name : this.collectionNames) {
			checkRestoredCollectionName(name);
			// Its entries would be read back as the chunks of another collection.
			final String restoredAs = baseCollectionName(name);
			if (!name.equals(restoredAs)) {
				throw new IOException(
						"Collection name refused: '" + name + "' would be restored into '" + restoredAs + "'");
			}
		}
	}

	private void restoreStreamToCollections(
			final DBAccessMongo dbMongo,
			final TarArchiveInputStream tarIn,
			final String collectionName,
			final boolean upsertMode) throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		if (this.type == EngineBackupType.JSON_EXTERNAL) {
			LOGGER.error("Try to retrieve data with generic JSON engine, you will lose real DB information");
		}
		// Cumulative document count per collection (a collection can be split in multiple chunks)
		final Map<String, Long> restoredCounts = new LinkedHashMap<>();
		long totalBytes = 0L;
		TarArchiveEntry entry;
		while ((entry = tarIn.getNextEntry()) != null) {
			totalBytes = checkEntrySize(entry, totalBytes);
			if (entry.isDirectory() || !entry.getName().endsWith(SUFFIX_JSON)) {
				continue;
			}
			// Media files live under "data/"; collection entries are at the archive root
			if (entry.getName().contains("/")) {
				continue;
			}

			final String entryBaseName = entry.getName().substring(0, entry.getName().length() - SUFFIX_JSON.length());
			final String colName = baseCollectionName(entryBaseName);
			checkRestoredCollectionName(colName);
			if (collectionName != null && !collectionName.equals(colName)) {
				LOGGER.info("Skip collection: {} (filtered)", entryBaseName);
				continue;
			}
			final MongoCollection<Document> collection = collectionForRestore(db, colName);
			if (!restoredCounts.containsKey(colName)) {
				LOGGER.info("Restore collection: [START] {}", colName);
				// First chunk of this collection: check that the target is empty.
				// A delta restore upserts on top of existing data, so no check in that mode.
				if (!upsertMode) {
					try (var cursor = collection.find().limit(1).iterator()) {
						if (cursor.hasNext()) {
							throw new IOException(
									"Collection: '" + colName + "' is not empty ==> can not insert object inside");
						}
					}
				}
				restoredCounts.put(colName, 0L);
			}
			final BufferedReader reader = new BufferedReader(new InputStreamReader(tarIn, StandardCharsets.UTF_8));
			final long count = writeDocumentsFromReader(collection, reader, upsertMode);
			restoredCounts.merge(colName, count, Long::sum);
		}
		for (final Map.Entry<String, Long> restored : restoredCounts.entrySet()) {
			final String colNameFormatted = String.format("%-25s", restored.getKey());
			final String countFormatted = String.format("%5d", restored.getValue());
			LOGGER.info("Restore collection: [ END ] {} {} document(s)", colNameFormatted, countFormatted);
		}
	}

	private void restoreCollectionsFromMap(final DBAccessMongo dbMongo, final Map<String, byte[]> data)
			throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		if (this.type == EngineBackupType.JSON_EXTERNAL) {
			LOGGER.error("Try to retrieve data with generic JSON engine, you will lose real DB information");
		}
		for (final Map.Entry<String, byte[]> entry : data.entrySet()) {
			final String colName = entry.getKey();
			LOGGER.info("Restore collection: [START] {}", colName);
			final MongoCollection<Document> collection = collectionForRestore(db, colName);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ByteArrayInputStream(entry.getValue()), StandardCharsets.UTF_8))) {
				final long count = writeDocumentsFromReader(collection, reader, false);
				final String colNameFormatted = String.format("%-25s", colName);
				final String countFormatted = String.format("%5d", count);
				LOGGER.info("Restore collection: [ END ] {} {} document(s)", colNameFormatted, countFormatted);
			}
		}
	}

	private void backupCollectionsToStream(final TarArchiveOutputStream tarOut, final Document filter)
			throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			backupCollectionsToStream(dbMongo, tarOut, filter);
		}
	}

	private void backupAllCollectionsToStream(final TarArchiveOutputStream tarOut, final Document filter)
			throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			backupAllCollectionsToStream(dbMongo, tarOut, filter);
		}
	}

	private void restoreStreamToCollections(
			final TarArchiveInputStream tarIn,
			final String collectionName,
			final boolean upsertMode) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			restoreStreamToCollections(dbMongo, tarIn, collectionName, upsertMode);
		}
	}

	private void backupDataToStream(final TarArchiveOutputStream tarOut, final Date modifiedSince)
			throws IOException, DataAccessException {
		final String mediaFolder = ConfigBaseVariable.getMediaDataFolder();
		// Create a File to the specific data of medias:
		final File mediaDirFile = new File(mediaFolder);

		if (mediaDirFile.exists() && mediaDirFile.isDirectory() && mediaDirFile.canRead()) {
			// recursive add files in the tar:
			addDirectoryToTar(tarOut, mediaDirFile, "data/", modifiedSince);
		}
	}

	private void addDirectoryToTar(
			final TarArchiveOutputStream tarOut,
			final File sourceDir,
			final String basePath,
			final Date modifiedSince) throws IOException {
		if (!sourceDir.canRead()) {
			LOGGER.warn("The folder is not readable ==> ignored: {}", sourceDir.getAbsolutePath());
			return;
		}
		final File[] files = sourceDir.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (!file.canRead()) {
					LOGGER.warn("The file is not readable ==> ignored: {}", file.getAbsolutePath());
					continue;
				}
				// Ignore the folder history_restore_* at the first data level
				if (file.isDirectory() && basePath.equals("data/") && file.getName().startsWith("history_restore")) {
					LOGGER.debug("Folder history_restore ignored: {}", file.getName());
					continue;
				}
				final String entryName = basePath + file.getName();
				try {
					if (file.isDirectory()) {
						final TarArchiveEntry dirEntry = new TarArchiveEntry(entryName + "/");
						tarOut.putArchiveEntry(dirEntry);
						tarOut.closeArchiveEntry();

						addDirectoryToTar(tarOut, file, entryName + "/", modifiedSince);
					} else {
						if (!file.exists()) {
							LOGGER.warn("Invalid file, ignored: {}", file.getAbsolutePath());
							continue;
						}
						// Delta backup: only include files modified since the reference date
						if (modifiedSince != null && file.lastModified() < modifiedSince.getTime()) {
							continue;
						}

						final TarArchiveEntry fileEntry = new TarArchiveEntry(file, entryName);
						tarOut.putArchiveEntry(fileEntry);

						try (FileInputStream fis = new FileInputStream(file)) {
							final byte[] buffer = new byte[8192];
							int bytesRead;
							while ((bytesRead = fis.read(buffer)) != -1) {
								tarOut.write(buffer, 0, bytesRead);
							}
							tarOut.closeArchiveEntry();
						} catch (final IOException e) {
							LOGGER.error("Fail to read the file, ignored: {} - {}", file.getAbsolutePath(),
									e.getMessage());
							tarOut.closeArchiveEntry();
						}
					}
				} catch (final IOException e) {
					LOGGER.error("Fail to add data in the archive, element ignored: {} - {}", file.getAbsolutePath(),
							e.getMessage());
				}
			}
		} else {
			LOGGER.warn("Fail to list the folder: {}", sourceDir.getAbsolutePath());
		}
	}

	private void restoreStreamToData(final TarArchiveInputStream tarIn, final boolean moveOldData)
			throws IOException, DataAccessException {
		final String mediaFolder = ConfigBaseVariable.getMediaDataFolder();
		final File mediaDirFile = new File(mediaFolder);

		// First step: move previous data if requested
		if (moveOldData && mediaDirFile.exists() && mediaDirFile.isDirectory()) {
			moveExistingDataToHistoryRestore(mediaDirFile);
			deleteEmptyFolder(mediaDirFile, false);
		}
		// extract data from tar
		extractTarToMediaFolder(tarIn, mediaDirFile);
	}

	private boolean deleteEmptyFolder(final File directory, final boolean deletecurrentFolder) throws IOException {
		final File[] files = directory.listFiles();
		boolean detectFile = false;
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					if (deleteEmptyFolder(file, true)) {
						detectFile = true;
					}
				} else {
					detectFile = true;
				}
			}
		}
		if (detectFile) {
			return true;
		}
		if (deletecurrentFolder && !directory.delete()) {
			LOGGER.warn("Fail to remove the folder: {}", directory.getAbsolutePath());
		}
		return false;
	}

	private void moveExistingDataToHistoryRestore(final File mediaDirFile) throws IOException {
		// create timestamp ISO8601 compatible
		final String timestamp = DateTools.serializeMilliWithOriginalTimeZone(new Date());

		final Path mediaPath = mediaDirFile.toPath();
		final Path historyRestorePath = mediaPath.resolve("history_restore").resolve(timestamp);

		Files.createDirectories(historyRestorePath);
		LOGGER.info("Move previous data to: {}", historyRestorePath);

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(mediaPath)) {
			for (final Path entry : stream) {
				final Path name = entry.getFileName();
				if (name == null) {
					continue;
				}
				final String fileName = name.toString();

				// Ignore the folder history_restore
				if (Files.isDirectory(entry) && fileName.equals("history_restore")) {
					LOGGER.info("skip folder history_restore: {}", fileName);
					continue;
				}

				final Path destination = historyRestorePath.resolve(fileName);
				try {
					Files.move(entry, destination, StandardCopyOption.REPLACE_EXISTING);
					LOGGER.debug("Moved: {} to {}", fileName, destination);
				} catch (final IOException e) {
					LOGGER.error("Fail to move {} : {}", entry, e.getMessage());
					throw e;
				}
			}
		}
	}

	/**
	 * Tell whether a path, once its existing part is resolved (symbolic links followed), stays inside the
	 * media folder.
	 * @param target the normalized absolute target path
	 * @param baseReal the real path of the media folder
	 * @return {@code false} if the deepest existing ancestor (or the target itself) resolves elsewhere
	 */
	private static boolean resolvesInside(final Path target, final Path baseReal) {
		Path existing = target;
		while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
			existing = existing.getParent();
		}
		if (existing == null) {
			return false;
		}
		try {
			return existing.toRealPath().startsWith(baseReal);
		} catch (final IOException ex) {
			// Broken link or unreadable: refused.
			return false;
		}
	}

	private void extractTarToMediaFolder(final TarArchiveInputStream tarIn, final File mediaDirFile)
			throws IOException {
		LOGGER.info("Extract data to: {}", mediaDirFile.getAbsolutePath());
		final Path base = mediaDirFile.toPath().toAbsolutePath().normalize();
		Files.createDirectories(base);
		final Path baseReal = base.toRealPath();
		long totalBytes = 0L;
		TarArchiveEntry entry;
		while ((entry = tarIn.getNextEntry()) != null) {
			totalBytes = checkEntrySize(entry, totalBytes);
			// Remove base folder in the backup "data/":
			String entryName = entry.getName();
			if (entryName.startsWith("data/")) {
				entryName = entryName.substring(5);
			} else {
				// skip files not in this folder.
				continue;
			}
			// Ignore empty
			if (entryName.isEmpty()) {
				continue;
			}
			// A link entry could point anywhere: never created (a backup made by the engine has none).
			if (entry.isSymbolicLink() || entry.isLink()) {
				LOGGER.warn("Input TAR link entry refused: {}", entry.getName());
				continue;
			}
			if (!entry.isDirectory() && !entry.isFile()) {
				LOGGER.warn("Input TAR special entry refused: {}", entry.getName());
				continue;
			}
			// Path traversal: compare paths, not strings ("/data/media-other" starts with "/data/media").
			final Path target = base.resolve(entryName).normalize();
			if (!target.startsWith(base) || target.equals(base) || entryName.indexOf('\0') >= 0) {
				LOGGER.warn("Input TAR too dangerous ignored: {}", entry.getName());
				continue;
			}
			// An existing symbolic link (the target or one of its parents) must not redirect the write.
			if (!resolvesInside(target, baseReal)) {
				LOGGER.warn("Input TAR entry resolves outside the media folder, ignored: {}", entry.getName());
				continue;
			}
			if (entry.isDirectory()) {
				Files.createDirectories(target);
				continue;
			}
			final Path targetParent = target.getParent();
			if (targetParent != null) {
				Files.createDirectories(targetParent);
			}
			// Extract the file:
			try (OutputStream fos = Files.newOutputStream(target, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
				final long copied = tarIn.transferTo(fos);
				LOGGER.debug("File extracted: {} size: {}", entryName, copied);
			} catch (final IOException e) {
				LOGGER.error("Fail to extract the file {} : {}", entryName, e.getMessage());
				// continue with next file...
			}
		}
	}

	@FunctionalInterface
	private interface BackupCollectionStrategy {
		void backup(TarArchiveOutputStream tarOut) throws IOException, DataAccessException;
	}

	private Date executeStoreInternal(
			final String sequence,
			final BackupCollectionStrategy strategy,
			final Date mediaModifiedSince,
			final boolean onlyIfChanged) throws IOException, DataAccessException {
		final Date now = Date.from(Instant.now());
		final String fileName = archiveFileName(sequence);
		// Ensure the store directory exists (owner only when the engine creates it)
		createPrivateDirectories(this.pathStore);
		// Create a hidden file for the temporary generation
		final Path outputFileTmp = this.pathStore.resolve("." + fileName + SUFFIX_TMP);
		final Path checksumTmp = this.pathStore.resolve("." + fileName + SUFFIX_SHA256 + SUFFIX_TMP);
		LOGGER.debug("Store in path: {} [BEGIN]", outputFileTmp);
		boolean stored = false;
		try {
			final MessageDigest digest = newSha256();
			try (TarArchiveOutputStream tarOut = openTarGzOutputStream(outputFileTmp, digest)) {
				strategy.backup(tarOut);
				if (this.enableStoreOrRestoreData) {
					backupDataToStream(tarOut, mediaModifiedSince);
				}
			}
			final String sum = toHex(digest.digest());
			if (onlyIfChanged && isLatestDelta(sum)) {
				LOGGER.info("Backup delta identical to the previous one, not kept: {}", fileName);
				return null;
			}
			final Path outputFileFinal = this.pathStore.resolve(fileName);
			// The sum (sha256sum format) is moved first: a new archive never sits next to an old sum, and an old
			// archive next to the new sum fails the check (safe side).
			createPrivateFile(checksumTmp);
			Files.writeString(checksumTmp, sum + "  " + fileName + "\n", StandardCharsets.US_ASCII);
			Files.move(checksumTmp, checksumPath(outputFileFinal), StandardCopyOption.REPLACE_EXISTING);
			Files.move(outputFileTmp, outputFileFinal, StandardCopyOption.REPLACE_EXISTING);
			stored = true;
			LOGGER.info("Backup done in file: {}", outputFileFinal);
			LOGGER.debug("Store in path: {} [ END ]", outputFileFinal);
		} finally {
			// Each attempt has its own name: a partial dump in clear would stay, unseen by the list and by clean().
			if (!stored) {
				deleteTemporaryFile(outputFileTmp);
				deleteTemporaryFile(checksumTmp);
			}
		}
		return now;
	}

	/**
	 * Compare a sum with the file of the delta a restore would apply, recomputed: a damaged archive does not stop the next
	 * one, and its {@code .sha256} is not read. Any doubt keeps the new delta.
	 */
	private boolean isLatestDelta(final String sum) {
		try {
			final List<Path> chain = getLatestRestoreChain();
			return chain.size() > 1 && sum.equals(sha256Of(chain.get(chain.size() - 1)));
		} catch (final IOException ex) {
			LOGGER.warn("Cannot read the previous delta, the new one is kept: {}", ex.getMessage());
			return false;
		}
	}

	/**
	 * Delete what a store killed with the process left in the store directory: a partial dump in clear, which each
	 * attempt writes under its own name. Only the regular files named exactly as this engine names them go, with a
	 * sequence holding a date: another base sharing the directory, even with a name starting the same way, keeps its
	 * files. Call it before any store of this process: it would delete the file of a store in progress.
	 * @throws IOException if the store directory can not be read
	 */
	void deleteAbandonedTemporaryFiles() throws IOException {
		final Pattern pattern = Pattern
				.compile(Pattern.quote("." + this.baseName + "_") + "(.+)" + Pattern.quote(SUFFIX_TAR_GZ) + "(?:"
						+ Pattern.quote(SUFFIX_SHA256) + ")?" + Pattern.quote(SUFFIX_TMP));
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.pathStore)) {
			for (final Path file : stream) {
				final Path name = file.getFileName();
				if (name == null) {
					continue;
				}
				final Matcher matcher = pattern.matcher(name.toString());
				if (matcher.matches() && parseDateFromSequence(matcher.group(1)) != null
						&& Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
					LOGGER.warn("Delete the temporary file of an interrupted backup: {}", name);
					deleteTemporaryFile(file);
				}
			}
		} catch (final DirectoryIteratorException ex) {
			throw ex.getCause();
		}
	}

	/** Delete a file of a failed store. Never throws: the failure of the store is the one to report. */
	private static void deleteTemporaryFile(final Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (final IOException ex) {
			LOGGER.error("Cannot delete the temporary file of a failed backup: {}", file, ex);
		}
	}

	/**
	 * Build the archive file name of a sequence. The sequence becomes part of a path: a separator, a
	 * {@code ..} or a NUL is refused, so that it can never leave the store directory.
	 */
	private String archiveFileName(final String sequence) {
		if (sequence != null && (sequence.contains("/") || sequence.contains("\\") || sequence.contains("..")
				|| sequence.indexOf('\0') >= 0)) {
			throw new IllegalArgumentException("Invalid backup sequence: '" + sequence + "'");
		}
		return this.baseName + (sequence != null ? "_" + sequence : "") + SUFFIX_TAR_GZ;
	}

	private void executeRestore(final String sequence) throws IOException, DataAccessException {
		final String fileName = archiveFileName(sequence);
		final Path restoreFileName = this.pathStore.resolve(fileName);
		restoreFile(restoreFileName, null);
	}

	/**
	 * Backup registered collections to a tar.gz archive.
	 * The archive is written to {@code pathStore/baseName_sequence.tar.gz}.
	 * Only collections previously registered via {@link #addClass} or {@link #addCollection} are included.
	 * @param sequence the sequence identifier appended to the backup filename (e.g. {@code "2025-05-16"})
	 * @return the date at which the backup started
	 * @throws IOException if the archive cannot be written
	 * @throws DataAccessException if the database connection fails
	 */
	public Date store(final String sequence) throws IOException, DataAccessException {
		return executeStoreInternal(sequence, tarOut -> backupCollectionsToStream(tarOut, null), null, false);
	}

	/**
	 * Backup only the documents of the registered collections that changed since the given date
	 * (delta backup). A document is included when its {@code updatedAt} or {@code createdAt} is
	 * after {@code since}; documents carrying neither field are always included. Media files are
	 * filtered on their modification time. The archive layout is identical to a full backup and
	 * must be restored with {@link #restoreDeltaFile} on top of the full backup it is based on.
	 * <p>
	 * The {@code "_delta"} filename suffix — which retention and {@link #getLastFullBackupDate()}
	 * rely on to tell delta archives apart from full ones — is appended to the sequence
	 * automatically when missing.
	 * @param sequence the sequence identifier appended to the backup filename (required)
	 * @param since only documents/files modified at or after this date are included
	 * @return the date at which the backup started
	 * @throws IOException if the archive cannot be written
	 * @throws DataAccessException if the database connection fails
	 */
	public Date storeDelta(final String sequence, final Date since) throws IOException, DataAccessException {
		return storeDelta(sequence, since, false);
	}

	private Date storeDelta(final String sequence, final Date since, final boolean onlyIfChanged)
			throws IOException, DataAccessException {
		final Document filter = buildDeltaFilter(since);
		return executeStoreInternal(ensureDeltaSequence(sequence), tarOut -> backupCollectionsToStream(tarOut, filter),
				since, onlyIfChanged);
	}

	/**
	 * Same as {@link #storeDelta(String, Date)}, but keep the archive only if it differs from the delta a restore would
	 * apply today (see {@link #getLatestRestoreChain()}). The deltas of a same full are cumulative: when the content is
	 * the same as the previous one, a new file would restore nothing more.
	 * <p>
	 * The comparison is the one of the whole content, so that it also sees what no date tells: the {@code counters},
	 * the documents without {@code updatedAt}, and a document deleted since the previous delta. It needs the archive,
	 * which is written and then dropped, so its cost is the one of a delta. It rereads the file of the previous delta, and
	 * survives a restart. The first delta after a full has nothing to be compared with and is always kept.
	 * Only meaningful with {@code setEnableStoreOrRestoreData(false)}: the folders of the media have the current date in
	 * the archive, which then always differs, and is kept.
	 * @param sequence the sequence identifier appended to the backup filename (required)
	 * @param since only documents modified at or after this date are included
	 * @return the date at which the backup started, {@code null} when nothing was kept
	 * @throws IOException if the archive cannot be written
	 * @throws DataAccessException if the database connection fails
	 */
	public Date storeDeltaIfChanged(final String sequence, final Date since) throws IOException, DataAccessException {
		return storeDelta(sequence, since, true);
	}

	/**
	 * Guarantee the {@code "_delta"} marker on a delta sequence: the filename suffix is what
	 * classifies the archive (retention, restore chain), so the engine enforces it instead of
	 * trusting every caller to follow the convention.
	 */
	private static String ensureDeltaSequence(final String sequence) {
		if (sequence == null || sequence.isEmpty()) {
			throw new IllegalArgumentException(
					"A delta backup requires a sequence identifier (the filename suffix drives retention and restore-chain resolution)");
		}
		return sequence.endsWith(SUFFIX_DELTA) ? sequence : sequence + SUFFIX_DELTA;
	}

	/**
	 * Backup all collections discovered in the database, excluding {@code system.*} collections.
	 * This method does not require prior {@link #addClass}/{@link #addCollection} calls.
	 * @param sequence the sequence identifier (e.g. {@code "2025-06-20"})
	 * @return the date at which the backup started
	 * @throws IOException if the archive cannot be written
	 * @throws DataAccessException if the database connection fails
	 */
	public Date storeAll(final String sequence) throws IOException, DataAccessException {
		return executeStoreInternal(sequence, tarOut -> backupAllCollectionsToStream(tarOut, null), null, false);
	}

	/**
	 * Delta variant of {@link #storeAll(String)}: backup the documents of all discovered
	 * collections (excluding {@code system.*}) that changed since the given date.
	 * See {@link #storeDelta(String, Date)} for the selection rules; the {@code "_delta"}
	 * filename suffix is appended automatically when missing.
	 * @param sequence the sequence identifier appended to the backup filename (required)
	 * @param since only documents/files modified at or after this date are included
	 * @return the date at which the backup started
	 * @throws IOException if the archive cannot be written
	 * @throws DataAccessException if the database connection fails
	 */
	public Date storeAllDelta(final String sequence, final Date since) throws IOException, DataAccessException {
		final Document filter = buildDeltaFilter(since);
		return executeStoreInternal(ensureDeltaSequence(sequence),
				tarOut -> backupAllCollectionsToStream(tarOut, filter), since, false);
	}

	/**
	 * Restore collections (and optionally media data) from an archive file.
	 * <p>
	 * If {@code collectionName} is {@code null}, all collections in the archive are restored and the
	 * target database must be empty. If a specific collection name is given, only that collection is
	 * restored and it must be empty.
	 * @param restoreFileName path to the tar.gz archive to restore from
	 * @param collectionName name of a single collection to restore, or {@code null} to restore all
	 * @return {@code true} if the restore succeeded, {@code false} if the database was not empty
	 * @throws IOException if the archive cannot be read or a collection is not empty
	 * @throws DataAccessException if the database connection fails
	 */
	public boolean restoreFile(final Path restoreFileName, final String collectionName)
			throws IOException, DataAccessException {
		LOGGER.info("Restore DB: [START] BD: '{}' from file: '{}'", this.baseName, restoreFileName);
		// check only the existence if not try to restore only a part of the BD
		if (collectionName == null && !DataAccess.listCollections(this.baseName).isEmpty()) {
			LOGGER.info("Restore DB: [ END ]");
			LOGGER.error("Can not restore, the DB {} already exist", this.baseName);
			return false;
		}
		verifyChecksum(restoreFileName);
		try (TarArchiveInputStream tarIn = openTarGzInputStream(restoreFileName)) {
			restoreStreamToCollections(tarIn, collectionName, false);
		}
		// Need to open the stream 2 time due to the fact it is not possible to reset marker position.
		if (this.enableStoreOrRestoreData) {
			try (TarArchiveInputStream tarIn = openTarGzInputStream(restoreFileName)) {
				restoreStreamToData(tarIn, true);
			}
		}
		LOGGER.info("Restore DB: [ END ]");
		return true;
	}

	/**
	 * Apply a delta archive on top of the current database state (typically right after restoring
	 * the full backup the delta is based on). Documents are upserted by {@code _id}: existing
	 * documents are replaced, missing ones are inserted, and nothing is ever deleted. Media files
	 * present in the delta are extracted over the existing ones (previous data is NOT moved away).
	 * <p>
	 * <b>Warning:</b> no consistency check is possible between the delta and the current database
	 * state — applying a delta on a database that is not the restored base full it was built on
	 * silently replaces newer documents with the archived versions. Prefer {@link #restoreLatest()}
	 * which always pairs a full with its own delta.
	 * @param deltaFileName path to the delta tar.gz archive to apply
	 * @throws IOException if the archive cannot be read
	 * @throws DataAccessException if the database connection fails
	 */
	public void restoreDeltaFile(final Path deltaFileName) throws IOException, DataAccessException {
		LOGGER.info("Restore delta: [START] BD: '{}' from file: '{}'", this.baseName, deltaFileName);
		LOGGER.warn("Applying a delta upserts documents over the CURRENT database state without any "
				+ "consistency check — make sure the base full backup of this delta was restored just before.");
		verifyChecksum(deltaFileName);
		try (TarArchiveInputStream tarIn = openTarGzInputStream(deltaFileName)) {
			restoreStreamToCollections(tarIn, null, true);
		}
		if (this.enableStoreOrRestoreData) {
			try (TarArchiveInputStream tarIn = openTarGzInputStream(deltaFileName)) {
				restoreStreamToData(tarIn, false);
			}
		}
		LOGGER.info("Restore delta: [ END ]");
	}

	/**
	 * Restore all collections and media data from the archive identified by the given sequence.
	 * The archive filename is resolved as {@code baseName_sequence.tar.gz} in the store directory.
	 * The target database must be empty.
	 * @param sequence the sequence identifier of the archive to restore
	 * @throws IOException if the archive cannot be read or a collection is not empty
	 * @throws DataAccessException if the database connection fails
	 */
	public void restore(final String sequence) throws IOException, DataAccessException {
		executeRestore(sequence);
	}

	/**
	 * Capture an in-memory snapshot of registered collections (those added via {@link #addClass}/{@link #addCollection}).
	 * The returned snapshot can later be restored with {@link #restore(BackupSnapshot)}.
	 * <p>
	 * <b>Warning:</b> the whole content of the collections is materialized in memory (test / small
	 * dataset facility). For large databases, use the streamed archive API ({@link #store} /
	 * {@link #storeAll}) instead.
	 * @return an immutable snapshot of the registered collections
	 * @throws IOException if serialization fails
	 * @throws DataAccessException if the database connection fails
	 */
	public BackupSnapshot snapshot() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			final MongoDatabase db = dbMongo.getInterface().getDatabase();
			return new BackupSnapshot(serializeCollections(db, this.collectionNames));
		}
	}

	/**
	 * Capture an in-memory snapshot of all collections discovered in the database,
	 * excluding {@code system.*} collections.
	 * This method does not require prior {@link #addClass}/{@link #addCollection} calls.
	 * <p>
	 * <b>Warning:</b> the whole content of the collections is materialized in memory (test / small
	 * dataset facility). For large databases, use the streamed archive API ({@link #store} /
	 * {@link #storeAll}) instead.
	 * @return an immutable snapshot of all user collections
	 * @throws IOException if serialization fails
	 * @throws DataAccessException if the database connection fails
	 */
	public BackupSnapshot snapshotAll() throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			final MongoDatabase db = dbMongo.getInterface().getDatabase();
			final List<String> allNames = db.listCollectionNames().into(new ArrayList<>());
			final List<String> filtered = allNames.stream().filter(name -> !name.startsWith("system.")).sorted()
					.toList();
			LOGGER.info("Discovered {} collections for snapshot (filtered from {} total)", filtered.size(),
					allNames.size());
			return new BackupSnapshot(serializeCollections(db, filtered));
		}
	}

	/**
	 * Restore database state from an in-memory snapshot.
	 * Each collection present in the snapshot is dropped and re-populated. Collections not present
	 * in the snapshot are left untouched.
	 * @param snapshot the snapshot to restore from
	 * @throws IOException if deserialization fails
	 * @throws DataAccessException if the database connection fails
	 */
	public void restore(final BackupSnapshot snapshot) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo dbMongo = ctx.get();
			final MongoDatabase db = dbMongo.getInterface().getDatabase();
			// Drop each collection present in the snapshot before restoring
			for (final String colName : snapshot.collections().keySet()) {
				LOGGER.info("Snapshot restore: dropping collection '{}'", colName);
				db.getCollection(colName).drop();
			}
			restoreCollectionsFromMap(dbMongo, snapshot.collections());
		}
	}

	private static final List<DateTimeFormatter> SEQUENCE_DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss"), DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"), DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd"));

	/**
	 * Parse a date from a backup sequence string.
	 * The sequence may contain trailing letter suffixes (e.g. "_full", "_partial", "_delta", or several
	 * of them such as "_full_delta") which are all stripped before parsing. Supports formats: yyyy-MM-dd, yyyy-MM-ddTHH-mm-ss,
	 * yyyy-MM-dd_HH-mm-ss, yyyy-MM-dd_HH:mm:ss.SSS, yyyy-MM-dd_HH:mm:ss.
	 * @param sequence the sequence string to parse
	 * @return the parsed LocalDate, or null if unparseable
	 */
	public static LocalDate parseDateFromSequence(final String sequence) {
		if (sequence == null || sequence.isEmpty()) {
			return null;
		}
		final String cleaned = stripSequenceSuffix(sequence);
		for (final DateTimeFormatter formatter : SEQUENCE_DATE_FORMATTERS) {
			try {
				return LocalDate.parse(cleaned, formatter);
			} catch (final DateTimeParseException e) {
				// try next format
			}
		}
		// Fallback: try with the original sequence in case stripping was wrong
		if (!cleaned.equals(sequence)) {
			for (final DateTimeFormatter formatter : SEQUENCE_DATE_FORMATTERS) {
				try {
					return LocalDate.parse(sequence, formatter);
				} catch (final DateTimeParseException e) {
					// try next format
				}
			}
		}
		return null;
	}

	/**
	 * Strip every trailing letter suffix like {@code "_full"}, {@code "_partial"} or {@code "_delta"}, and the date of
	 * the full of a delta, so that a sequence such as {@code "2026-10-15_full_delta"} keeps a parsable date.
	 */
	private static String stripSequenceSuffix(final String sequence) {
		String cleaned = sequence;
		int lastUnderscore = cleaned.lastIndexOf('_');
		while (lastUnderscore > 0) {
			final String suffix = cleaned.substring(lastUnderscore + 1);
			if (suffix.isEmpty()
					|| !suffix.chars().allMatch(Character::isLetter) && !DELTA_BASE_PART.matcher(suffix).matches()) {
				break;
			}
			cleaned = cleaned.substring(0, lastUnderscore);
			lastUnderscore = cleaned.lastIndexOf('_');
		}
		return cleaned;
	}

	/**
	 * Parse a date and time from a backup sequence string (same formats and suffix handling as
	 * {@link #parseDateFromSequence}). A sequence carrying only a date resolves to midnight.
	 * @param sequence the sequence string to parse
	 * @return the parsed LocalDateTime, or null if unparseable
	 */
	public static LocalDateTime parseDateTimeFromSequence(final String sequence) {
		if (sequence == null || sequence.isEmpty()) {
			return null;
		}
		final String cleaned = stripSequenceSuffix(sequence);
		for (final DateTimeFormatter formatter : SEQUENCE_DATE_FORMATTERS) {
			try {
				return LocalDateTime.parse(cleaned, formatter);
			} catch (final DateTimeParseException e) {
				// try next format (date-only patterns cannot produce a LocalDateTime)
			}
		}
		final LocalDate dateOnly = parseDateFromSequence(sequence);
		return dateOnly != null ? dateOnly.atStartOfDay() : null;
	}

	/**
	 * List all backup files in pathStore matching this engine's baseName.
	 * Parses the sequence date from each filename.
	 * Files with unparseable dates are ignored with a warning.
	 * @return sorted list of BackupFileInfo (oldest first)
	 */
	List<BackupFileInfo> listBackupFiles() throws IOException {
		final List<BackupFileInfo> result = new ArrayList<>();
		final String prefix = this.baseName + "_";
		if (!Files.isDirectory(this.pathStore)) {
			return result;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.pathStore, prefix + "*" + SUFFIX_TAR_GZ)) {
			for (final Path filePath : stream) {
				final Path name = filePath.getFileName();
				if (name == null) {
					continue;
				}
				final String fileName = name.toString();
				// Skip hidden temporary files
				if (fileName.startsWith(".")) {
					continue;
				}
				// Extract the part between baseName_ and .tar.gz
				String middle = fileName.substring(prefix.length(), fileName.length() - SUFFIX_TAR_GZ.length());
				final boolean partial = middle.endsWith(SUFFIX_PARTIAL);
				if (partial) {
					middle = middle.substring(0, middle.length() - SUFFIX_PARTIAL.length());
				}
				final boolean delta = middle.endsWith(SUFFIX_DELTA);
				final LocalDateTime dateTime = parseDateTimeFromSequence(middle);
				if (dateTime == null) {
					LOGGER.warn("Backup file with unparseable date in sequence, ignored for retention: {}", fileName);
					continue;
				}
				final Matcher base = DELTA_BASE.matcher(middle);
				result.add(new BackupFileInfo(filePath, middle, dateTime, partial, delta,
						delta && base.find() ? base.group(1) : null));
			}
		} catch (final DirectoryIteratorException ex) {
			throw ex.getCause();
		}
		Collections.sort(result);
		return result;
	}

	private static BackupFileInfo findLatestFull(final List<BackupFileInfo> files) {
		BackupFileInfo best = null;
		for (final BackupFileInfo file : files) {
			// A partial archive (subset of collections) can not serve as a full-restore base
			if (file.delta() || file.partial()) {
				continue;
			}
			if (best == null || file.compareTo(best) > 0) {
				best = file;
			}
		}
		return best;
	}

	/**
	 * Return the start date of the most recent full (non-delta) backup file, parsed from its
	 * filename sequence. Sequences are interpreted as UTC timestamps — callers generating
	 * sequences (e.g. for {@link #store}) should format them in UTC for this to be exact.
	 * Typically used to compute the {@code since} parameter of {@link #storeDelta}/{@link #storeAllDelta}.
	 * @return the date of the latest full backup, or {@code null} when no full backup exists
	 * @throws IOException if listing the backup files fails
	 */
	public Date getLastFullBackupDate() throws IOException {
		final BackupFileInfo latestFull = findLatestFull(listBackupFiles());
		if (latestFull == null) {
			return null;
		}
		return Date.from(latestFull.dateTime().toInstant(ZoneOffset.UTC));
	}

	/** @return how the name of a delta holds the date of a full */
	static String baseStamp(final LocalDateTime fullDate) {
		return DELTA_BASE_DATE.format(fullDate);
	}

	/**
	 * Build the sequence of a delta, with the date of the full it starts from in its name: a date alone can not tell a
	 * delta from one written before a full under a clock that was ahead.
	 * @param date the date of the delta, in a format of {@link #parseDateTimeFromSequence(String)}
	 * @param full the date of the full, as {@link #getLastFullBackupDate()} gives it
	 * @return the sequence, ending with {@code "_delta"}
	 */
	public static String deltaSequence(final String date, final Date full) {
		return date + "_" + DELTA_BASE_PREFIX + baseStamp(LocalDateTime.ofInstant(full.toInstant(), ZoneOffset.UTC))
				+ SUFFIX_DELTA;
	}

	/**
	 * Resolve the ordered list of archives needed to restore the latest known state: the most
	 * recent full backup, followed by the most recent delta based on it (if any). An archive dated
	 * more than a day after the current UTC time is left out with a warning, full or delta: a clock that jumped forward
	 * makes it look newer than what was written since, and it would write old documents over them.
	 * So is a delta older than the full, or whose name says it starts from another full.
	 * @return the archive paths to restore in order, or an empty list when no valid full backup exists
	 * @throws IOException if listing the backup files fails
	 */
	public List<Path> getLatestRestoreChain() throws IOException {
		final LocalDateTime limit = LocalDateTime.now(ZoneOffset.UTC).plus(RESTORE_FUTURE_TOLERANCE);
		final List<BackupFileInfo> files = new ArrayList<>();
		for (final BackupFileInfo file : listBackupFiles()) {
			if (file.dateTime().isAfter(limit)) {
				LOGGER.warn("Archive left out of the restore, dated after the current time: {}",
						file.path().getFileName());
				continue;
			}
			files.add(file);
		}
		final BackupFileInfo latestFull = findLatestFull(files);
		if (latestFull == null) {
			return List.of();
		}
		BackupFileInfo latestDelta = null;
		for (final BackupFileInfo file : files) {
			if (!file.delta() || file.compareTo(latestFull) < 0) {
				continue;
			}
			if (!file.isDeltaOf(latestFull)) {
				LOGGER.warn("Delta left out of the restore: {} does not belong to {}", file.path().getFileName(),
						latestFull.path().getFileName());
				continue;
			}
			if (latestDelta == null || file.compareTo(latestDelta) > 0) {
				latestDelta = file;
			}
		}
		if (latestDelta == null) {
			return List.of(latestFull.path());
		}
		return List.of(latestFull.path(), latestDelta.path());
	}

	/**
	 * Restore the latest known state: restore the most recent full backup (the target database
	 * must be empty) then apply the most recent delta based on it, if any.
	 * @return {@code true} if a full backup was found and restored, {@code false} when no valid one exists
	 * @throws IOException if an archive cannot be read
	 * @throws DataAccessException if the database connection fails
	 */
	public boolean restoreLatest() throws IOException, DataAccessException {
		final List<Path> chain = getLatestRestoreChain();
		if (chain.isEmpty()) {
			LOGGER.error("Nothing restored: no valid full backup in: {}", this.pathStore);
			return false;
		}
		if (!restoreFile(chain.get(0), null)) {
			return false;
		}
		for (int i = 1; i < chain.size(); i++) {
			restoreDeltaFile(chain.get(i));
		}
		return true;
	}

	/**
	 * Apply retention policy logic and return the list of files to delete. The latest full backup is never part of it.
	 * @param files sorted list of backup files (oldest first)
	 * @param policy the retention policy
	 * @param referenceDate the reference date for age calculation (typically today)
	 * @return list of files to delete
	 */
	static List<BackupFileInfo> applyRetentionPolicy(
			final List<BackupFileInfo> files,
			final RetentionPolicy policy,
			final LocalDate referenceDate) {
		final LocalDate keepAllLimit = referenceDate.minusDays(policy.keepAllDays());
		final LocalDate keepDailyLimit = referenceDate.minusMonths(policy.keepDailyMonths());
		final LocalDate keepWeeklyLimit = referenceDate.minusMonths(policy.keepWeeklyMonths());
		final WeekFields weekFields = WeekFields.of(Locale.getDefault());

		final List<BackupFileInfo> toDelete = new ArrayList<>();

		// Delta backups are cumulative since the full they are based on: any newer full
		// supersedes them. Keep only the deltas newer than the latest full (they are the ones
		// still needed to roll forward); delete all the others. Deltas never enter the zone
		// deduplication below — only full backups are retained long-term.
		final BackupFileInfo latestFull = findLatestFull(files);

		// Group files by zone, then apply deduplication per group
		// Zone 1: date >= keepAllLimit (recent) → keep all
		// Zone 2: date >= keepDailyLimit && date < keepAllLimit → keep 1 per day
		// Zone 3: date >= keepWeeklyLimit && date < keepDailyLimit → keep 1 per week
		// Zone 4: date < keepWeeklyLimit → delete all

		// For daily dedup: group by LocalDate, keep latest (highest sequence) per day
		final Map<LocalDate, BackupFileInfo> dailyBest = new HashMap<>();
		final List<BackupFileInfo> dailyAll = new ArrayList<>();

		// For weekly dedup: group by year+week, keep latest per week
		final Map<String, BackupFileInfo> weeklyBest = new HashMap<>();
		final List<BackupFileInfo> weeklyAll = new ArrayList<>();

		for (final BackupFileInfo file : files) {
			if (file.delta()) {
				if (latestFull != null && !file.isDeltaOf(latestFull)) {
					toDelete.add(file);
				}
				continue;
			}
			final LocalDate fileDate = file.date();
			if (!fileDate.isBefore(keepAllLimit)) {
				// Zone 1: keep all
				continue;
			} else if (!fileDate.isBefore(keepDailyLimit)) {
				// Zone 2: daily dedup
				dailyAll.add(file);
				final BackupFileInfo existing = dailyBest.get(fileDate);
				if (existing == null || file.compareTo(existing) > 0) {
					dailyBest.put(fileDate, file);
				}
			} else if (!fileDate.isBefore(keepWeeklyLimit)) {
				// Zone 3: weekly dedup
				weeklyAll.add(file);
				final int weekNumber = fileDate.get(weekFields.weekOfWeekBasedYear());
				final int year = fileDate.get(weekFields.weekBasedYear());
				final String weekKey = year + "-W" + weekNumber;
				final BackupFileInfo existingWeek = weeklyBest.get(weekKey);
				if (existingWeek == null || file.compareTo(existingWeek) > 0) {
					weeklyBest.put(weekKey, file);
				}
			} else {
				// Zone 4: delete
				toDelete.add(file);
			}
		}

		// Mark non-best daily files for deletion
		for (final BackupFileInfo file : dailyAll) {
			if (!file.equals(dailyBest.get(file.date()))) {
				toDelete.add(file);
			}
		}

		// Mark non-best weekly files for deletion
		for (final BackupFileInfo file : weeklyAll) {
			final int weekNumber = file.date().get(weekFields.weekOfWeekBasedYear());
			final int year = file.date().get(weekFields.weekBasedYear());
			final String weekKey = year + "-W" + weekNumber;
			if (!file.equals(weeklyBest.get(weekKey))) {
				toDelete.add(file);
			}
		}

		// Whatever the dates say (a clock that jumped, a server stopped for years, a partial archive later on the same
		// day): the policy thins the archives out, it never leaves a base with nothing to restore.
		toDelete.removeIf(file -> file.equals(latestFull));
		return toDelete;
	}

	/**
	 * Apply a retention policy to existing backup files, deleting those that fall outside the policy, each with its
	 * {@code .sha256} file. The latest full backup is always kept, whatever its age. The deltas are outside the policy:
	 * those older than the latest full or whose name gives another full are deleted whatever their age, the others are
	 * kept. Without a full backup, no delta is deleted. Only the archives of this engine go: a name that does not start with its base name, or whose
	 * sequence has no date, is left alone.
	 * Uses today as the reference date for age calculation.
	 * @param policy the retention policy to apply
	 * @return list of deleted file paths
	 * @throws IOException if listing or deleting files fails
	 */
	public List<Path> clean(final RetentionPolicy policy) throws IOException {
		return executeClean(policy, false, LocalDate.now());
	}

	/**
	 * Apply a retention policy to existing backup files, as {@link #clean(RetentionPolicy)} does.
	 * @param policy the retention policy to apply
	 * @param referenceDate the reference date for age calculation (instead of today)
	 * @return list of deleted file paths
	 * @throws IOException if listing or deleting files fails
	 */
	public List<Path> clean(final RetentionPolicy policy, final LocalDate referenceDate) throws IOException {
		return executeClean(policy, false, referenceDate);
	}

	/**
	 * Simulate a retention policy without actually deleting any files.
	 * Uses today as the reference date for age calculation.
	 * @param policy the retention policy to evaluate
	 * @return list of file paths that would be deleted
	 * @throws IOException if listing files fails
	 */
	public List<Path> cleanDryRun(final RetentionPolicy policy) throws IOException {
		return executeClean(policy, true, LocalDate.now());
	}

	/**
	 * Simulate a retention policy without actually deleting any files.
	 * @param policy the retention policy to evaluate
	 * @param referenceDate the reference date for age calculation (instead of today)
	 * @return list of file paths that would be deleted
	 * @throws IOException if listing files fails
	 */
	public List<Path> cleanDryRun(final RetentionPolicy policy, final LocalDate referenceDate) throws IOException {
		return executeClean(policy, true, referenceDate);
	}

	private static void deleteArchive(final BackupFileInfo file) throws IOException {
		LOGGER.info("Retention delete: {}", file.path().getFileName());
		Files.deleteIfExists(file.path());
		Files.deleteIfExists(checksumPath(file.path()));
	}

	/**
	 * Delete the deltas dated after a full that was just written, each with its {@code .sha256} file. Under a clock that
	 * was ahead they were written before this full, and a restore must not apply them on it. Call it only with such a
	 * date: at any other moment a delta later than a full is a good one.
	 * @param fullDate the date of the full
	 * @return the deleted paths
	 * @throws IOException if listing or deleting files fails
	 */
	public List<Path> deleteDeltasAfter(final Date fullDate) throws IOException {
		final LocalDateTime limit = LocalDateTime.ofInstant(fullDate.toInstant(), ZoneOffset.UTC);
		final List<Path> deleted = new ArrayList<>();
		for (final BackupFileInfo file : listBackupFiles()) {
			if (file.delta() && file.dateTime().isAfter(limit)) {
				deleteArchive(file);
				deleted.add(file.path());
			}
		}
		return deleted;
	}

	/**
	 * Delete the deltas of the full a restore would use, each with its {@code .sha256} file, except the latest: they are
	 * cumulative, and {@link #restoreLatest()} uses that one alone. What goes is the way back to a time of the day: for a
	 * volume that is full, when {@link #clean(RetentionPolicy)} did not free enough. The full and the delta of
	 * {@link #getLatestRestoreChain()} stay; without such a pair nothing is deleted.
	 * @return the deleted paths
	 * @throws IOException if listing or deleting files fails
	 */
	public List<Path> deleteDeltasExceptLatest() throws IOException {
		final List<Path> chain = getLatestRestoreChain();
		final List<Path> deleted = new ArrayList<>();
		if (chain.size() < 2) {
			return deleted;
		}
		final List<BackupFileInfo> files = listBackupFiles();
		final BackupFileInfo full = files.stream().filter(file -> file.path().equals(chain.get(0))).findFirst()
				.orElseThrow(() -> new IOException("The full backup disappeared: " + chain.get(0)));
		for (final BackupFileInfo file : files) {
			if (file.isDeltaOf(full) && !file.path().equals(chain.get(1))) {
				deleteArchive(file);
				deleted.add(file.path());
			}
		}
		return deleted;
	}

	private List<Path> executeClean(final RetentionPolicy policy, final boolean dryRun, final LocalDate referenceDate)
			throws IOException {
		final List<BackupFileInfo> allFiles = listBackupFiles();
		final List<BackupFileInfo> toDelete = applyRetentionPolicy(allFiles, policy, referenceDate);
		final List<Path> deletedPaths = new ArrayList<>();

		for (final BackupFileInfo file : toDelete) {
			if (dryRun) {
				LOGGER.info("Retention [DRY-RUN] would delete: {}", file.path().getFileName());
			} else {
				deleteArchive(file);
			}
			deletedPaths.add(file.path());
		}

		LOGGER.info("Retention policy applied: {} kept, {} {}", allFiles.size() - toDelete.size(), toDelete.size(),
				dryRun ? "would be deleted (dry-run)" : "deleted");

		return deletedPaths;
	}
}
