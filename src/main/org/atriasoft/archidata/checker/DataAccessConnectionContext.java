package org.atriasoft.archidata.checker;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.exception.DataAccessException;

import jakarta.ws.rs.InternalServerErrorException;

// Note: never sent the DBAccess through an other thread.
public class DataAccessConnectionContext implements AutoCloseable {
	private static long createdReal = 0L;
	private static long created = 0L;
	private static final ThreadLocal<DBAccess> threadLocalConnection = new ThreadLocal<>();
	private final boolean isOwner;

	/**
	 * Initializes the connection context. If a connection already exists in the
	 * current thread, it reuses it. Otherwise, it creates a new one and takes
	 * ownership for closing it.
	 *
	 * @throws DataAccessException
	 * @throws IOException
	 * @throws InternalServerErrorException
	 */
	public DataAccessConnectionContext() throws InternalServerErrorException, IOException, DataAccessException {
		if (threadLocalConnection.get() == null) {
			final DBAccess db = DBAccess.createInterface();
			threadLocalConnection.set(db);
			this.isOwner = true;
			createdReal++;
		} else {
			this.isOwner = false;
		}
		created++;
	}

	/**
	 * Returns the DBAccess connection for the current thread.
	 */
	public DBAccess get() {
		final DBAccess db = threadLocalConnection.get();
		if (db == null) {
			throw new IllegalStateException(
					"No DBAccess available in current thread. Ensure you're within a DataAccessConnectionContext.");
		}
		return db;
	}

	/**
	 * Returns the DBAccess connection for the current thread.
	 */
	public static DBAccess getConnection() {
		final DBAccess db = threadLocalConnection.get();
		if (db == null) {
			throw new IllegalStateException(
					"No DBAccess available in current thread. Ensure you're within a DataAccessConnectionContext.");
		}
		return db;
	}

	/**
	 * Closes the DBAccess connection if this context created it.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		if (this.isOwner) {
			final DBAccess db = threadLocalConnection.get();
			if (db != null) {
				try {
					db.close();
					System.out.println("connection the DB: real=" + createdReal + " requested=" + created);
				} finally {
					threadLocalConnection.remove();
				}
			}
		}
	}

}