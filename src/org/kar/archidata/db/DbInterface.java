package org.kar.archidata.db;

import java.io.Closeable;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbInterface implements Closeable {
	private final static Logger LOGGER = LoggerFactory.getLogger(DbInterface.class);

	@Override
	public void close() throws IOException {
		LOGGER.error("Check db interface close implementation !!! " + this.getClass().getCanonicalName());
	}
}
