package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.statistic.QuerySignature.Operation;
import org.atriasoft.archidata.dataAccess.statistic.QueryStatistics;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Raw BSON access of a {@link DBAccessMongo}: insert, read and update documents of a collection by its name, without
 * entity mapping, add-ons or relations.
 *
 * <p>
 * The driver calls go through the {@link MongoSessionCalls} of the connection, so they take part in the active
 * transaction. The conditions are merged by {@link DBAccessMongo#conditionFusionOrEmpty(QueryOptions, boolean)} and
 * the sort built by the sort helper of {@link DBAccessMongo}. It holds no state of its own besides these three
 * references, fixed at the construction; the statistics counters and the logger are the ones of
 * {@link DBAccessMongo}.
 * </p>
 *
 * <p>
 * Thread-safety: none of its own. It is used through the {@link DBAccessMongo} that owns it, which is used by one
 * thread at a time.
 * </p>
 */
final class MongoRawAccess {
	private final DbIoMongo db;
	private final MongoSessionCalls sessionCalls;
	private final DBAccessMongo owner;

	/**
	 * Creates the raw access of a connection.
	 *
	 * @param owner        the access that merges the conditions
	 * @param db           the connection the documents are read from and written to
	 * @param sessionCalls the session-aware driver calls of the connection
	 */
	MongoRawAccess(final DBAccessMongo owner, final DbIoMongo db, final MongoSessionCalls sessionCalls) {
		this.owner = owner;
		this.db = db;
		this.sessionCalls = sessionCalls;
	}

	/**
	 * Inserts a BSON document in a collection.
	 *
	 * @param collectionName Name of the collection to insert into
	 * @param document       BSON Document to insert
	 * @return The ObjectId of the inserted document (auto-generated if not provided)
	 * @throws DataAccessException if insertion fails
	 */
	ObjectId insertBsonDocument(final String collectionName, final Document document) throws DataAccessException {
		try {
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			DBAccessMongo.statistic.countInsertOne++;
			final InsertOneResult result = this.sessionCalls.insertOneIn(collection, document);
			final Object insertedId = result.getInsertedId();
			if (insertedId != null && insertedId instanceof org.bson.BsonObjectId) {
				return ((org.bson.BsonObjectId) insertedId).getValue();
			}
			// If no ID was returned, check if the document has one
			final Object docId = document.get("_id");
			if (docId instanceof ObjectId) {
				return (ObjectId) docId;
			}
			throw new DataAccessException("Failed to retrieve inserted document ID");
		} catch (final DataAccessException ex) {
			throw ex;
		} catch (final Exception ex) {
			DBAccessMongo.LOGGER.error("Failed to insert BSON document: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to insert BSON document: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Retrieves the first BSON document of a collection matching the options.
	 *
	 * @param collectionName Name of the collection to query
	 * @param options        Query options including conditions for filtering
	 * @return The first matching BSON Document or null if not found
	 * @throws DataAccessException if retrieval fails
	 */
	Document getBsonDocument(final String collectionName, final QueryOption... options) throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			final Condition condition = this.owner.conditionFusionOrEmpty(queryOptions, false);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			QueryStatistics.record(collectionName, Operation.FIND, condition.getFilter(), null, null);
			DBAccessMongo.statistic.countFind++;
			final FindIterable<Document> cursor;
			if (filters != null) {
				cursor = this.sessionCalls.findIn(collection, filters);
			} else {
				cursor = this.sessionCalls.findAllIn(collection);
			}
			try (MongoCursor<Document> iterator = cursor.iterator()) {
				if (iterator.hasNext()) {
					return iterator.next();
				}
			}
			return null;
		} catch (final Exception ex) {
			DBAccessMongo.LOGGER.error("Failed to retrieve BSON document: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to retrieve BSON document: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Retrieves the BSON documents of a collection matching the options.
	 *
	 * @param collectionName Name of the collection to query
	 * @param options        Query options including conditions, ordering, limits
	 * @return List of matching BSON Documents (empty list if none found)
	 * @throws DataAccessException if retrieval fails
	 */
	List<Document> getBsonDocuments(final String collectionName, final QueryOption... options)
			throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			final Condition condition = this.owner.conditionFusionOrEmpty(queryOptions, false);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			final Document sorts = DBAccessMongo.generateSortOrNull(queryOptions);
			QueryStatistics.record(collectionName, Operation.FIND, condition.getFilter(), sorts, null);
			DBAccessMongo.statistic.countFind++;
			FindIterable<Document> cursor;
			if (filters != null) {
				cursor = this.sessionCalls.findIn(collection, filters);
			} else {
				cursor = this.sessionCalls.findAllIn(collection);
			}

			// Apply ordering
			if (sorts != null) {
				cursor = cursor.sort(sorts);
			}

			// Apply limit
			final List<Limit> limits = queryOptions.get(Limit.class);
			if (!limits.isEmpty()) {
				cursor = cursor.limit(Math.toIntExact(limits.get(0).getValue()));
			}

			final List<Document> results = new ArrayList<>();
			try (MongoCursor<Document> iterator = cursor.iterator()) {
				while (iterator.hasNext()) {
					results.add(iterator.next());
				}
			}
			return results;
		} catch (final RuntimeException ex) {
			DBAccessMongo.LOGGER.error("Failed to retrieve BSON documents: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to retrieve BSON documents: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Updates the BSON documents of a collection matching the options.
	 *
	 * @param collectionName Name of the collection to update
	 * @param updateDocument BSON Document containing MongoDB update operators
	 * @param options        Query options including conditions for filtering which documents to update
	 * @return Number of documents modified
	 * @throws DataAccessException if update fails, or if the condition is missing or empty without
	 *                             {@link org.atriasoft.archidata.dataAccess.options.AllowMassOperation}
	 */
	long updateBsonDocuments(final String collectionName, final Document updateDocument, final QueryOption... options)
			throws DataAccessException {
		try {
			final QueryOptions queryOptions = new QueryOptions(options);
			// A missing or empty condition is refused, unless AllowMassOperation.
			final Condition condition = this.owner.conditionFusionOrEmpty(queryOptions, true);
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Bson filters = condition.getFilter(collectionName, queryOptions, null);
			QueryStatistics.record(collectionName, Operation.UPDATE, condition.getFilter(), null, null);
			DBAccessMongo.statistic.countUpdateMany++;
			if (filters != null) {
				final UpdateResult result = this.sessionCalls.updateManyIn(collection, filters, updateDocument);
				return result.getModifiedCount();
			}
			// No filter: AllowMassOperation was given, update all documents
			final UpdateResult result = this.sessionCalls.updateManyIn(collection, new Document(), updateDocument);
			return result.getModifiedCount();
		} catch (final Exception ex) {
			DBAccessMongo.LOGGER.error("Failed to update BSON documents: {}", ex.getMessage(), ex);
			throw new DataAccessException("Failed to update BSON documents: " + ex.getMessage(), ex);
		}
	}
}
