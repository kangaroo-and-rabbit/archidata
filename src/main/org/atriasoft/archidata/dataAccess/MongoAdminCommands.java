package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.db.DbIoMongo;
import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

/**
 * Administration commands of a {@link DBAccessMongo}: list or rename collections, drop a database, create an index,
 * enable or disable the schema validation of a collection.
 *
 * <p>
 * These commands go straight to the driver of the connection and never take part in a transaction: they do not
 * use the session of {@link MongoSessionCalls}. The only state it holds is the connection, fixed at the
 * construction; the only shared state it touches is the {@code countRunCommand} counter of
 * {@link DBAccessMongo#statistic}.
 * </p>
 *
 * <p>
 * Thread-safety: none of its own. It is used through the {@link DBAccessMongo} that owns it, which is used by one
 * thread at a time.
 * </p>
 */
final class MongoAdminCommands {
	private final DbIoMongo db;

	/**
	 * Creates the administration commands of a connection.
	 *
	 * @param db the connection the commands run on
	 */
	MongoAdminCommands(final DbIoMongo db) {
		this.db = db;
	}

	/**
	 * Lists the names of the collections of the database of the connection.
	 *
	 * @return List of collection names
	 */
	List<String> listCollections() {
		return this.db.getDatabase().listCollectionNames().into(new ArrayList<>());
	}

	/**
	 * Renames a collection of the database of the connection.
	 *
	 * @param source      Current collection name
	 * @param destination New collection name
	 */
	void renameCollection(final String source, final String destination) {
		final MongoCollection<Document> previousCollection = this.db.getDatabase().getCollection(source);
		previousCollection
				.renameCollection(new com.mongodb.MongoNamespace(this.db.getDatabase().getName(), destination));
	}

	/**
	 * Drops an entire database.
	 *
	 * @param name DataBase name to delete
	 * @return true if deletion succeeds
	 */
	boolean deleteDatabase(final String name) {
		final MongoDatabase database = this.db.getClient().getDatabase(name);
		database.drop();
		return true;
	}

	/**
	 * Creates an ascending index on a field of a collection.
	 *
	 * @param collectionName Name of the collection
	 * @param fieldName      Name of the field to index
	 */
	void ascendingIndex(final String collectionName, final String fieldName) {
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		collection.createIndex(Indexes.ascending(fieldName));
	}

	/**
	 * Removes the schema validator of a collection and turns its validation off.
	 *
	 * @param collection the name of the collection
	 */
	void disableSchema(final String collection) {
		final Document command = new Document("collMod", collection)//
				.append("validator", new Document())//
				.append("validationLevel", "off");
		DBAccessMongo.statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}

	/**
	 * Sets the JSON Schema validator of a collection, strictly enforced.
	 *
	 * @param collection the name of the collection
	 * @param schema     the JSON Schema to enforce
	 */
	void enableSchema(final String collection, final Document schema) {
		final Document command = new Document("collMod", collection)//
				.append("validator", schema)//
				.append("validationLevel", "strict");
		DBAccessMongo.statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}
}
