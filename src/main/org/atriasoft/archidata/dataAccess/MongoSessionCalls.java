package org.atriasoft.archidata.dataAccess;

import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Transaction state of one {@link DBAccessMongo} and the driver calls that follow it.
 *
 * <p>
 * Owns the {@link ClientSession} of the connection: {@code null} until {@link #startTransaction()} opens one,
 * cleared by {@link #commitTransaction()}, {@link #abortTransaction()} and {@link #close()}. When
 * {@link #startTransaction()} fails after the session is opened, the session stays set with no transaction
 * active. The eight driver wrappers ({@code findOneAndUpdateIn}, {@code insertOneIn}, ...) read it on
 * every call: they run the driver call inside the session when there is one, outside otherwise.
 * </p>
 *
 * <p>
 * Thread-safety: none. The session is a plain mutable field, read and written without synchronisation, like the
 * {@link DBAccessMongo} that owns this object, which is used by one thread at a time
 * ({@link org.atriasoft.archidata.checker.DataAccessConnectionContext} keeps one per thread).
 * </p>
 */
final class MongoSessionCalls {
	private final DbIoMongo db;
	private ClientSession session = null;

	/**
	 * Creates the session holder of a connection.
	 *
	 * @param db the connection a transaction is started on
	 */
	MongoSessionCalls(final DbIoMongo db) {
		this.db = db;
	}

	/**
	 * Aborts the transaction still active, if any, and closes its session.
	 */
	void close() {
		if (this.session != null) {
			try {
				this.session.abortTransaction();
				DBAccessMongo.LOGGER.warn("Transaction was still active during close() - aborted");
			} catch (final Exception ex) {
				DBAccessMongo.LOGGER.error("Failed to abort transaction during close: {}", ex.getMessage(), ex);
			} finally {
				this.session.close();
				this.session = null;
			}
		}
	}

	/**
	 * Starts a transaction on the connection.
	 *
	 * @throws DataAccessException if a transaction is already active
	 */
	void startTransaction() throws DataAccessException {
		if (this.session != null) {
			throw new DataAccessException("A transaction is already active on this connection");
		}
		this.session = this.db.getClient().startSession();
		this.session.startTransaction();
		DBAccessMongo.LOGGER.debug("Transaction started");
	}

	/**
	 * Commits the active transaction and closes its session.
	 *
	 * @throws DataAccessException if no transaction is active
	 */
	void commitTransaction() throws DataAccessException {
		if (this.session == null) {
			throw new DataAccessException("No active transaction to commit");
		}
		try {
			this.session.commitTransaction();
			DBAccessMongo.LOGGER.debug("Transaction committed");
		} finally {
			this.session.close();
			this.session = null;
		}
	}

	/**
	 * Aborts the active transaction and closes its session.
	 *
	 * @throws DataAccessException if no transaction is active
	 */
	void abortTransaction() throws DataAccessException {
		if (this.session == null) {
			throw new DataAccessException("No active transaction to abort");
		}
		try {
			this.session.abortTransaction();
			DBAccessMongo.LOGGER.debug("Transaction aborted");
		} finally {
			this.session.close();
			this.session = null;
		}
	}

	/**
	 * Returns the current session.
	 *
	 * @return the active session, or {@code null} when no transaction is active
	 */
	ClientSession getSession() {
		return this.session;
	}

	/**
	 * Returns whether a transaction is currently active.
	 *
	 * @return true if a transaction is active
	 */
	boolean isTransactionActive() {
		return this.session != null;
	}

	/**
	 * {@code collection.findOneAndUpdate(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @param update the update
	 * @param options the options of the driver call
	 * @return the document the driver returns
	 */
	Document findOneAndUpdateIn(
			final MongoCollection<Document> collection,
			final Bson filter,
			final Bson update,
			final FindOneAndUpdateOptions options) {
		return this.session != null ? collection.findOneAndUpdate(this.session, filter, update, options)
				: collection.findOneAndUpdate(filter, update, options);
	}

	/**
	 * {@code collection.insertOne(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param document the document to insert
	 * @return the result of the driver
	 */
	InsertOneResult insertOneIn(final MongoCollection<Document> collection, final Document document) {
		return this.session != null ? collection.insertOne(this.session, document) : collection.insertOne(document);
	}

	/**
	 * {@code collection.updateMany(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @param update the update
	 * @return the result of the driver
	 */
	UpdateResult updateManyIn(final MongoCollection<Document> collection, final Bson filter, final Bson update) {
		return this.session != null ? collection.updateMany(this.session, filter, update)
				: collection.updateMany(filter, update);
	}

	/**
	 * {@code collection.deleteMany(...)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the result of the driver
	 */
	DeleteResult deleteManyIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.deleteMany(this.session, filter) : collection.deleteMany(filter);
	}

	/**
	 * {@code collection.find(filter)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the cursor of the driver
	 */
	FindIterable<Document> findIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.find(this.session, filter) : collection.find(filter);
	}

	/**
	 * {@code collection.find()} without filter, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @return the cursor of the driver
	 */
	FindIterable<Document> findAllIn(final MongoCollection<Document> collection) {
		return this.session != null ? collection.find(this.session) : collection.find();
	}

	/**
	 * {@code collection.countDocuments(filter)}, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @param filter the filter
	 * @return the number of documents
	 */
	long countIn(final MongoCollection<Document> collection, final Bson filter) {
		return this.session != null ? collection.countDocuments(this.session, filter)
				: collection.countDocuments(filter);
	}

	/**
	 * {@code collection.countDocuments()} without filter, inside the active transaction when there is one.
	 *
	 * @param collection the collection
	 * @return the number of documents
	 */
	long countAllIn(final MongoCollection<Document> collection) {
		return this.session != null ? collection.countDocuments(this.session) : collection.countDocuments();
	}
}
