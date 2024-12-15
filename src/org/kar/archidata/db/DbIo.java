package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbIo implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(DbIo.class);

	// we count the number of connection in the system to prevent disconnection in a middle of a stream.
	private int count = 0;

	private static int idCount = 0;
	protected final int id;
	protected final DbConfig config;

	protected DbIo(final DbConfig config) throws IOException {
		this.id = idCount;
		idCount += 10;
		this.config = config;
	}

	@Override
	public synchronized final void close() throws IOException {
		LOGGER.error("[{}] >>>>>>>>>>> Request close count={}", this.id, this.count);
		if (this.count <= 0) {
			LOGGER.error("[{}] >>>>>>>>>>> Request one more close: {}", this.id, this.getClass().getCanonicalName());
			return;
		}
		this.count--;
		if (this.count == 0) {
			LOGGER.warn("v>>>>>>>>>>> close: {}", this.id, this.getClass().getCanonicalName());
			closeImplement();
		} else {
			LOGGER.debug("v>>>>>>>>>>> postponed close: {}", this.id, this.getClass().getCanonicalName());
		}
	}

	public synchronized final void closeForce() throws IOException {
		LOGGER.warn("[{}] >>>>>>>>>>> Request Force close count={}", this.id, this.count);
		if (this.count == 0) {
			LOGGER.info("[{}] >>>>>>>>>>> Nothing to do in force close, DB is already closed", this.id);
			return;
		}
		if (this.config.getKeepConnected()) {
			if (this.count >= 2) {
				LOGGER.error("[{}] >>>>>>>>>>> force close: {} with {} connection on it", this.id,
						this.getClass().getCanonicalName(), this.count - 1);
			}
		} else if (this.count >= 1) {
			LOGGER.error("[{}] >>>>>>>>>>> force close: {} with {} connection on it", this.id,
					this.getClass().getCanonicalName(), this.count);
		}
		this.count = 0;
		LOGGER.warn("[{}] >>>>>>>>>>> force close: {}", this.id, this.getClass().getCanonicalName());
		closeImplement();
	}

	public synchronized final void open() throws IOException {
		LOGGER.warn("[{}] >>>>>>>>>>> Request open count={}", this.id, this.count);
		if (this.count == 0) {
			LOGGER.warn("[{}] >>>>>>>>>>> open: {}", this.id, this.getClass().getCanonicalName());
			openImplement();
		} else {
			LOGGER.debug("[{}] >>>>>>>>>>> already open: {}", this.id, this.getClass().getCanonicalName());
		}
		this.count++;

	}

	protected abstract void closeImplement() throws IOException;

	protected abstract void openImplement() throws IOException;

	public boolean compatible(final DbConfig config) {
		return config.equals(config);
	}
}
