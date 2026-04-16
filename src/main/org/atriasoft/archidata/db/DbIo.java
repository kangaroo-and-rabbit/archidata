package org.atriasoft.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for database I/O connections with reference counting.
 *
 * <p>
 * Manages the lifecycle of a database connection through reference counting.
 * The connection is opened on the first {@link #open()} call and closed when
 * the last reference is released via {@link #close()}.
 * </p>
 */
public abstract class DbIo implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbIo.class);

	// we count the number of connection in the system to prevent disconnection in a
	// middle of a stream.
	private int count = 0;

	private static int idCount = 0;
	/** The unique identifier for this connection instance. */
	protected final int id;
	/** The database configuration associated with this connection. */
	protected final DbConfig config;

	/**
	 * Constructs a DbIo with the given configuration and assigns a unique identifier.
	 *
	 * @param config the database configuration
	 * @throws IOException if initialization fails
	 */
	protected DbIo(final DbConfig config) throws IOException {
		this.id = idCount;
		idCount += 10;
		this.config = config;
	}

	/**
	 * Decrements the reference count and closes the underlying connection when the count reaches zero.
	 *
	 * @throws IOException if closing the connection fails
	 */
	@Override
	public synchronized final void close() throws IOException {
		if (this.count <= 0) {
			return;
		}
		this.count--;
		if (this.count == 0) {
			closeImplement();
		}
	}

	/**
	 * Forcefully closes the connection regardless of the reference count.
	 *
	 * @throws IOException if closing the connection fails
	 */
	public synchronized final void closeForce() throws IOException {
		if (this.count == 0) {
			return;
		}
		if (this.config.getKeepConnected()) {
			if (this.count >= 2) {
				LOGGER.error("[{}] Force close: with {} connection on it", this.id, this.count - 1);
			}
		} else if (this.count >= 1) {
			LOGGER.error("[{}] Force close: with {} connection on it", this.id, this.count);
		}
		this.count = 0;
		closeImplement();
	}

	/**
	 * Increments the reference count and opens the underlying connection if not already open.
	 *
	 * @throws IOException if opening the connection fails
	 */
	public synchronized final void open() throws IOException {
		if (this.count == 0) {
			openImplement();
		}
		this.count++;

	}

	/**
	 * Implementation-specific close logic. Called when the reference count reaches zero.
	 *
	 * @throws IOException if closing the underlying resource fails
	 */
	protected abstract void closeImplement() throws IOException;

	/**
	 * Implementation-specific open logic. Called when the first reference is acquired.
	 *
	 * @throws IOException if opening the underlying resource fails
	 */
	protected abstract void openImplement() throws IOException;

	/**
	 * Checks whether this connection is compatible with the given configuration.
	 *
	 * @param config the configuration to compare against
	 * @return {@code true} if the configurations are equal
	 */
	public boolean compatible(final DbConfig config) {
		return this.config.equals(config);
	}

	/**
	 * Returns the database configuration associated with this connection.
	 *
	 * @return the database configuration
	 */
	public DbConfig getConfig() {
		return this.config;
	}
}
