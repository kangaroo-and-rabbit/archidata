package org.atriasoft.archidata.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.LongSupplier;

import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Gives the tests, which live in another package, the constructors of {@link BackupService} that take an engine or a
 * free space, so that they depend neither on a real dump nor on the real disk.
 */
public final class BackupServiceBridge {
	/** A disk that never refuses a backup. */
	public static final long LOTS_OF_SPACE = Long.MAX_VALUE;

	private BackupServiceBridge() {
		// Utility class
	}

	/**
	 * Build a service on a test engine, which blocks or fails on demand, with plenty of free space.
	 * @param engine the engine
	 * @return the service
	 */
	public static BackupService on(final BackupEngine engine) {
		return on(engine, () -> LOTS_OF_SPACE);
	}

	/**
	 * Build a service on a test engine and a free space.
	 * @param engine the engine
	 * @param usableSpace the free space, read at each start
	 * @return the service
	 */
	public static BackupService on(final BackupEngine engine, final LongSupplier usableSpace) {
		return new BackupService(engine, usableSpace::getAsLong);
	}

	/**
	 * Build a service as an application does, with plenty of free space.
	 * @param folder folder of the archives
	 * @param baseName prefix of the archives
	 * @param classes model classes to back up
	 * @param collections collections to back up that no class describes
	 * @return the service
	 * @throws DataAccessException if the collection name of a class can not be resolved
	 * @throws IOException if the folder can not be created or read
	 */
	public static BackupService on(
			final Path folder,
			final String baseName,
			final List<Class<?>> classes,
			final List<String> collections) throws DataAccessException, IOException {
		return new BackupService(folder, baseName, classes, collections, () -> LOTS_OF_SPACE);
	}
}
