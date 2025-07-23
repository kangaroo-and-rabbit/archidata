package org.atriasoft.archidata.backup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
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
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ContextGenericTools;
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

	public BackupEngine(final Path pathToStoreDB, final String baseName, final EngineBackupType type) {
		this.pathStore = pathToStoreDB;
		this.baseName = baseName;
		this.type = type;
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
			LOGGER.warn("Store data take in memory: {} Bytes", data.length);

			final TarArchiveEntry entry = new TarArchiveEntry(collectionDescription.name() + ".json");
			entry.setSize(data.length);
			tarOut.putArchiveEntry(entry);
			tarOut.write(data);
			tarOut.closeArchiveEntry();
		}
	}

	private void restoreStreamToCollections(final DBAccessMongo dbMongo, final TarArchiveInputStream tarIn)
			throws IOException {
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
			final MongoCollection<Document> collection = db.getCollection(colName);
			final List<Document> buffer = new ArrayList<>();

			final BufferedReader reader = new BufferedReader(new InputStreamReader(tarIn));
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.add(Document.parse(line));
				if (buffer.size() >= 1000) {
					collection.insertMany(buffer);
					buffer.clear();
				}
			}
			if (!buffer.isEmpty()) {
				collection.insertMany(buffer);
			}
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

	private void restoreStreamToCollections(final TarArchiveInputStream tarIn) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccess db = ctx.get();
			if (db instanceof final DBAccessMongo dbMongo) {
				restoreStreamToCollections(dbMongo, tarIn);
				return;
			}
			throw new DataAccessException("Fait to restore DB: only implemented on mongoDb");
		}
	}

	private Date executeStore(final String sequence, final Date since) throws IOException, DataAccessException {
		final Date now = Date.from(Instant.now());
		final String fileName = this.baseName + (sequence != null ? "_" + sequence : "")
				+ (since != null ? "_partial" : "") + ".tar.gz";
		// Create a hidden file fort the temporary generation
		final Path outputFileTmp = this.pathStore.resolve("." + fileName + "_tmp");
		LOGGER.warn("Sttore in path: {} [BEGIN]", outputFileTmp);
		try (TarArchiveOutputStream tarOut = openTarGzOutputStream(outputFileTmp)) {
			backupCollectionsToStream(tarOut);
		}
		try {
			Files.move(outputFileTmp, this.pathStore.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
		} catch (final IOException e) {
			LOGGER.error("Erreur lors du déplacement du fichier {} vers {}", outputFileTmp, fileName, e);
		}
		LOGGER.warn("Sttore in path: {} [ END ]", outputFileTmp);
		return now;
	}

	private void executeRestore(final String sequence, final Date since) throws IOException, DataAccessException {
		final String fileName = this.baseName + (sequence != null ? "_" + sequence : "")
				+ (since != null ? "_partial" : "") + ".tar.gz";
		final Path outputFileTmp = this.pathStore.resolve(fileName);
		try (TarArchiveInputStream tarIn = openTarGzInputStream(outputFileTmp)) {
			restoreStreamToCollections(tarIn);
		}
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

	public void restore(final String sequence) throws IOException, DataAccessException {
		executeRestore(sequence, null);
	}

	public void restorePartial(final String sequence, final Date since) throws IOException, DataAccessException {
		executeRestore(sequence, since);
	}
}
