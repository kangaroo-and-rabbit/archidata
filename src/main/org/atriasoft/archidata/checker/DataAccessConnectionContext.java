package org.atriasoft.archidata.checker;

import java.io.IOException;

import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.exception.DataAccessException;

import jakarta.ws.rs.InternalServerErrorException;

// Note: never sent the DBAccess through an other thread.
public class DataAccessConnectionContext implements AutoCloseable {

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
			DBAccess db = DBAccess.createInterface();
			threadLocalConnection.set(db);
			isOwner = true;
		} else {
			isOwner = false;
		}
	}

	/**
	 * Returns the DBAccess connection for the current thread.
	 */
	public DBAccess get() {
		DBAccess db = threadLocalConnection.get();
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
		DBAccess db = threadLocalConnection.get();
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
		if (isOwner) {
			DBAccess db = threadLocalConnection.get();
			if (db != null) {
				try {
					db.close();
				} finally {
					threadLocalConnection.remove();
				}
			}
		}
	}

}