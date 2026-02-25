package org.atriasoft.archidata.dataAccess;

import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoCloseable transaction scope for {@link DBAccessMongo}.
 *
 * <p>
 * Provides a try-with-resources pattern for MongoDB transactions.
 * If {@link #commit()} is not called before the context is closed,
 * the transaction is automatically aborted.
 * </p>
 *
 * <p>Usage with explicit DB instance:</p>
 * <pre>
 * try (TransactionContext tx = new TransactionContext(db)) {
 *     db.insert(entity1);
 *     db.insert(entity2);
 *     tx.commit();
 * }
 * // If commit() was not called, the transaction is automatically aborted.
 * </pre>
 *
 * <p>Usage with ThreadLocal connection (within {@code @DataAccessSingleConnection} context):</p>
 * <pre>
 * try (TransactionContext tx = TransactionContext.getTransactionContext()) {
 *     final DBAccessMongo db = DataAccessConnectionContext.getConnection();
 *     db.insert(entity1);
 *     db.insert(entity2);
 *     tx.commit();
 * }
 * </pre>
 *
 * <p>
 * <strong>Important:</strong> MongoDB transactions require a replica set.
 * Standalone MongoDB instances do not support transactions.
 * </p>
 */
public class TransactionContext implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionContext.class);

	private final DBAccessMongo db;
	private boolean committed = false;

	/**
	 * Creates a new transaction context and starts a transaction on the given DB instance.
	 *
	 * @param db the database access instance
	 * @throws DataAccessException if a transaction is already active or cannot be started
	 */
	public TransactionContext(final DBAccessMongo db) throws DataAccessException {
		this.db = db;
		this.db.startTransaction();
	}

	/**
	 * Commits the active transaction.
	 *
	 * @throws DataAccessException if no transaction is active or commit fails
	 */
	public void commit() throws DataAccessException {
		this.db.commitTransaction();
		this.committed = true;
	}

	/**
	 * Closes the transaction context. If {@link #commit()} was not called,
	 * the transaction is automatically aborted.
	 */
	@Override
	public void close() {
		if (!this.committed && this.db.isTransactionActive()) {
			try {
				this.db.abortTransaction();
				LOGGER.debug("Transaction auto-aborted on close");
			} catch (final DataAccessException ex) {
				LOGGER.error("Failed to abort transaction on close: {}", ex.getMessage(), ex);
			}
		}
	}

	/**
	 * Creates a new {@link TransactionContext} using the current thread's
	 * {@link DataAccessConnectionContext} connection.
	 *
	 * <p>
	 * This convenience method retrieves the {@link DBAccessMongo} instance
	 * from the thread-local connection context and starts a transaction on it.
	 * </p>
	 *
	 * @return a new TransactionContext wrapping the current thread's connection
	 * @throws DataAccessException if no connection context is available or transaction cannot be started
	 */
	public static TransactionContext getTransactionContext() throws DataAccessException {
		final DBAccessMongo db = DataAccessConnectionContext.getConnection();
		return new TransactionContext(db);
	}
}
