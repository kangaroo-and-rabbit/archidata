package org.atriasoft.archidata.backup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccess;
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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class BackupEngine {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackupEngine.class);

	public static enum EngineBackupType {
		JSON_EXTERNAL, // serialize time as ISO and OID as String
		JSON_STANDARD, //
		JSON_EXTENDED, // best solution to retrieve the same DB
	}

	private record CollectionWithUpdate(
			String name,
			String updateField) {};

	private final Path pathStore;
	private final String baseName;
	private final EngineBackupType type;
	private final List<CollectionWithUpdate> collections = new ArrayList<>();
	private boolean enableStoreOrRestoreData = true;

	public BackupEngine(final Path pathToStoreDB, final String baseName, final EngineBackupType type) {
		this.pathStore = pathToStoreDB;
		this.baseName = baseName;
		this.type = type;
	}

	/**
	 * Change the state of the store of the data in the system (need to be disable when data can be too big...
	 * @param value new state
	 */
	public void setEnableStoreOrRestoreData(boolean value) {
		this.enableStoreOrRestoreData = value;
	}

	public void addClass(final Class<?>... classes) throws DataAccessException {
		for (final Class<?> clazz : classes) {
			final String collectionName = AnnotationTools.getTableName(clazz, null);
			boolean foundUpdate = false;
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				foundUpdate = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (foundUpdate) {
					final FieldName tableFieldName = AnnotationTools.getFieldName(field, null);
					this.collections.add(new CollectionWithUpdate(collectionName, tableFieldName.inTable()));
					break;
				}
			}
			if (!foundUpdate) {
				this.collections.add(new CollectionWithUpdate(collectionName, null));
			}
		}
	}

	public void addCollection(final String... collectionNames) {
		for (final String elem : collectionNames) {
			this.collections.add(new CollectionWithUpdate(elem, null));
		}
	}

	public void addCollectionUpdateAt(final String collectionName, final String fieldUpdateName) {
		this.collections.add(new CollectionWithUpdate(collectionName, fieldUpdateName));
	}

	private TarArchiveOutputStream openTarGzOutputStream(final Path tarGzPath) throws IOException {
		final OutputStream fos = Files.newOutputStream(tarGzPath);
		final GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
		final TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos);
		taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		return taos;
	}

	private TarArchiveInputStream openTarGzInputStream(final Path tarGzPath) throws IOException {
		final InputStream fis = Files.newInputStream(tarGzPath);
		final GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
		return new TarArchiveInputStream(gcis);
	}

	private void backupCollectionsToStream(final DBAccessMongo dbMongo, final TarArchiveOutputStream tarOut)
			throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		// Mapper for external:
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
		// config for BSON Mapper
		final JsonWriterSettings settings = JsonWriterSettings.builder()//
				.outputMode(this.type == EngineBackupType.JSON_EXTENDED ? JsonMode.EXTENDED : JsonMode.RELAXED) //
				.build();

		for (final CollectionWithUpdate collectionDescription : this.collections) {
			final ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();

			// TODO use a better way to stream the data ... here if the collection is too
			// big, it will fail
			final MongoCollection<Document> collection = db.getCollection(collectionDescription.name());
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jsonOut))) {
				for (final Document doc : collection.find()) {
					if (this.type == EngineBackupType.JSON_EXTERNAL) {
						writer.write(mapper.writeValueAsString(doc));
					} else {
						writer.write(doc.toJson(settings));
					}
					writer.newLine();
				}
				writer.flush();
			}

			final byte[] data = jsonOut.toByteArray();
			LOGGER.debug("Store data take in memory: {} Bytes", data.length);

			final TarArchiveEntry entry = new TarArchiveEntry(collectionDescription.name() + ".json");
			entry.setSize(data.length);
			tarOut.putArchiveEntry(entry);
			tarOut.write(data);
			tarOut.closeArchiveEntry();
		}
	}

	private void restoreStreamToCollections(
			final DBAccessMongo dbMongo,
			final TarArchiveInputStream tarIn,
			final String collectionName) throws IOException {
		final MongoDatabase db = dbMongo.getInterface().getDatabase();
		if (this.type == EngineBackupType.JSON_EXTERNAL) {
			LOGGER.error("Try to retreive data with generic JSON engine, you will lost real DB inforamtion");
		}
		TarArchiveEntry entry;
		while ((entry = tarIn.getNextEntry()) != null) {
			if (!entry.getName().endsWith(".json")) {
				continue;
			}

			final String colName = entry.getName().replace(".json", "");
			if (collectionName != null && !collectionName.equals(colName)) {
				LOGGER.info("Skip collection: {} (filtered)", colName);
				continue;
			}
			LOGGER.info("Restore collection: [START] {}", colName);
			final MongoCollection<Document> collection = db.getCollection(colName);
			if (collection.find().limit(1).iterator().hasNext()) {
				throw new IOException("Collection : '" + colName + "'is not empty ==> can not insert object inside");
			}
			final List<Document> buffer = new ArrayList<>();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(tarIn));
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null) {
				count++;
				buffer.add(Document.parse(line));
				if (buffer.size() >= 1000) {
					collection.insertMany(buffer);
					buffer.clear();
				}
			}
			if (!buffer.isEmpty()) {
				collection.insertMany(buffer);
			}
			final String colNameFormatted = String.format("%-25s", colName);
			final String countFormatted = String.format("%5d", count);
			LOGGER.info("Restore collection: [ END ] {} {} document(s)", colNameFormatted, countFormatted);
		}

	}

	private void backupCollectionsToStream(final TarArchiveOutputStream tarOut)
			throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccess db = ctx.get();
			if (db instanceof final DBAccessMongo dbMongo) {
				backupCollectionsToStream(dbMongo, tarOut);
				return;
			}
			throw new DataAccessException("Fait to restore DB: only implemented on mongoDb");
		}
	}

	private void restoreStreamToCollections(final TarArchiveInputStream tarIn, final String collectionName)
			throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccess db = ctx.get();
			if (db instanceof final DBAccessMongo dbMongo) {
				restoreStreamToCollections(dbMongo, tarIn, collectionName);
				return;
			}
			throw new DataAccessException("Fait to restore DB: only implemented on mongoDb");
		}
	}

	private void backupDataToStream(final TarArchiveOutputStream tarOut) throws IOException, DataAccessException {
		final String mediaFolder = ConfigBaseVariable.getMediaDataFolder();
		// Create a File to the specific data of medias:
		final File mediaDirFile = new File(mediaFolder);

		if (mediaDirFile.exists() && mediaDirFile.isDirectory() && mediaDirFile.canRead()) {
			// recursive add files in the tar:
			addDirectoryToTar(tarOut, mediaDirFile, "data/");
		}
	}

	private void addDirectoryToTar(final TarArchiveOutputStream tarOut, final File sourceDir, final String basePath)
			throws IOException {
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

						addDirectoryToTar(tarOut, file, entryName + "/");
					} else {
						if (!file.exists() || file.length() < 0) {
							LOGGER.warn("Fichier invalide, ignoré : {}", file.getAbsolutePath());
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
							LOGGER.error("Fal to read the file, ignored: {} - {}", file.getAbsolutePath(),
									e.getMessage());
							tarOut.closeArchiveEntry();
						}
					}
				} catch (final IOException e) {
					LOGGER.error("Fait to add data in the archive, element ignored: {} - {}", file.getAbsolutePath(),
							e.getMessage());
				}
			}
		} else {
			LOGGER.warn("Fail to list the folder: {}", sourceDir.getAbsolutePath());
		}
	}

	private void restoreStreamToData(
			final TarArchiveInputStream tarIn,
			final boolean eraseOldData,
			final boolean moveOldData) throws IOException, DataAccessException {
		final String mediaFolder = ConfigBaseVariable.getMediaDataFolder();
		final File mediaDirFile = new File(mediaFolder);

		// First step: erase or move previous data...
		if (moveOldData && mediaDirFile.exists() && mediaDirFile.isDirectory()) {
			moveExistingDataToHistoryRestore(mediaDirFile);
			deleteEmptyFolder(mediaDirFile, false);
		} else if (eraseOldData && mediaDirFile.exists()) {
			// Remove the old data
			deleteDirectory(mediaDirFile, false);
		}
		// extract data from tar
		extractTarToMediaFolder(tarIn, mediaDirFile);
	}

	private void deleteDirectory(final File directory, final boolean deletecurrentFolder) throws IOException {
		// disable, I an not sure it is a good ideas...
		return;
		/*
		final File[] files = directory.listFiles();
		if (files != null) {
			for (final File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file, true);
				} else if (!file.delete()) {
					LOGGER.warn("Fail to remove the file : {}", file.getAbsolutePath());
				}
			}
		}
		if (deletecurrentFolder && !directory.delete()) {
			LOGGER.warn("Fail to remove the folder: {}", directory.getAbsolutePath());
		}
		*/
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
				final String fileName = entry.getFileName().toString();

				// Ignore the folder history_restore
				if (Files.isDirectory(entry) && fileName.equals("history_restore")) {
					LOGGER.info("skip folder history_restore: {}", fileName);
					continue;
				}

				final Path destination = historyRestorePath.resolve(fileName);
				try {
					Files.move(entry, destination, StandardCopyOption.REPLACE_EXISTING);
					LOGGER.debug("Déplacé : {} vers {}", fileName, destination);
				} catch (final IOException e) {
					LOGGER.error("Erreur lors du déplacement de {} : {}", entry, e.getMessage());
					throw e;
				}
			}
		}
	}

	private void extractTarToMediaFolder(final TarArchiveInputStream tarIn, final File mediaDirFile)
			throws IOException {
		LOGGER.info("Extract data to: {}", mediaDirFile.getAbsolutePath());
		TarArchiveEntry entry;
		while ((entry = tarIn.getNextEntry()) != null) {
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
			final File outputFile = new File(mediaDirFile, entryName);
			// check security to prevent path traversal attacks
			if (!outputFile.getCanonicalPath().startsWith(mediaDirFile.getCanonicalPath())) {
				LOGGER.warn("Input TAR too dangerous ignored: {}", entry.getName());
				continue;
			}
			if (entry.isDirectory()) {
				if (!outputFile.mkdirs() && !outputFile.exists()) {
					LOGGER.warn("Fail to create the folder: {}", outputFile.getAbsolutePath());
				}
			} else {
				// Create parent directories:
				final File parentDir = outputFile.getParentFile();
				if (parentDir != null && !parentDir.exists()) {
					if (!parentDir.mkdirs()) {
						LOGGER.warn("Fail to create parent folder: {}", parentDir.getAbsolutePath());
						continue;
					}
				}
				// Extract the file:
				try (FileOutputStream fos = new FileOutputStream(outputFile)) {
					final byte[] buffer = new byte[8192];
					int bytesRead = 0;
					int totalBytesRead = 0;
					while ((bytesRead = tarIn.read(buffer)) != -1) {
						fos.write(buffer, 0, bytesRead);
						totalBytesRead += bytesRead;
					}
					LOGGER.debug("File extracted: {} size: {}", entryName, totalBytesRead);
				} catch (final IOException e) {
					LOGGER.error("Fail to extract the file {} : {}", entryName, e.getMessage());
					// continue with next file...
				}
			}
		}
	}

	private Date executeStore(final String sequence, final Date since) throws IOException, DataAccessException {
		final Date now = Date.from(Instant.now());
		final String fileName = this.baseName + (sequence != null ? "_" + sequence : "")
				+ (since != null ? "_partial" : "") + ".tar.gz";
		// Create a hidden file fort the temporary generation
		final Path outputFileTmp = this.pathStore.resolve("." + fileName + "_tmp");
		LOGGER.debug("Store in path: {} [BEGIN]", outputFileTmp);
		try (TarArchiveOutputStream tarOut = openTarGzOutputStream(outputFileTmp)) {
			backupCollectionsToStream(tarOut);
			if (this.enableStoreOrRestoreData) {
				backupDataToStream(tarOut);
			}
		}
		try {
			Files.move(outputFileTmp, this.pathStore.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Backup done in file: {}", this.pathStore.resolve(fileName));
		} catch (final IOException e) {
			LOGGER.error("Erreur lors du déplacement du fichier {} vers {}", outputFileTmp, fileName, e);
		}
		LOGGER.debug("Store in path: {} [ END ]", outputFileTmp);
		return now;
	}

	private void executeRestore(final String sequence, final Date since) throws IOException, DataAccessException {
		final String fileName = this.baseName + (sequence != null ? "_" + sequence : "")
				+ (since != null ? "_partial" : "") + ".tar.gz";

		final Path retoreFileName = this.pathStore.resolve(fileName);
		restoreFile(retoreFileName, null);
	}

	// sequence number correspond a un element a ajouter a baseName, cela permet de
	// choisir par exmple 2025-05-16 et donc de faire une sauvegarde complète par
	// jour ou autre selon vos besoins
	public Date store(final String sequence) throws IOException, DataAccessException {
		return executeStore(sequence, null);
	}

	public Date storePartial(final String sequence, final Date since) throws IOException, DataAccessException {
		return executeStore(sequence, since);
	}

	public boolean restoreFile(final Path retoreFileName, final String collectionName)
			throws IOException, DataAccessException {
		LOGGER.info("Restore DB: [START] BD: '{}' from file: '{}'", this.baseName, retoreFileName);
		// check only the existence if not try to restore only a part of the BD
		if (collectionName == null && DataAccess.listCollections(this.baseName).size() != 0) {
			LOGGER.info("Restore DB: [ END ]");
			LOGGER.error("Can not restore, the DB {} already exist", this.baseName);
			return false;
		}
		try (TarArchiveInputStream tarIn = openTarGzInputStream(retoreFileName)) {
			restoreStreamToCollections(tarIn, collectionName);
		}
		// Need to open the stream 2 time due to the fact it is not possible to reset marker position.
		if (this.enableStoreOrRestoreData) {
			try (TarArchiveInputStream tarIn = openTarGzInputStream(retoreFileName)) {
				restoreStreamToData(tarIn, false, true);
			}
		}
		LOGGER.info("Restore DB: [ END ]");
		return true;
	}

	public void restore(final String sequence) throws IOException, DataAccessException {
		executeRestore(sequence, null);
	}

	public void restorePartial(final String sequence, final Date since) throws IOException, DataAccessException {
		executeRestore(sequence, since);
	}
}
