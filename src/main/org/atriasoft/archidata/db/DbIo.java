package org.atriasoft.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbIo implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(DbIo.class);

	// we count the number of connection in the system to prevent disconnection in a
	// middle of a stream.
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
		if (this.count <= 0) {
			return;
		}
		this.count--;
		if (this.count == 0) {
			closeImplement();
		}
	}

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

	public synchronized final void open() throws IOException {
		if (this.count == 0) {
			openImplement();
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
