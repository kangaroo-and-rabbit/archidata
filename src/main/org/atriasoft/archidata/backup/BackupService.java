package org.atriasoft.archidata.backup;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * scheduler.addTask("backup", "0 0 * * *", backup::startAsync);
 * BackupResource.setService(backup);
 * }</pre>
 */
public final class BackupService {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);
	/** Free space required on top of twice the size of the last full archive: 1 GiB. */
	public static final long SPACE_MARGIN_BYTES = 1024L * 1024 * 1024;
	/** The memory of a backup is about twice this: a server taking imports has little left at midnight. */
	private static final int CHUNK_MAX_BYTES = 4 * 1024 * 1024;
	/** One of the formats {@link BackupEngine#parseDateTimeFromSequence(String)} reads, the one of the existing archives. */
	private static final DateTimeFormatter SEQUENCE_DATE = DateTimeFormatter
			.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS", Locale.ROOT).withZone(ZoneOffset.UTC);
	private static final String SEQUENCE_SUFFIX_FULL = "_full";
	private static final String THREAD_NAME = "backup-service";

	/** Reads the free space of the volume of the archives. */
	@FunctionalInterface
	interface UsableSpace {
		/**
		 * Read the space a backup can use.
		 * @return the usable space in bytes
		 * @throws IOException if the volume can not be read
		 */
		long read() throws IOException;
	}

	private final BackupEngine engine;
	private final UsableSpace usableSpace;
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
		this(folder, baseName, classes, collections, () -> Files.getFileStore(folder).getUsableSpace());
	}

	/**
	 * Same as the public constructor, on a free space that does not come from the disk.
	 *
	 * @param folder folder of the archives
	 * @param baseName prefix of the archives
	 * @param classes model classes to back up
	 * @param collections collections to back up that no class describes
	 * @param usableSpace reader of the free space
	 * @throws DataAccessException if the collection name of a class can not be resolved
	 * @throws IOException if the folder can not be created or read, or if a collection has a name a restore refuses
	 */
	BackupService(final Path folder, final String baseName, final List<Class<?>> classes,
			final List<String> collections, final UsableSpace usableSpace) throws DataAccessException, IOException {
		this(createEngine(folder, baseName, classes, collections), usableSpace);
	}

	/**
	 * Create the service on an engine that is already configured.
	 *
	 * @param engine the engine, used for {@link BackupEngine#store(String)} and to list the archives
	 * @param usableSpace reader of the free space
	 */
	BackupService(final BackupEngine engine, final UsableSpace usableSpace) {
		this.engine = engine;
		this.usableSpace = usableSpace;
	}

	private static BackupEngine createEngine(
			final Path folder,
			final String baseName,
			final List<Class<?>> classes,
			final List<String> collections) throws DataAccessException, IOException {
		final BackupEngine engine = new BackupEngine(folder, baseName, EngineBackupType.JSON_EXTENDED);
		engine.setEnableStoreOrRestoreData(false);
		engine.setChunkMaxBytes(CHUNK_MAX_BYTES);
		engine.addClass(classes.toArray(Class<?>[]::new));
		engine.addCollection(collections.toArray(String[]::new));
		engine.addCollection(DBAccessMongo.COLLECTION_COUNTERS);
		engine.addClass(Migration.class);
		engine.checkRegisteredCollectionNames();
		BackupEngine.createPrivateDirectories(folder);
		BackupEngine.restrictDirectory(folder);
		engine.deleteAbandonedTemporaryFiles();
		return engine;
	}

	/**
	 * Start a backup in its own thread and return at once, for the cron task as for a manual start: the scheduler has a
	 * single consumer thread, which a dump would hold for minutes. The lock is taken before returning: a state read
	 * right after {@link BackupStart#STARTED} says that a backup is running. A failure of the backup is logged and shows
	 * in {@link #getState()}.
	 * <p>
	 * The volume must have twice the size of the last full archive plus {@link #SPACE_MARGIN_BYTES} free. A start
	 * refused for this reason, or that can not be checked, is logged and recorded as a failed attempt, whoever asked: the
	 * cron task has no other way to tell.
	 *
	 * @return whether the backup started
	 * @throws UncheckedIOException if the folder or its free space can not be read: its message names the folder and must
	 *             not reach a client
	 */
	public BackupStart startAsync() {
		if (!this.running.compareAndSet(false, true)) {
			LOGGER.warn("Backup not started: another one is running");
			return BackupStart.ALREADY_RUNNING;
		}
		boolean started = false;
		try {
			if (!hasEnoughSpace()) {
				return BackupStart.INSUFFICIENT_SPACE;
			}
			final Thread worker = new Thread(this::storeThenRelease, THREAD_NAME);
			worker.setDaemon(true);
			worker.start();
			started = true;
			return BackupStart.STARTED;
		} catch (final IOException ex) {
			LOGGER.error("Backup not started: {}", ex.getMessage(), ex);
			throw new UncheckedIOException(ex);
		} finally {
			if (!started) {
				try {
					this.lastAttempt = new BackupAttempt(now(), false);
				} finally {
					this.running.set(false);
				}
			}
		}
	}

	private boolean hasEnoughSpace() throws IOException {
		long lastFullSize = 0;
		for (final BackupArchive archive : listArchives()) {
			if (archive.type() == BackupArchiveType.FULL) {
				lastFullSize = archive.size();
				break;
			}
		}
		final long needed = 2 * lastFullSize + SPACE_MARGIN_BYTES;
		final long usable = this.usableSpace.read();
		if (usable < needed) {
			LOGGER.error("Backup refused: {} bytes free on the volume, {} needed", usable, needed);
			return false;
		}
		return true;
	}

	private static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	private void storeThenRelease() {
		try {
			final Instant startedAt = now();
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
