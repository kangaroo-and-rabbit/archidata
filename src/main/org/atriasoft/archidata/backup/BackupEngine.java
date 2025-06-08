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
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class BackupEngine {
	static final Logger LOGGER = LoggerFactory.getLogger(BackupEngine.class);

	private record CollectionWithUpdate(String name, String updateField) {
	};

	private final Path pathStore;
	private final String baseName;
	private final List<CollectionWithUpdate> collections = new ArrayList<>();

	public BackupEngine(final Path pathToStoreDB, final String baseName) {
		this.pathStore = pathToStoreDB;
		this.baseName = baseName;
	}

	public void addClass(final Class<?>... classes) throws DataAccessException {
		for (Class<?> clazz : classes) {
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
		for (String elem : collectionNames) {
			this.collections.add(new CollectionWithUpdate(elem, null));
		}
	}

	public void addCollectionUpdateAt(final String collectionName, final String fieldUpdateName) {
		this.collections.add(new CollectionWithUpdate(collectionName, fieldUpdateName));
	}

	private TarArchiveOutputStream openTarGzOutputStream(Path tarGzPath) throws IOException {
		OutputStream fos = Files.newOutputStream(tarGzPath);
		GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
		TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos);
		taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		return taos;
	}

	private TarArchiveInputStream openTarGzInputStream(Path tarGzPath) throws IOException {
		InputStream fis = Files.newInputStream(tarGzPath);
		GzipCompressorInputStream gcis = new GzipCompressorInputStream(fis);
		return new TarArchiveInputStream(gcis);
	}

	private void backupCollectionsToStream(DBAccessMongo dbMongo, TarArchiveOutputStream tarOut) throws IOException {
		MongoDatabase db = dbMongo.getInterface().getDatabase();
		ObjectMapper mapper = new ObjectMapper();

		for (CollectionWithUpdate collectionDescription : collections) {
			MongoCollection<Document> collection = db.getCollection(collectionDescription.name());

			// TODO use a better way to stream the data ... here if the collection is too
			// big, it will fail
			ByteArrayOutputStream jsonOut = new ByteArrayOutputStream();
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(jsonOut))) {
				for (Document doc : collection.find()) {
					writer.write(mapper.writeValueAsString(doc));
					writer.newLine();
				}
				writer.flush();
			}

			byte[] data = jsonOut.toByteArray();
			LOGGER.warn("Store data take in memory: {} Bytes", data.length);

			TarArchiveEntry entry = new TarArchiveEntry(collectionDescription.name() + ".json");
			entry.setSize(data.length);
			tarOut.putArchiveEntry(entry);
			tarOut.write(data);
			tarOut.closeArchiveEntry();
		}
	}

	private void restoreStreamToCollections(DBAccessMongo dbMongo, TarArchiveInputStream tarIn) throws IOException {
		MongoDatabase db = dbMongo.getInterface().getDatabase();
		ObjectMapper mapper = new ObjectMapper();

		TarArchiveEntry entry;
		while ((entry = tarIn.getNextEntry()) != null) {
			if (!entry.getName().endsWith(".json")) {
				continue;
			}

			String colName = entry.getName().replace(".json", "");
			MongoCollection<Document> collection = db.getCollection(colName);
			List<Document> buffer = new ArrayList<>();

			BufferedReader reader = new BufferedReader(new InputStreamReader(tarIn));
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

	private void backupCollectionsToStream(TarArchiveOutputStream tarOut) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			DBAccess db = ctx.get();
			if (db instanceof DBAccessMongo dbMongo) {
				backupCollectionsToStream(dbMongo, tarOut);
				return;
			}
			throw new DataAccessException("Fait to restore DB: only implemented on mongoDb");
		}
	}

	private void restoreStreamToCollections(TarArchiveInputStream tarIn) throws IOException, DataAccessException {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			DBAccess db = ctx.get();
			if (db instanceof DBAccessMongo dbMongo) {
				restoreStreamToCollections(dbMongo, tarIn);
				return;
			}
			throw new DataAccessException("Fait to restore DB: only implemented on mongoDb");
		}
	}

	private Date executeStore(String sequence, Date since) throws IOException, DataAccessException {
		Date now = Date.from(Instant.now());
		String fileName = baseName + "_" + sequence + (since == null ? "_partial" : "") + "tar.gz";
		Path outputFileTmp = pathStore.resolve("." + fileName + "_tmp");
		try (TarArchiveOutputStream tarOut = openTarGzOutputStream(outputFileTmp)) {
			backupCollectionsToStream(tarOut);
		}
		// TODO: Move file to be atomic
		return now;
	}

	private void executeRestore(String sequence, Date since) throws IOException, DataAccessException {
		String fileName = baseName + "_" + sequence + (since == null ? "_partial" : "") + "tar.gz";
		Path outputFileTmp = pathStore.resolve("." + fileName);
		try (TarArchiveInputStream tarIn = openTarGzInputStream(outputFileTmp)) {
			restoreStreamToCollections(tarIn);
		}
	}

	// sequence number correspond a un element a ajouter a baseName, cela permet de
	// choisir par exmple 2025-05-16 et donc de faire une sauvegarde complète par
	// jour ou autre selon vos besoins
	public Date store(String sequence) throws IOException, DataAccessException {
		return executeStore(sequence, null);
	}

	public Date storePartial(String sequence, Date since) throws IOException, DataAccessException {
		return executeStore(sequence, since);
	}

	public Date restore(String sequence) throws IOException, DataAccessException {
		return executeRestore(sequence, null);
	}

	public Date restorePartial(String sequence, Date since) throws IOException, DataAccessException {
		return executeRestore(sequence, since);
	}
}
