package test.atriasoft.archidata.journal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.journal.ChangeJournalEntry;
import org.atriasoft.archidata.journal.ChangeJournalMarker;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoDatabase;

import test.atriasoft.archidata.ConfigureDb;

/** Shared helpers for the change journal tests: raw access to the journal collections. */
final class JournalTestTools {

	private JournalTestTools() {
		// Utility class
	}

	static MongoDatabase database() {
		return ConfigureDb.da.getInterface().getDatabase();
	}

	/** Remove every journal entry and every marker. */
	static void dropJournal() {
		database().getCollection(ChangeJournalEntry.COLLECTION_NAME).deleteMany(new Document());
		database().getCollection(ChangeJournalMarker.COLLECTION_NAME).deleteMany(new Document());
	}

	/** Read every journal entry as a raw document. */
	static List<Document> journalEntries() {
		return database().getCollection(ChangeJournalEntry.COLLECTION_NAME).find().into(new ArrayList<>());
	}

	/** Insert a source document with an explicit {@code updatedAt}, bypassing the timestamp handling. */
	static ObjectId insertRawDated(final String collectionName, final Date updatedAt) {
		final ObjectId id = new ObjectId();
		database().getCollection(collectionName).insertOne(new Document("_id", id)//
				.append("dataLong", 1L)//
				.append("createdAt", updatedAt)//
				.append("updatedAt", updatedAt));
		return id;
	}

	/** Insert a journal entry with an explicit record date, to build a controlled retention scenario. */
	static ObjectId insertJournalEntry(
			final String sourceCollectionName,
			final String sourceId,
			final Date recordedAt) {
		final ObjectId id = new ObjectId();
		database().getCollection(ChangeJournalEntry.COLLECTION_NAME).insertOne(new Document("_id", id)//
				.append("collectionName", sourceCollectionName)//
				.append("sourceId", sourceId)//
				.append("data", "{\"_id\": {\"$oid\": \"" + sourceId + "\"}}")//
				.append("sourceUpdatedAt", recordedAt)//
				.append("recordedAt", recordedAt));
		return id;
	}
}
