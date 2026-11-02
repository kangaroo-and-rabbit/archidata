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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.migration.model.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nightly, on-demand and delta backups of the database of an application, sharing one lock.
 * <p>
 * The application only declares its folder, its classes and its extra collections. The archives hold the database
 * only (no media file), under the names {@link BackupEngine} expects, and the {@code counters} and migration
 * collections are always part of them: without them a restored base would reuse ids and replay its migrations.
 * <p>
 * Each full backup that succeeds applies a {@link RetentionPolicy}, as does a start refused for lack of space: nothing
 * else deletes an archive, and no route does.
 * Nothing here restores an archive, and what it returns carries no path, no file name and no exception text. The lock
 * lives in the process: two instances of a server on the same folder do not see each other.
 *
 * <pre>{@code
 * final BackupService backup = new BackupService(Paths.get(ConfigVariable.getBackupFolder()),
 * 		ConfigBaseVariable.getDBName(), Initialization.CLASSES_BASE, List.of());
 * scheduler.addTask("backup", "0 0 * * *", backup::startAsync);
 * scheduler.addTask("backup-delta", "0,15,30,45 * * * *", backup::startDeltaAsync);
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
	/**
	 * How far before the last full a delta starts looking. {@code updatedAt} is set when a document is written, not when
	 * its transaction commits, and a Mongo transaction lives up to 60 s by default: a document dated before the full can
	 * become visible behind the cursor of the dump. Two minutes cover this with some room. A document taken twice costs
	 * nothing, a delta replaces it by its {@code _id} with what is the same or newer.
	 */
	static final long DELTA_MARGIN_MILLIS = 120_000;
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
	/** {@code null}: nothing here deletes an archive. */
	private final RetentionPolicy retention;
	/** Guards the fields below, for the time of a decision, never of a dump. */
	private final Object lock = new Object();
	/** What is running: whoever sets it runs the backup, and clears it after the attempt is written. */
	private BackupArchiveType runningType = null;
	/** A full asked during a delta: the thread of the delta runs it next. */
	private boolean fullWaiting = false;
	private BackupAttempt lastFullAttempt = null;
	private BackupAttempt lastDeltaAttempt = null;

	/**
	 * Create the service with the default retention, {@link RetentionPolicy#RetentionPolicy()}: see
	 * {@link #BackupService(Path, String, List, List, RetentionPolicy)}.
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
		this(folder, baseName, classes, collections, new RetentionPolicy());
	}

	/**
	 * Create the service and its folder, private to the service when it is created here, and delete the temporary files
	 * a backup of this base left when the process was killed.
	 *
	 * @param folder folder of the archives
	 * @param baseName prefix of the archives: pass the name of the database ({@code ConfigBaseVariable.getDBName()})
	 * @param classes model classes to back up
	 * @param collections collections to back up that no class describes; {@code counters} and the migration
	 *            collection are always added
	 * @param retention what to keep, applied after each full backup that succeeds and when a start lacks space;
	 *            {@code null} for an application that handles its archives by itself: nothing here deletes one
	 * @throws DataAccessException if the collection name of a class can not be resolved
	 * @throws IOException if the folder can not be created or read, or if a collection has a name a restore refuses
	 */
	public BackupService(final Path folder, final String baseName, final List<Class<?>> classes,
			final List<String> collections, final RetentionPolicy retention) throws DataAccessException, IOException {
		this(createEngine(folder, baseName, classes, collections), () -> Files.getFileStore(folder).getUsableSpace(),
				retention);
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
		this(createEngine(folder, baseName, classes, collections), usableSpace, new RetentionPolicy());
	}

	/**
	 * Create the service on an engine that is already configured.
	 *
	 * @param engine the engine, used for {@link BackupEngine#store(String)} and to list the archives
	 * @param usableSpace reader of the free space
	 */
	BackupService(final BackupEngine engine, final UsableSpace usableSpace) {
		this(engine, usableSpace, new RetentionPolicy());
	}

	/**
	 * Create the service on an engine that is already configured.
	 *
	 * @param engine the engine, used for {@link BackupEngine#store(String)} and to list the archives
	 * @param usableSpace reader of the free space
	 * @param retention what to keep, applied after each full backup that succeeds; {@code null} to delete nothing
	 */
	BackupService(final BackupEngine engine, final UsableSpace usableSpace, final RetentionPolicy retention) {
		this.engine = engine;
		this.usableSpace = usableSpace;
		this.retention = retention;
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
	 * Start a full backup in its own thread and return at once, for the cron task as for a manual start: the scheduler
	 * has a single consumer thread, which a dump would hold for minutes. The lock is taken before returning: a state read
	 * right after {@link BackupStart#STARTED} says that a backup is running. A failure of the backup is logged and shows
	 * in {@link #getState()}. A full that succeeds applies the retention, whose failure is only logged.
	 * <p>
	 * During a delta the full waits for it and {@link BackupStart#STARTED} is returned: the scheduler does not order two
	 * tasks of the same minute, and a delta that goes first at midnight must not cost the night its full.
	 * <p>
	 * The volume must have twice the size of the last full archive plus {@link #SPACE_MARGIN_BYTES} free. When it does
	 * not, the retention runs and the space is checked once more. A start refused for this reason, or that can not be
	 * checked, is logged and recorded as a failed attempt, whoever asked: the
	 * cron task has no other way to tell.
	 *
	 * @return whether the backup started
	 * @throws UncheckedIOException if the folder or its free space can not be read: its message names the folder and must
	 *             not reach a client
	 */
	public BackupStart startAsync() {
		synchronized (this.lock) {
			if (this.runningType == BackupArchiveType.FULL || this.fullWaiting) {
				LOGGER.warn("Backup not started: another one is running");
				return BackupStart.ALREADY_RUNNING;
			}
			return checkThenStart(BackupArchiveType.FULL);
		}
	}

	/**
	 * Start a delta backup, for a cron task: {@code backup::startDeltaAsync}. It follows the rules of {@link #startAsync()}
	 * (its own thread, the same lock, the same check of the free space, an attempt recorded) and holds what changed since
	 * the last full archive found on the disk, so that it survives a restart and a restore finds the pair.
	 * <p>
	 * A backup in progress makes it skip its turn with no warning: the next one holds everything this one would have. With
	 * no full archive on the disk there is nothing a delta could be restored on: a full backup starts instead, so that
	 * a new server, or an emptied folder, has something to restore from the next quarter of an hour on. A delta with the
	 * same content as the previous one leaves no file and counts as a successful attempt. A failed full stays in the
	 * state whatever the deltas do: see {@link #getState()}.
	 *
	 * @return whether the backup started
	 * @throws UncheckedIOException if the folder or its free space can not be read: its message names the folder and must
	 *             not reach a client
	 */
	public BackupStart startDeltaAsync() {
		synchronized (this.lock) {
			if (this.runningType != null) {
				LOGGER.info("Delta backup skipped: a backup is running");
				return BackupStart.ALREADY_RUNNING;
			}
			return checkThenStart(BackupArchiveType.DELTA);
		}
	}

	/** Call it with {@link #lock} held. */
	private BackupStart checkThenStart(final BackupArchiveType requested) {
		BackupArchiveType type = requested;
		boolean accepted = false;
		try {
			// First: an empty list says nothing of a folder that is gone, and a full would be the wrong answer to it.
			final Path folder = this.engine.getPathStore();
			if (!Files.isDirectory(folder)) {
				throw new NoSuchFileException(folder.toString());
			}
			if (type == BackupArchiveType.DELTA && this.engine.getLastFullBackupDate() == null) {
				LOGGER.info("No full backup to base a delta on: a full one starts instead");
				type = BackupArchiveType.FULL;
			}
			if (!hasEnoughSpaceOnceCleaned()) {
				return BackupStart.INSUFFICIENT_SPACE;
			}
			if (this.runningType != null) {
				LOGGER.info("Backup waits for the delta in progress");
				this.fullWaiting = true;
			} else {
				final BackupArchiveType first = type;
				final Thread worker = new Thread(() -> runThenRelease(first), THREAD_NAME);
				worker.setDaemon(true);
				worker.start();
				this.runningType = type;
			}
			accepted = true;
			return BackupStart.STARTED;
		} catch (final IOException ex) {
			LOGGER.error("Backup not started: {}", ex.getMessage(), ex);
			throw new UncheckedIOException(ex);
		} finally {
			if (!accepted) {
				recordAttempt(type, now(), false);
			}
		}
	}

	/** Call it with {@link #lock} held. */
	private void recordAttempt(final BackupArchiveType type, final Instant date, final boolean success) {
		final BackupAttempt attempt = new BackupAttempt(date, success);
		if (type == BackupArchiveType.FULL) {
			this.lastFullAttempt = attempt;
		} else {
			this.lastDeltaAttempt = attempt;
		}
	}

	/**
	 * Call it with {@link #lock} held.
	 * @return the failed full if there is one, otherwise the most recent attempt: 96 deltas a day must not make a failed
	 *         full look fine
	 */
	private BackupAttempt attemptToShow() {
		if (this.lastFullAttempt == null) {
			return this.lastDeltaAttempt;
		}
		if (!this.lastFullAttempt.success() || this.lastDeltaAttempt == null
				|| this.lastFullAttempt.date().isAfter(this.lastDeltaAttempt.date())) {
			return this.lastFullAttempt;
		}
		return this.lastDeltaAttempt;
	}

	/**
	 * Only a full applies the retention after it, and a volume too full refuses that full: nothing would ever free space
	 * again. So a refusal applies it, then checks once more.
	 */
	private boolean hasEnoughSpaceOnceCleaned() throws IOException {
		if (hasEnoughSpace(this.retention == null)) {
			return true;
		}
		if (this.retention == null) {
			return false;
		}
		LOGGER.warn("Too little free space for a backup: the retention runs before a new check");
		applyRetention();
		return hasEnoughSpace(true);
	}

	private boolean hasEnoughSpace(final boolean logRefusal) throws IOException {
		long lastFullSize = 0;
		for (final BackupArchive archive : listArchives()) {
			if (archive.type() == BackupArchiveType.FULL) {
				lastFullSize = archive.size();
				break;
			}
		}
		final long needed = 2 * lastFullSize + SPACE_MARGIN_BYTES;
		final long usable = this.usableSpace.read();
		if (usable >= needed) {
			return true;
		}
		if (logRefusal) {
			LOGGER.error("Backup refused: {} bytes free on the volume, {} needed", usable, needed);
		}
		return false;
	}

	private static Instant now() {
		return Instant.now().truncatedTo(ChronoUnit.MILLIS);
	}

	private void runThenRelease(final BackupArchiveType first) {
		boolean released = false;
		try {
			BackupArchiveType type = first;
			while (type != null) {
				runOne(type);
				type = nextOrRelease();
			}
			released = true;
		} finally {
			// Unconditional: whatever fails above, a lock kept here would refuse every backup until a restart.
			if (!released) {
				synchronized (this.lock) {
					this.runningType = null;
					this.fullWaiting = false;
				}
			}
		}
	}

	/** @return the full that waited for this backup, {@code null} when the lock is released */
	private BackupArchiveType nextOrRelease() {
		synchronized (this.lock) {
			this.runningType = this.fullWaiting ? BackupArchiveType.FULL : null;
			this.fullWaiting = false;
			return this.runningType;
		}
	}

	private void runOne(final BackupArchiveType type) {
		final Instant startedAt = now();
		boolean success = false;
		try {
			LOGGER.info("Backup {} [START]", type);
			if (type == BackupArchiveType.FULL) {
				this.engine.store(SEQUENCE_DATE.format(startedAt) + SEQUENCE_SUFFIX_FULL);
				success = true;
				retireLaterDeltas(startedAt);
				applyRetention();
			} else {
				storeDelta(startedAt);
				success = true;
			}
			LOGGER.info("Backup {} [ END ]", type);
		} catch (final Throwable ex) {
			LOGGER.error("Backup {} failed: {}", type, ex.getMessage(), ex);
		} finally {
			synchronized (this.lock) {
				recordAttempt(type, startedAt, success);
			}
		}
	}

	/** A delta dated after the full that just ended comes from a clock that was ahead: it was written before this full. */
	private void retireLaterDeltas(final Instant fullDate) {
		if (this.retention == null) {
			return;
		}
		try {
			this.engine.deleteDeltasAfter(Date.from(fullDate));
		} catch (final Throwable ex) {
			// getLatestRestoreChain leaves them out as long as their date or their name tells them apart.
			LOGGER.error("Backup: can not delete the deltas dated after the full: {}", ex.getMessage(), ex);
		}
	}

	/** After a full that succeeded, or at a refusal for lack of space: never at the end of a failed backup. */
	private void applyRetention() {
		if (this.retention == null) {
			return;
		}
		try {
			final int deleted = this.engine.clean(this.retention).size();
			LOGGER.info("Backup retention: {} archive(s) deleted", deleted);
		} catch (final Throwable ex) {
			// A backup that just succeeded stays a success; what was not deleted goes with the next one.
			LOGGER.error("Backup retention failed: {}", ex.getMessage(), ex);
		}
	}

	private void storeDelta(final Instant startedAt) throws IOException, DataAccessException {
		final Date lastFull = this.engine.getLastFullBackupDate();
		if (lastFull == null) {
			throw new IOException("The full backup a delta needs is gone");
		}
		if (lastFull.toInstant().isAfter(startedAt)) {
			// Such a delta would hold nothing, and a restore would stop at this full: the clock must be looked at.
			throw new IOException("The last full backup is dated after the current time: " + lastFull.toInstant());
		}
		final Date since = Date.from(lastFull.toInstant().minusMillis(DELTA_MARGIN_MILLIS));
		this.engine.storeDeltaIfChanged(BackupEngine.deltaSequence(SEQUENCE_DATE.format(startedAt), lastFull), since);
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
			final Instant date = file.dateTime().toInstant(ZoneOffset.UTC);
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
	 * Read the state of the backups. The last attempt is kept in memory: it is {@code null} after a restart. It is the last
	 * full as long as it failed, until a full works, otherwise the most recent attempt, full or delta.
	 *
	 * @return whether a backup is running, the last attempt and the archives
	 * @throws IOException if the folder can not be read, see {@link #listArchives()}
	 */
	public BackupState getState() throws IOException {
		final boolean isRunning;
		final BackupAttempt attempt;
		synchronized (this.lock) {
			isRunning = this.runningType != null;
			attempt = attemptToShow();
		}
		return new BackupState(isRunning, attempt, listArchives());
	}
}
