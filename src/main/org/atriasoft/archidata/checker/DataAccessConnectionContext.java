package org.atriasoft.archidata.checker;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.exception.DataAccessException;

import jakarta.ws.rs.InternalServerErrorException;

/**
 * Thread-local context for managing a shared {@link DBAccessMongo} connection within a single thread.
 *
 * <p>
 * Ensures that only one database connection exists per thread. The first context created
 * on a thread becomes the owner and is responsible for closing the connection.
 * Subsequent contexts on the same thread reuse the existing connection.
 * </p>
 *
 * <p>
 * <strong>Important:</strong> Never pass the {@link DBAccessMongo} instance to another thread.
 * </p>
 */
// Note: never sent the DBAccessMongo through an other thread.
public class DataAccessConnectionContext implements AutoCloseable {
	private static long createdReal = 0L;
	private static long created = 0L;
	private static final ThreadLocal<DBAccessMongo> threadLocalConnection = new ThreadLocal<>();
	private final boolean isOwner;

	/**
	 * Initializes the connection context. If a connection already exists in the
	 * current thread, it reuses it. Otherwise, it creates a new one and takes
	 * ownership for closing it.
	 *
	 * @throws InternalServerErrorException if the database interface creation fails internally
	 * @throws IOException if an I/O error occurs during connection creation
	 * @throws DataAccessException if the database access configuration is invalid
	 */
	public DataAccessConnectionContext() throws InternalServerErrorException, IOException, DataAccessException {
		if (threadLocalConnection.get() == null) {
			final DBAccessMongo db = DBAccessMongo.createInterface();
			threadLocalConnection.set(db);
			this.isOwner = true;
			createdReal++;
		} else {
			this.isOwner = false;
		}
		created++;
	}

	/**
	 * Returns the {@link DBAccessMongo} connection for the current thread.
	 *
	 * @return the database access instance
	 * @throws IllegalStateException if no connection is available in the current thread
	 */
	public DBAccessMongo get() {
		final DBAccessMongo db = threadLocalConnection.get();
		if (db == null) {
			throw new IllegalStateException(
					"No DBAccessMongo available in current thread. Ensure you're within a DataAccessConnectionContext.");
		}
		return db;
	}

	/**
	 * Returns the {@link DBAccessMongo} connection for the current thread (static access).
	 *
	 * @return the database access instance
	 * @throws IllegalStateException if no connection is available in the current thread
	 */
	public static DBAccessMongo getConnection() {
		final DBAccessMongo db = threadLocalConnection.get();
		if (db == null) {
			throw new IllegalStateException(
					"No DBAccessMongo available in current thread. Ensure you're within a DataAccessConnectionContext.");
		}
		return db;
	}

	/**
	 * Closes the {@link DBAccessMongo} connection if this context is the owner.
	 *
	 * @throws IOException if closing the connection fails
	 */
	@Override
	public void close() throws IOException {
		if (this.isOwner) {
			final DBAccessMongo db = threadLocalConnection.get();
			if (db != null) {
				try {
					db.close();
					//System.out.println("connection the DB: real=" + createdReal + " requested=" + created);
				} finally {
					threadLocalConnection.remove();
				}
			}
		}
	}

}
