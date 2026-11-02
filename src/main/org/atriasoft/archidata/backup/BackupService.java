package org.atriasoft.archidata.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.migration.model.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nightly and on-demand backups of the database of an application, sharing one lock.
 * <p>
 * The application only declares its folder, its classes and its extra collections. The archives hold the database
 * only (no media file), under the names {@link BackupEngine} expects, and the {@code counters} and migration
 * collections are always part of them: without them a restored base would reuse ids and replay its migrations.
 * <p>
 * Nothing here deletes or restores an archive, and what it returns carries no path, no file name and no exception
 * text. The lock lives in the process: two instances of a server on the same folder do not see each other.
 *
 * <pre>{@code
 * final BackupService backup = new BackupService(Paths.get(ConfigVariable.getBackupFolder()),
 * 		ConfigBaseVariable.getDBName(), Initialization.CLASSES_BASE, List.of());
 * scheduler.addTask("backup", "0 0 * * *", backup);
 * BackupResource.setService(backup);
 * }</pre>
 */
public final class BackupService implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);
	/** One of the formats {@link BackupEngine#parseDateTimeFromSequence(String)} reads, the one of the existing archives. */
	private static final DateTimeFormatter SEQUENCE_DATE = DateTimeFormatter
			.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS", Locale.ROOT).withZone(ZoneOffset.UTC);
	private static final String SEQUENCE_SUFFIX_FULL = "_full";
	private static final String THREAD_NAME = "backup-service";

	private final BackupEngine engine;
	/** The lock: whoever sets it runs the backup, and releases it after {@link #lastAttempt} is written. */
	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile BackupAttempt lastAttempt = null;

	/**
	 * Create the service and its folder, private to the service when it is created here, and delete the temporary files
	 * a backup of this base left when the process was killed.
	 *
	 * @param folder folder of the archives
	 * @param baseName prefix of the archives: pass the name of the database ({@code ConfigBaseVariable.getDBName()})
	 * @param classes model classes to back up
	 * @param collections collections to back up that no class describes; {@code counters} and the migration
	 *            collection are always added
	 * @throws DataAccessException if the collection name of a class can not be resolved
	 * @throws IOException if the folder can not be created or read, or if a collection has a name a restore refuses
	 */
	public BackupService(final Path folder, final String baseName, final List<Class<?>> classes,
			final List<String> collections) throws DataAccessException, IOException {
		this(createEngine(folder, baseName, classes, collections));
	}

	/**
	 * Create the service on an engine that is already configured.
	 *
	 * @param engine the engine, used for {@link BackupEngine#store(String)} and to list the archives
	 */
	BackupService(final BackupEngine engine) {
		this.engine = engine;
	}

	private static BackupEngine createEngine(
			final Path folder,
			final String baseName,
			final List<Class<?>> classes,
			final List<String> collections) throws DataAccessException, IOException {
		final BackupEngine engine = new BackupEngine(folder, baseName, EngineBackupType.JSON_EXTENDED);
		engine.setEnableStoreOrRestoreData(false);
		engine.addClass(classes.toArray(Class<?>[]::new));
		engine.addCollection(collections.toArray(String[]::new));
		engine.addCollection(DBAccessMongo.COLLECTION_COUNTERS);
		engine.addClass(Migration.class);
		engine.checkRegisteredCollectionNames();
		BackupEngine.createPrivateDirectories(folder);
		engine.deleteAbandonedTemporaryFiles();
		return engine;
	}

	/**
	 * Run a backup in the calling thread, for the cron task. Does nothing when a backup is already running: that one
	 * is as recent as this one would be, and the scheduler must not wait for it. Never throws: a failure is logged and
	 * shows in {@link #getState()}.
	 */
	@Override
	public void run() {
		if (!this.running.compareAndSet(false, true)) {
			LOGGER.warn("Backup skipped: another one is running");
			return;
		}
		storeThenRelease();
	}

	/**
	 * Start a backup in its own thread and return at once. The lock is taken before returning: a state read right
	 * after a {@code true} says that a backup is running.
	 *
	 * @return {@code true} if the backup started, {@code false} if one was already running
	 */
	public boolean startAsync() {
		if (!this.running.compareAndSet(false, true)) {
			return false;
		}
		boolean started = false;
		try {
			final Thread worker = new Thread(this::storeThenRelease, THREAD_NAME);
			worker.setDaemon(true);
			worker.start();
			started = true;
		} finally {
			if (!started) {
				this.running.set(false);
			}
		}
		return true;
	}

	private void storeThenRelease() {
		try {
			final Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
			boolean success = false;
			try {
				LOGGER.info("Backup [START]");
				this.engine.store(SEQUENCE_DATE.format(startedAt) + SEQUENCE_SUFFIX_FULL);
				success = true;
				LOGGER.info("Backup [ END ]");
			} catch (final Throwable ex) {
				LOGGER.error("Backup failed: {}", ex.getMessage(), ex);
			} finally {
				this.lastAttempt = new BackupAttempt(startedAt, success);
			}
		} finally {
			// Unconditional: whatever fails above, a lock kept here would refuse every backup until a restart.
			this.running.set(false);
		}
	}

	/**
	 * List the finished archives of the folder, most recent first. An archive being written and a file whose name the
	 * engine does not understand are left out.
	 *
	 * @return the archives
	 * @throws IOException if the folder can not be read or does not exist any more, which an empty list would hide (a
	 *             volume no longer mounted): the message of this exception names the folder and must not reach a client
	 */
	public List<BackupArchive> listArchives() throws IOException {
		final Path folder = this.engine.getPathStore();
		if (!Files.isDirectory(folder)) {
			throw new NoSuchFileException(folder.toString());
		}
		final List<BackupArchive> out = new ArrayList<>();
		for (final BackupFileInfo file : this.engine.listBackupFiles()) {
			final BackupArchive archive = toArchive(file);
			if (archive != null) {
				out.add(archive);
			}
		}
		out.sort(Comparator.comparing(BackupArchive::date).reversed());
		return out;
	}

	/** @return what a client may know of this file, {@code null} when it is not a regular file or just disappeared */
	private static BackupArchive toArchive(final BackupFileInfo file) {
		try {
			final BasicFileAttributes attributes = Files.readAttributes(file.path(), BasicFileAttributes.class,
					LinkOption.NOFOLLOW_LINKS);
			if (!attributes.isRegularFile()) {
				LOGGER.warn("Backup list: not a regular file, ignored: {}", file.path().getFileName());
				return null;
			}
			final Instant date = BackupEngine.parseDateTimeFromSequence(file.sequence()).toInstant(ZoneOffset.UTC);
			return new BackupArchive(date, attributes.size(), typeOf(file),
					Files.exists(BackupEngine.checksumPath(file.path())));
		} catch (final IOException ex) {
			LOGGER.warn("Backup list: unreadable file, ignored: {}", file.path().getFileName(), ex);
			return null;
		}
	}

	private static BackupArchiveType typeOf(final BackupFileInfo file) {
		if (file.partial()) {
			return BackupArchiveType.PARTIAL;
		}
		return file.delta() ? BackupArchiveType.DELTA : BackupArchiveType.FULL;
	}

	/**
	 * Read the state of the backups. The last attempt is kept in memory: it is {@code null} after a restart.
	 *
	 * @return whether a backup is running, the last attempt and the archives
	 * @throws IOException if the folder can not be read, see {@link #listArchives()}
	 */
	public BackupState getState() throws IOException {
		final boolean isRunning = this.running.get();
		return new BackupState(isRunning, this.lastAttempt, listArchives());
	}
}
