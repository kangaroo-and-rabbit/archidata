package org.atriasoft.archidata.db;

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
		LOGGER.trace("[{}] Request close count={}", this.id, this.count);
		if (this.count <= 0) {
			LOGGER.error("[{}] Request one more close", this.id);
			return;
		}
		this.count--;
		if (this.count == 0) {
			LOGGER.trace("[{}] close", this.id);
			closeImplement();
		} else {
			LOGGER.trace("[{}] postponed close", this.id);
		}
	}

	public synchronized final void closeForce() throws IOException {
		LOGGER.trace("[{}] Request Force close count={}", this.id, this.count);
		if (this.count == 0) {
			LOGGER.trace("[{}] Nothing to do in force close, DB is already closed", this.id);
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
		LOGGER.trace("[{}] Force close", this.id);
		closeImplement();
	}

	public synchronized final void open() throws IOException {
		LOGGER.trace("[{}] Request open count={}", this.id, this.count);
		if (this.count == 0) {
			LOGGER.trace("[{}] open", this.id);
			openImplement();
		} else {
			LOGGER.trace("[{}] open: already done", this.id);
		}
		this.count++;

	}

	protected abstract void closeImplement() throws IOException;

	protected abstract void openImplement() throws IOException;

	public boolean compatible(final DbConfig config) {
		return this.config.equals(config);
	}

	public DbConfig getConfig() {
		return this.config;
	}
}
