package test.atriasoft.archidata.backup;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.backup.BackupArchive;
import org.atriasoft.archidata.backup.BackupArchiveType;
import org.atriasoft.archidata.backup.BackupAttempt;
import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.backup.BackupService;
import org.atriasoft.archidata.backup.BackupServiceBridge;
import org.atriasoft.archidata.backup.BackupStart;
import org.atriasoft.archidata.backup.BackupState;
import org.atriasoft.archidata.backup.RetentionPolicy;
import org.atriasoft.archidata.cron.CronScheduler;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.migration.model.Migration;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoDatabase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import test.atriasoft.archidata.ConfigureDb;

/** {@link BackupService}: one backup at a time, a state and a list that carry nothing of the disk. */
public class TestBackupService {
	private static final String BASE_NAME = "service_base";
	private static final String COLLECTION = "backupServiceData";
	private static final long WAIT_SECONDS = 20;

	private static final DateTimeFormatter SEQUENCE = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS")
			.withZone(ZoneOffset.UTC);

	private Path folder;
	private String previousDataFolder;

	/** An engine that blocks in {@code store} until released, then fails or succeeds. */
	private static final class ControlledEngine extends BackupEngine {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicInteger calls = new AtomicInteger();
		final CountDownLatch deltaEntered = new CountDownLatch(1);
		final CountDownLatch deltaRelease = new CountDownLatch(1);
		final AtomicInteger deltaCalls = new AtomicInteger();
		final AtomicReference<String> deltaSequence = new AtomicReference<>();
		final AtomicReference<Date> deltaSince = new AtomicReference<>();
		IOException deltaFailure = null;
		final AtomicInteger cleanCalls = new AtomicInteger();
		final AtomicReference<RetentionPolicy> cleanPolicy = new AtomicReference<>();
		IOException cleanFailure = null;
		private final Throwable failure;

		ControlledEngine(final Path folder, final Throwable failure) {
			super(folder, BASE_NAME, EngineBackupType.JSON_EXTENDED);
			this.failure = failure;
		}

		@Override
		public Date store(final String sequence) throws IOException, DataAccessException {
			this.calls.incrementAndGet();
			this.entered.countDown();
			try {
				Assertions.assertTrue(this.release.await(WAIT_SECONDS, TimeUnit.SECONDS));
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException(ex);
			}
			if (this.failure instanceof final IOException ioFailure) {
				throw ioFailure;
			}
			if (this.failure instanceof final Error errorFailure) {
				throw errorFailure;
			}
			return new Date();
		}

		@Override
		public Date storeDeltaIfChanged(final String sequence, final Date since)
				throws IOException, DataAccessException {
			this.deltaSequence.set(sequence);
			this.deltaSince.set(since);
			this.deltaCalls.incrementAndGet();
			this.deltaEntered.countDown();
			try {
				Assertions.assertTrue(this.deltaRelease.await(WAIT_SECONDS, TimeUnit.SECONDS));
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IOException(ex);
			}
			if (this.deltaFailure != null) {
				throw this.deltaFailure;
			}
			return new Date();
		}

		@Override
		public List<Path> clean(final RetentionPolicy policy) throws IOException {
			this.cleanCalls.incrementAndGet();
			this.cleanPolicy.set(policy);
			if (this.cleanFailure != null) {
				throw this.cleanFailure;
			}
			return super.clean(policy);
		}

		void awaitEntered() throws InterruptedException {
			Assertions.assertTrue(this.entered.await(WAIT_SECONDS, TimeUnit.SECONDS));
		}
	}

	@BeforeAll
	public static void configureDb() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDb() throws IOException {
		ConfigureDb.clear();
	}

	@BeforeEach
	public void createFolder() throws IOException {
		this.folder = Files.createTempDirectory("test_backup_service_");
		this.previousDataFolder = ConfigBaseVariable.getMediaDataFolder();
	}

	@AfterEach
	public void deleteFolder() throws IOException {
		ConfigBaseVariable.setDataFolder(this.previousDataFolder);
		BackupTestTools.deleteRecursive(this.folder);
	}

	private static void awaitIdle(final BackupService service) throws Exception {
		final long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
		while (service.getState().running()) {
			Assertions.assertTrue(System.nanoTime() < limit, "the backup did not finish");
			Thread.sleep(5);
		}
	}

	private static void assertStarts(final BackupService service) {
		Assertions.assertEquals(BackupStart.STARTED, service.startAsync());
	}

	private Path archive(final String name) throws IOException {
		return Files.writeString(this.folder.resolve(name), "archive");
	}

	private BackupService readOnlyService() {
		return BackupServiceBridge.on(new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED));
	}

	private static List<String> entriesOf(final Path archive) throws IOException {
		final List<String> names = new ArrayList<>();
		try (InputStream in = new BufferedInputStream(Files.newInputStream(archive));
				GzipCompressorInputStream gzip = new GzipCompressorInputStream(in);
				TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
			TarArchiveEntry entry;
			while ((entry = tar.getNextEntry()) != null) {
				names.add(entry.getName());
			}
		}
		return names;
	}

	@Test
	public void testSimultaneousStartsRunOnlyOneBackup() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		final int callers = 8;
		final CyclicBarrier barrier = new CyclicBarrier(callers);
		final ExecutorService pool = Executors.newFixedThreadPool(callers);
		try {
			final List<Future<BackupStart>> results = new ArrayList<>();
			for (int iii = 0; iii < callers; iii++) {
				results.add(pool.submit(() -> {
					barrier.await(WAIT_SECONDS, TimeUnit.SECONDS);
					return service.startAsync();
				}));
			}
			int started = 0;
			for (final Future<BackupStart> result : results) {
				if (result.get(WAIT_SECONDS, TimeUnit.SECONDS) == BackupStart.STARTED) {
					started++;
				}
			}
			Assertions.assertEquals(1, started);
			engine.awaitEntered();
			Assertions.assertTrue(service.getState().running());
		} finally {
			pool.shutdownNow();
			engine.release.countDown();
		}
		awaitIdle(service);
		Assertions.assertEquals(1, engine.calls.get());
		Assertions.assertTrue(service.getState().lastAttempt().success());
		// The lock is free again.
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(2, engine.calls.get());
	}

	@Test
	public void testAStateReadAfterAStartSaysRunning() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		final BackupState during = service.getState();
		engine.release.countDown();
		Assertions.assertTrue(during.running());
		Assertions.assertNull(during.lastAttempt());
		awaitIdle(service);
	}

	@Test
	public void testNightlyTaskDuringAManualStartIsSkipped() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		engine.awaitEntered();
		// The nightly task goes through the same door, and returns at once.
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING,
				Assertions.assertTimeoutPreemptively(Duration.ofSeconds(WAIT_SECONDS), service::startAsync));
		Assertions.assertTrue(service.getState().running());
		engine.release.countDown();
		awaitIdle(service);
		Assertions.assertEquals(1, engine.calls.get());
	}

	/** The cron has a single consumer thread: a dump held there would stop every other task of the application. */
	@Test
	public void testTheNightlyTaskDoesNotHoldTheCronThread() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		final CronScheduler scheduler = new CronScheduler();
		scheduler.setDaemonThreads(true);
		scheduler.disableGracePeriodMinutes();
		scheduler.start();
		try {
			scheduler.addTask(service::startAsync);
			engine.awaitEntered();
			final CountDownLatch nextTask = new CountDownLatch(1);
			scheduler.addTask(nextTask::countDown);
			Assertions.assertTrue(nextTask.await(5, TimeUnit.SECONDS), "the backup holds the cron thread");
		} finally {
			engine.release.countDown();
			scheduler.stop();
		}
		awaitIdle(service);
	}

	private void assertFailureIsReflected(final Throwable failure) throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, failure);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		final BackupState state = service.getState();
		Assertions.assertFalse(state.running());
		Assertions.assertFalse(state.lastAttempt().success());
		Assertions.assertNotNull(state.lastAttempt().date());
		final String shown = state.toString();
		Assertions.assertFalse(shown.contains("/secret"), shown);
		Assertions.assertFalse(shown.contains(this.folder.toString()), shown);
		// A failure does not keep the lock.
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(2, engine.calls.get());
	}

	@Test
	public void testEngineFailureIsReflectedWithoutItsMessage() throws Exception {
		assertFailureIsReflected(new IOException("/secret/backup/folder: disk full"));
	}

	@Test
	public void testErrorIsCaughtToo() throws Exception {
		assertFailureIsReflected(new OutOfMemoryError("/secret/backup/folder"));
	}

	@Test
	public void testArchivesBeingWrittenAreNotListed() throws Exception {
		archive("." + BASE_NAME + "_2026-11-02_10:00:00.000_full.tar.gz_tmp");
		archive("." + BASE_NAME + "_2026-11-02_10:00:00.000_full.tar.gz.sha256_tmp");
		archive(BASE_NAME + "_2026-11-01_10:00:00.000_full.tar.gz");
		final List<BackupArchive> archives = readOnlyService().listArchives();
		Assertions.assertEquals(1, archives.size());
		Assertions.assertEquals(Instant.parse("2026-11-01T10:00:00Z"), archives.get(0).date());
	}

	/**
	 * What this proves: a name {@code BackupEngine.listBackupFiles} can not parse does not break the list. The date is
	 * not validated: the parser is lenient, and an impossible day that it accepts is listed at the day it resolves to.
	 */
	@Test
	public void testNamesTheEngineCanNotParseAreLeftOut() throws Exception {
		archive(BASE_NAME + "_garbage.tar.gz");
		archive(BASE_NAME + "_2026-13-45_full.tar.gz");
		archive(BASE_NAME + "_.tar.gz");
		archive(BASE_NAME + "_2026-11-02.tar.gz");
		archive(BASE_NAME + "_2026-02-30.tar.gz");
		final List<BackupArchive> archives = readOnlyService().listArchives();
		Assertions.assertEquals(List.of(Instant.parse("2026-11-02T00:00:00Z"), Instant.parse("2026-02-28T00:00:00Z")),
				archives.stream().map(BackupArchive::date).toList());
	}

	@Test
	public void testFilesOfAnotherBaseAreLeftOut() throws Exception {
		archive("other_base_2026-11-01_full.tar.gz");
		archive(BASE_NAME + "_x_2026-11-01_full.tar.gz");
		archive("readme.txt");
		archive(BASE_NAME + "_2026-11-02.tar.gz");
		final List<BackupArchive> archives = readOnlyService().listArchives();
		Assertions.assertEquals(1, archives.size());
		Assertions.assertEquals(Instant.parse("2026-11-02T00:00:00Z"), archives.get(0).date());
	}

	/** These names are valid: the engine keeps them, and only the service leaves them out. */
	@Test
	public void testOnlyRegularFilesAreListed() throws Exception {
		Files.createDirectory(this.folder.resolve(BASE_NAME + "_2026-11-03.tar.gz"));
		Files.createSymbolicLink(this.folder.resolve(BASE_NAME + "_2026-11-04.tar.gz"), archive("target"));
		archive(BASE_NAME + "_2026-11-02.tar.gz");
		final List<BackupArchive> archives = readOnlyService().listArchives();
		Assertions.assertEquals(1, archives.size());
		Assertions.assertEquals(Instant.parse("2026-11-02T00:00:00Z"), archives.get(0).date());
	}

	@Test
	public void testListGivesTypeSizeChecksumMostRecentFirst() throws Exception {
		final Path full = archive(BASE_NAME + "_2026-11-01_00:00:00.000_full.tar.gz");
		Files.writeString(BackupEngine.checksumPath(full), "sum");
		archive(BASE_NAME + "_2026-11-01_12:30:15.250_delta.tar.gz");
		archive(BASE_NAME + "_2026-11-03_full_partial.tar.gz");
		// A name of an older format sorts by its date, not by its text.
		archive(BASE_NAME + "_2026-11-01_12-00-00_full.tar.gz");
		final List<BackupArchive> archives = readOnlyService().listArchives();
		Assertions.assertEquals(
				List.of(new BackupArchive(Instant.parse("2026-11-03T00:00:00Z"), 7, BackupArchiveType.PARTIAL, false),
						new BackupArchive(Instant.parse("2026-11-01T12:30:15.250Z"), 7, BackupArchiveType.DELTA, false),
						new BackupArchive(Instant.parse("2026-11-01T12:00:00Z"), 7, BackupArchiveType.FULL, false),
						new BackupArchive(Instant.parse("2026-11-01T00:00:00Z"), 7, BackupArchiveType.FULL, true)),
				archives);
	}

	@Test
	public void testAFolderThatDisappearedIsAnErrorNotAnEmptyList() throws Exception {
		final BackupService service = BackupServiceBridge
				.on(new BackupEngine(this.folder.resolve("absent"), BASE_NAME, EngineBackupType.JSON_EXTENDED));
		Assertions.assertThrows(NoSuchFileException.class, service::listArchives);
		Assertions.assertThrows(NoSuchFileException.class, service::getState);
	}

	@Test
	public void testRealBackupIsDatabaseOnlyAndWritesWhatARestoreNeeds() throws Exception {
		// The media folder has a file: a backup that took the media would hold a data/ entry.
		final Path media = this.folder.resolve("media");
		Files.createDirectories(media);
		Files.writeString(media.resolve("picture.jpg"), "private");
		ConfigBaseVariable.setDataFolder(media.toString());
		final MongoDatabase db = ConfigureDb.da.getInterface().getDatabase();
		db.getCollection(COLLECTION).drop();
		db.getCollection(COLLECTION).insertOne(new Document("name", "one"));

		final Path store = this.folder.resolve("deep").resolve("store");
		final BackupService service = BackupServiceBridge.on(store, BASE_NAME, List.of(Migration.class),
				List.of(COLLECTION, "counters"));
		Assertions.assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store)));
		final Instant before = Instant.now().minusMillis(1);
		assertStarts(service);
		awaitIdle(service);

		final BackupState state = service.getState();
		final BackupAttempt attempt = state.lastAttempt();
		Assertions.assertTrue(attempt.success());
		Assertions.assertEquals(1, state.archives().size());
		final BackupArchive archive = state.archives().get(0);
		Assertions.assertEquals(attempt.date(), archive.date());
		Assertions.assertFalse(archive.date().isBefore(before));
		Assertions.assertEquals(BackupArchiveType.FULL, archive.type());
		Assertions.assertTrue(archive.hasChecksum());
		Assertions.assertTrue(archive.size() > 0);

		final List<Path> onDisk;
		try (Stream<Path> files = Files.list(store)) {
			onDisk = files.filter(file -> file.getFileName().toString().endsWith(".tar.gz")).toList();
		}
		Assertions.assertEquals(1, onDisk.size());
		// In UTC and of the format the engine parses, so getLastFullBackupDate reads the exact date back.
		Assertions.assertEquals(attempt.date(),
				new BackupEngine(store, BASE_NAME, EngineBackupType.JSON_EXTENDED).getLastFullBackupDate().toInstant());
		final List<String> entries = entriesOf(onDisk.get(0));
		Assertions.assertEquals(new HashSet<>(entries).size(), entries.size(),
				"a collection written twice: " + entries);
		Assertions.assertEquals(Set.of(COLLECTION + ".json", "counters.json", "KAR_migration.json"),
				new HashSet<>(entries));
		final Map<String, String> content = BackupTestTools.extractTarGzToMap(onDisk.get(0));
		Assertions.assertEquals(1, BackupTestTools.countLines(content.get(COLLECTION + ".json")));
	}

	@Test
	public void testAConstructionDeletesWhatAnInterruptedBackupOfThisBaseLeft() throws Exception {
		final List<Path> leftovers = List.of(//
				archive("." + BASE_NAME + "_2026-11-02_10:00:00.000_full.tar.gz_tmp"), //
				archive("." + BASE_NAME + "_2026-11-02_10:00:00.000_full.tar.gz.sha256_tmp"), //
				archive("." + BASE_NAME + "_2026-11-02_delta.tar.gz_tmp"));
		final List<Path> kept = new ArrayList<>(List.of(//
				// Finished archives.
				archive(BASE_NAME + "_2026-11-02_full.tar.gz"), //
				archive(BASE_NAME + "_2026-11-02_full.tar.gz.sha256"), //
				// Another base, one of them starting with the same text.
				archive(".other_2026-11-02_full.tar.gz_tmp"), //
				archive("." + BASE_NAME + "_x_2026-11-02_full.tar.gz_tmp"), //
				// Not the exact form of the name.
				archive(BASE_NAME + "_2026-11-02_full.tar.gz_tmp"), //
				archive("." + BASE_NAME + "_2026-11-02_full.tar.gz"), //
				archive("." + BASE_NAME + "_2026-11-02_full.tar.gz_tmp2"), //
				archive("." + BASE_NAME + "_2026-11-02_tmp"), //
				archive("." + BASE_NAME + "_notes.tar.gz_tmp")));
		// Only a regular file goes: neither a folder nor what a link points to.
		kept.add(Files.createDirectory(this.folder.resolve("." + BASE_NAME + "_2026-11-03.tar.gz_tmp")));
		final Path target = archive("target");
		kept.add(target);
		kept.add(Files.createSymbolicLink(this.folder.resolve("." + BASE_NAME + "_2026-11-04.tar.gz_tmp"), target));

		new BackupService(this.folder, BASE_NAME, List.of(), List.of());

		for (final Path file : leftovers) {
			Assertions.assertFalse(Files.exists(file), file.toString());
		}
		for (final Path file : kept) {
			Assertions.assertTrue(Files.exists(file, LinkOption.NOFOLLOW_LINKS), file.toString());
		}
	}

	private void archive(final String name, final int size) throws IOException {
		Files.write(this.folder.resolve(name), new byte[size]);
	}

	private void assertRefused(final ControlledEngine engine, final BackupService service) throws Exception {
		Assertions.assertEquals(BackupStart.INSUFFICIENT_SPACE, service.startAsync());
		Assertions.assertEquals(0, engine.calls.get(), "nothing is written");
		final BackupState state = service.getState();
		Assertions.assertFalse(state.running(), "a refusal does not keep the lock");
		Assertions.assertFalse(state.lastAttempt().success());
	}

	@Test
	public void testStartNeedsTwiceTheLastFullArchivePlusTheMargin() throws Exception {
		// The size that counts is the one of the most recent full archive, whatever is around it.
		archive(BASE_NAME + "_2026-10-01_full.tar.gz", 5000);
		archive(BASE_NAME + "_2026-10-02_full.tar.gz", 100);
		archive(BASE_NAME + "_2026-10-03_delta.tar.gz", 7000);
		archive(BASE_NAME + "_2026-10-04_full_partial.tar.gz", 9000);
		final long needed = 2 * 100 + BackupService.SPACE_MARGIN_BYTES;
		final AtomicLong usable = new AtomicLong(needed - 1);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine, usable::get);
		assertRefused(engine, service);
		usable.set(needed);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.calls.get());
		Assertions.assertTrue(service.getState().lastAttempt().success());
	}

	@Test
	public void testAFirstBackupNeedsTheMargin() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		assertRefused(engine, BackupServiceBridge.on(engine, () -> BackupService.SPACE_MARGIN_BYTES - 1));
	}

	/** The cron task drops the answer of startAsync: the state is all that is left. */
	@Test
	public void testANightlyTaskRefusedIsAFailedAttempt() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine, () -> 0);
		final CronScheduler scheduler = new CronScheduler();
		scheduler.setDaemonThreads(true);
		scheduler.disableGracePeriodMinutes();
		scheduler.start();
		try {
			scheduler.addTask(service::startAsync);
			final long limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
			while (service.getState().lastAttempt() == null) {
				Assertions.assertTrue(System.nanoTime() < limit, "the task did not run");
				Thread.sleep(5);
			}
		} finally {
			scheduler.stop();
		}
		Assertions.assertEquals(0, engine.calls.get());
		Assertions.assertFalse(service.getState().running());
		Assertions.assertFalse(service.getState().lastAttempt().success());
	}

	@Test
	public void testARunningBackupIsNotASpaceProblem() throws Exception {
		final AtomicLong usable = new AtomicLong(BackupServiceBridge.LOTS_OF_SPACE);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine, usable::get);
		assertStarts(service);
		engine.awaitEntered();
		usable.set(0);
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startAsync());
		engine.release.countDown();
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success(),
				"the running backup is not recorded as refused");
	}

	/** A volume no longer mounted: the engine would create the folder again on what lies under it. */
	@Test
	public void testAStartThatCanNotBeCheckedIsAFailedAttempt() throws Exception {
		final Path gone = this.folder.resolve("gone");
		Files.createDirectory(gone);
		final ControlledEngine engine = new ControlledEngine(gone, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		Files.delete(gone);
		Assertions.assertThrows(UncheckedIOException.class, service::startAsync);
		Assertions.assertEquals(0, engine.calls.get());
		Assertions.assertFalse(Files.exists(gone));
		Files.createDirectory(gone);
		Assertions.assertFalse(service.getState().running(), "the lock is released");
		Assertions.assertFalse(service.getState().lastAttempt().success());
	}

	@Test
	public void testThePublicConstructorReadsTheRealDisk() throws Exception {
		final long free = Files.getFileStore(this.folder).getUsableSpace();
		Assumptions.assumeTrue(free > 2 * BackupService.SPACE_MARGIN_BYTES, "not enough space to run a backup here");
		final BackupService service = new BackupService(this.folder, BASE_NAME, List.of(), List.of());
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success());
	}

	/** A class whose collection a restore refuses. */
	@Collection("system.js")
	private static final class ClassOfAForbiddenCollection {}

	/** Refused at the start of the server, not the day the archive is needed. */
	@Test
	public void testACollectionARestoreRefusesFailsTheConstruction() throws Exception {
		final Path store = this.folder.resolve("store");
		for (final String name : List.of("system.js", "", ".hidden", "a/b", "a$b", "a".repeat(121), "events__2")) {
			Assertions.assertThrows(IOException.class,
					() -> new BackupService(store, BASE_NAME, List.of(), List.of(name)), name);
		}
		Assertions.assertThrows(IOException.class,
				() -> new BackupService(store, BASE_NAME, List.of(ClassOfAForbiddenCollection.class), List.of()));
		Assertions.assertFalse(Files.exists(store), "nothing is created for a refused configuration");
	}

	/** Its archives are in clear: a folder made by hand keeps no right for the others. */
	@Test
	public void testAnExistingFolderIsMadePrivate() throws Exception {
		final Path store = Files.createDirectory(this.folder.resolve("store"),
				PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
		Assertions.assertNotNull(new BackupService(store, BASE_NAME, List.of(), List.of()));
		Assertions.assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(store)));
	}

	// ===== Retention =====

	@Test
	public void testAFullAppliesTheRetention() throws Exception {
		final List<Path> old = new ArrayList<>();
		old.add(archive(BASE_NAME + "_2015-01-01_00:00:00.000_full.tar.gz"));
		old.add(archive(BASE_NAME + "_2015-03-01_00:00:00.000_delta.tar.gz"));
		old.add(archive(BASE_NAME + "_2015-06-01_00:00:00.000_full.tar.gz"));
		old.add(BackupEngine.checksumPath(old.get(0)));
		old.add(BackupEngine.checksumPath(old.get(1)));
		Files.writeString(old.get(3), "sum");
		Files.writeString(old.get(4), "sum");
		final List<Path> foreign = List.of(//
				archive("other_2015-01-01_00:00:00.000_full.tar.gz"), //
				archive("other_2015-01-01_00:00:00.000_full.tar.gz.sha256"), //
				archive("other_2015-01-01_00:15:00.000_delta.tar.gz"), //
				archive(BASE_NAME + "_x_2015-01-01_00:00:00.000_full.tar.gz"), //
				archive(BASE_NAME + "_manual.tar.gz"), //
				archive("readme.txt"));

		final BackupService service = BackupServiceBridge.on(this.folder, BASE_NAME, List.of(), List.of());
		assertStarts(service);
		awaitIdle(service);

		for (final Path file : old) {
			Assertions.assertFalse(Files.exists(file), file.toString());
		}
		for (final Path file : foreign) {
			Assertions.assertTrue(Files.exists(file), file.toString());
		}
		final BackupState state = service.getState();
		Assertions.assertTrue(state.lastAttempt().success());
		Assertions.assertEquals(List.of(state.lastAttempt().date()),
				state.archives().stream().map(BackupArchive::date).toList());
	}

	@Test
	public void testTheDeltasOfTheLastFullGoWithTheNextOne() throws Exception {
		final Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
		final Path older = archive(BASE_NAME + "_" + SEQUENCE.format(yesterday) + "_full.tar.gz");
		final Path delta = archive(BASE_NAME + "_" + SEQUENCE.format(yesterday.plusSeconds(900)) + "_delta.tar.gz");
		final Path sidecar = BackupEngine.checksumPath(delta);
		Files.writeString(sidecar, "sum");
		final Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);
		final Path recentDelta = archive(BASE_NAME + "_" + SEQUENCE.format(recent) + "_delta.tar.gz");

		final BackupService service = BackupServiceBridge.on(this.folder, BASE_NAME, List.of(), List.of());
		assertStarts(service);
		awaitIdle(service);

		Assertions.assertFalse(Files.exists(delta), "a delta before the last full can not be restored any more");
		Assertions.assertFalse(Files.exists(sidecar));
		Assertions.assertFalse(Files.exists(recentDelta), "keepAllDays keeps no delta");
		Assertions.assertTrue(Files.exists(older), "a full follows the zones of the policy");
		Assertions.assertEquals(0, countOf(service.getState(), BackupArchiveType.DELTA));
	}

	/** After a clock that jumped forward: this delta comes before the new full, whatever its name says. */
	@Test
	public void testAFullRemovesADeltaDatedAfterIt() throws Exception {
		final Path future = archive(
				BASE_NAME + "_" + SEQUENCE.format(Instant.now().plus(1, ChronoUnit.DAYS)) + "_delta.tar.gz");
		final Path sidecar = BackupEngine.checksumPath(future);
		Files.writeString(sidecar, "sum");
		final BackupService service = BackupServiceBridge.on(this.folder, BASE_NAME, List.of(), List.of());
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertFalse(Files.exists(future));
		Assertions.assertFalse(Files.exists(sidecar));
		Assertions.assertEquals(0, countOf(service.getState(), BackupArchiveType.DELTA));
	}

	@Test
	public void testAFailedFullLeavesADeltaDatedAfterIt() throws Exception {
		archive(FULL_NAME);
		final Path future = archive(
				BASE_NAME + "_" + SEQUENCE.format(Instant.now().plus(1, ChronoUnit.DAYS)) + "_delta.tar.gz");
		final ControlledEngine engine = new ControlledEngine(this.folder, new IOException("disk full"));
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(Files.exists(future));
	}

	/** The name of a delta says which full it starts from: the chain can tell it from a delta of another full. */
	@Test
	public void testADeltaNamesItsFull() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(engine.deltaSequence.get().endsWith("_base20261101000000000_delta"),
				engine.deltaSequence.get());
	}

	@Test
	public void testTheRetentionOfTheServiceCanBeChosen() throws Exception {
		final RetentionPolicy policy = new RetentionPolicy(1, 1, 1);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine, policy);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(policy, engine.cleanPolicy.get());
		Assertions.assertThrows(NullPointerException.class,
				() -> BackupServiceBridge.on(engine, (RetentionPolicy) null));
	}

	@Test
	public void testTheDefaultRetentionIsTheOneOfThePolicy() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(new RetentionPolicy(), engine.cleanPolicy.get());
		Assertions.assertNotNull(
				new BackupService(this.folder, BASE_NAME, List.of(), List.of(), new RetentionPolicy(1, 1, 1)));
	}

	@Test
	public void testAFailedFullDeletesNothing() throws Exception {
		final Path old = archive(BASE_NAME + "_2015-01-01_00:00:00.000_full.tar.gz");
		final Path last = archive(BASE_NAME + "_2026-11-01_00:00:00.000_full.tar.gz");
		for (final Throwable failure : List.of(new IOException("disk full"), new OutOfMemoryError("heap"))) {
			final ControlledEngine engine = new ControlledEngine(this.folder, failure);
			engine.release.countDown();
			final BackupService service = BackupServiceBridge.on(engine);
			assertStarts(service);
			awaitIdle(service);
			Assertions.assertFalse(service.getState().lastAttempt().success());
			Assertions.assertEquals(0, engine.cleanCalls.get());
		}
		Assertions.assertTrue(Files.exists(old));
		Assertions.assertTrue(Files.exists(last));
	}

	@Test
	public void testARefusedFullDeletesNothing() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		assertRefused(engine, BackupServiceBridge.on(engine, () -> 0));
		Assertions.assertEquals(0, engine.cleanCalls.get());
	}

	@Test
	public void testADeltaDeletesNothing() throws Exception {
		final Path old = archive(BASE_NAME + "_2015-01-01_00:00:00.000_delta.tar.gz");
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.deltaCalls.get());
		Assertions.assertEquals(0, engine.cleanCalls.get());
		Assertions.assertTrue(Files.exists(old));
	}

	/** A full started by a delta, or that waited behind one, is a full. */
	@Test
	public void testAFullReachedThroughADeltaAppliesTheRetention() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.cleanCalls.get());

		archive(FULL_NAME);
		final ControlledEngine waited = new ControlledEngine(this.folder, null);
		waited.release.countDown();
		final BackupService other = BackupServiceBridge.on(waited);
		assertDeltaStarts(other);
		assertStarts(other);
		waited.deltaRelease.countDown();
		awaitIdle(other);
		Assertions.assertEquals(1, waited.calls.get());
		Assertions.assertEquals(1, waited.cleanCalls.get());
	}

	@Test
	public void testAFailedRetentionLeavesTheBackupASuccess() throws Exception {
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		engine.cleanFailure = new IOException("/secret/backup/folder: permission denied");
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.cleanCalls.get());
		Assertions.assertTrue(service.getState().lastAttempt().success());
		// No lock kept, and the next full tries again.
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(2, engine.cleanCalls.get());
	}

	/** {@code store} writes nothing here: the archive that stays is the one on the disk, far past the last zone. */
	@Test
	public void testTheLastFullIsNeverDeleted() throws Exception {
		final Path older = archive(BASE_NAME + "_2015-01-01_00:00:00.000_full.tar.gz");
		final Path last = archive(BASE_NAME + "_2015-01-02_00:00:00.000_full.tar.gz");
		final Path sidecar = BackupEngine.checksumPath(last);
		Files.writeString(sidecar, "sum");
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine, new RetentionPolicy(0, 0, 0));
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertFalse(Files.exists(older));
		Assertions.assertTrue(Files.exists(last));
		Assertions.assertTrue(Files.exists(sidecar));
	}

	// ===== Delta =====

	private static final String FULL_NAME = BASE_NAME + "_2026-11-01_00:00:00.000_full.tar.gz";
	private static final Instant FULL_DATE = Instant.parse("2026-11-01T00:00:00Z");

	private static void assertDeltaStarts(final BackupService service) {
		Assertions.assertEquals(BackupStart.STARTED, service.startDeltaAsync());
	}

	private static long countOf(final BackupState state, final BackupArchiveType type) {
		return state.archives().stream().filter(archive -> archive.type() == type).count();
	}

	@Test
	public void testADeltaStartsFromTheFullFoundOnTheDisk() throws Exception {
		archive(BASE_NAME + "_2026-10-31_00:00:00.000_full.tar.gz");
		archive(FULL_NAME);
		archive(BASE_NAME + "_2026-11-01_00:15:00.000_delta.tar.gz");
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final Instant before = Instant.now().minusMillis(1);

		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);

		Assertions.assertEquals(1, engine.deltaCalls.get());
		Assertions.assertEquals(0, engine.calls.get());
		Assertions.assertEquals(Date.from(FULL_DATE.minusMillis(BackupServiceBridge.DELTA_MARGIN_MILLIS)),
				engine.deltaSince.get());
		final BackupAttempt attempt = service.getState().lastAttempt();
		Assertions.assertTrue(attempt.success());
		Assertions.assertFalse(attempt.date().isBefore(before));
		// In UTC and of the format the engine parses, as the name of a full.
		Assertions.assertEquals(attempt.date(),
				BackupEngine.parseDateTimeFromSequence(engine.deltaSequence.get()).toInstant(ZoneOffset.UTC));
		Assertions.assertTrue(engine.deltaSequence.get().endsWith("_delta"));
	}

	/** The lifetime of a Mongo transaction, 60 s by default, has to fit in it. */
	@Test
	public void testADeltaLooksBeyondATransactionOpenDuringTheFull() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(Date.from(Instant.parse("2026-10-31T23:58:00Z")), engine.deltaSince.get());
	}

	/** Nothing is kept in memory: a new service reads the same reference, and the chain stays one. */
	@Test
	public void testARestartFindsTheReferenceAgain() throws Exception {
		final BackupEngine first = new BackupEngine(this.folder, BASE_NAME, EngineBackupType.JSON_EXTENDED);
		first.setEnableStoreOrRestoreData(false);
		first.store("2026-11-01_03:00:00.000_full");
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final BackupService afterRestart = BackupServiceBridge.on(engine);
		Assertions.assertNull(afterRestart.getState().lastAttempt());
		assertDeltaStarts(afterRestart);
		awaitIdle(afterRestart);
		Assertions.assertEquals(
				Date.from(Instant.parse("2026-11-01T03:00:00Z").minusMillis(BackupServiceBridge.DELTA_MARGIN_MILLIS)),
				engine.deltaSince.get());
	}

	@Test
	public void testADeltaDuringAFullIsSkippedWithoutAWarning() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		engine.awaitEntered();
		final Logger logger = (Logger) LoggerFactory.getLogger(BackupService.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startDeltaAsync());
		} finally {
			logger.detachAppender(appender);
		}
		Assertions.assertTrue(appender.list.stream().noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN)),
				appender.list.toString());
		Assertions.assertNull(service.getState().lastAttempt(), "a turn skipped is not an attempt");
		engine.release.countDown();
		awaitIdle(service);
		Assertions.assertEquals(0, engine.deltaCalls.get());
	}

	@Test
	public void testADeltaDuringADeltaIsSkipped() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startDeltaAsync());
		engine.deltaRelease.countDown();
		awaitIdle(service);
		Assertions.assertEquals(1, engine.deltaCalls.get());
	}

	/** The scheduler does not order two tasks of the same minute: at midnight the delta can go first. */
	@Test
	public void testAFullAskedDuringADeltaRunsRightAfterIt() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		Assertions.assertTrue(engine.deltaEntered.await(WAIT_SECONDS, TimeUnit.SECONDS));

		assertStarts(service);
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startAsync(), "one full waits, not two");
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startDeltaAsync());
		Assertions.assertEquals(0, engine.calls.get(), "the full waits for the delta");

		engine.deltaRelease.countDown();
		engine.awaitEntered();
		Assertions.assertTrue(service.getState().running(), "no gap between the two");
		Assertions.assertEquals(BackupStart.ALREADY_RUNNING, service.startDeltaAsync());
		engine.release.countDown();
		awaitIdle(service);
		Assertions.assertEquals(1, engine.calls.get());
		Assertions.assertEquals(1, engine.deltaCalls.get());
	}

	@Test
	public void testAFullWaitingBehindADeltaIsStillCheckedForSpace() throws Exception {
		archive(FULL_NAME);
		final AtomicLong usable = new AtomicLong(BackupServiceBridge.LOTS_OF_SPACE);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine, usable::get);
		assertDeltaStarts(service);
		usable.set(0);
		Assertions.assertEquals(BackupStart.INSUFFICIENT_SPACE, service.startAsync());
		engine.deltaRelease.countDown();
		awaitIdle(service);
		Assertions.assertEquals(0, engine.calls.get());
	}

	/** There is nothing a delta could be restored on: without this, the first backup would wait for the night. */
	@Test
	public void testWithoutAFullADeltaStartsAFull() throws Exception {
		// A partial archive and a delta are no base.
		archive(BASE_NAME + "_2026-11-01_full_partial.tar.gz");
		archive(BASE_NAME + "_2026-11-01_00:15:00.000_delta.tar.gz");
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.calls.get());
		Assertions.assertEquals(0, engine.deltaCalls.get());
		Assertions.assertTrue(service.getState().lastAttempt().success());
	}

	/** A volume no longer mounted is no reason for a full: the failure is the one of a delta, and the folder stays gone. */
	@Test
	public void testAFolderThatDisappearedFailsTheDeltaWithoutAFull() throws Exception {
		final Path gone = this.folder.resolve("gone");
		Files.createDirectory(gone);
		final ControlledEngine engine = new ControlledEngine(gone, null);
		engine.release.countDown();
		engine.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(engine);
		Files.delete(gone);
		Assertions.assertThrows(UncheckedIOException.class, service::startDeltaAsync);
		Assertions.assertEquals(0, engine.calls.get());
		Assertions.assertFalse(Files.exists(gone));

		Files.createDirectory(gone);
		Assertions.assertFalse(service.getState().lastAttempt().success());
		Files.writeString(gone.resolve(FULL_NAME), "archive");
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success(), "this was not the failure of a full");
	}

	@Test
	public void testADeltaFollowsTheSpaceCheckOfAFull() throws Exception {
		archive(FULL_NAME, 100);
		final long needed = 2 * 100 + BackupService.SPACE_MARGIN_BYTES;
		final AtomicLong usable = new AtomicLong(needed - 1);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(engine, usable::get);
		Assertions.assertEquals(BackupStart.INSUFFICIENT_SPACE, service.startDeltaAsync());
		Assertions.assertEquals(0, engine.deltaCalls.get());
		Assertions.assertFalse(service.getState().lastAttempt().success());
		Assertions.assertFalse(service.getState().running());
		usable.set(needed);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(1, engine.deltaCalls.get());
	}

	@Test
	public void testAFailedDeltaIsRecordedAndKeepsNoLock() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.deltaRelease.countDown();
		engine.deltaFailure = new IOException("/secret/backup/folder: disk full");
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertFalse(service.getState().lastAttempt().success());
		Assertions.assertFalse(service.getState().toString().contains("/secret"));
		engine.deltaFailure = null;
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success());
	}

	/** 96 deltas a day: without this, a full that failed at night is out of the state 15 minutes later. */
	@Test
	public void testAWorkingDeltaDoesNotHideAFailedFull() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine failing = new ControlledEngine(this.folder, new IOException("disk full"));
		failing.release.countDown();
		failing.deltaRelease.countDown();
		final BackupService service = BackupServiceBridge.on(failing);
		assertStarts(service);
		awaitIdle(service);
		final BackupAttempt failed = service.getState().lastAttempt();
		Assertions.assertFalse(failed.success());
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(failed, service.getState().lastAttempt());
		Assertions.assertEquals(1, failing.deltaCalls.get());
	}

	@Test
	public void testAFailedFullStaysShownOverAFailedDelta() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, new IOException("disk full"));
		engine.release.countDown();
		engine.deltaRelease.countDown();
		engine.deltaFailure = new IOException("disk full");
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		final BackupAttempt failed = service.getState().lastAttempt();
		Thread.sleep(2);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(failed, service.getState().lastAttempt());
	}

	/** Without a failed full, the state gives the most recent attempt, whichever it is. */
	@Test
	public void testTheMostRecentAttemptIsShownOnceTheFullsWork() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		engine.release.countDown();
		engine.deltaRelease.countDown();
		engine.deltaFailure = new IOException("disk full");
		final BackupService service = BackupServiceBridge.on(engine);
		assertStarts(service);
		awaitIdle(service);
		final BackupAttempt full = service.getState().lastAttempt();
		Assertions.assertTrue(full.success());
		Thread.sleep(2);
		assertDeltaStarts(service);
		awaitIdle(service);
		final BackupAttempt delta = service.getState().lastAttempt();
		Assertions.assertFalse(delta.success());
		Assertions.assertTrue(delta.date().isAfter(full.date()));
		Thread.sleep(2);
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success());
	}

	/** Such a delta would hold nothing, and look fine. */
	@Test
	public void testAFullFromTheFutureFailsTheDelta() throws Exception {
		archive(BASE_NAME + "_2100-01-01_00:00:00.000_full.tar.gz");
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(0, engine.deltaCalls.get());
		Assertions.assertFalse(service.getState().lastAttempt().success());
	}

	@Test
	public void testTheDeltaTaskDoesNotHoldTheCronThread() throws Exception {
		archive(FULL_NAME);
		final ControlledEngine engine = new ControlledEngine(this.folder, null);
		final BackupService service = BackupServiceBridge.on(engine);
		final CronScheduler scheduler = new CronScheduler();
		scheduler.setDaemonThreads(true);
		scheduler.disableGracePeriodMinutes();
		scheduler.start();
		try {
			scheduler.addTask(service::startDeltaAsync);
			Assertions.assertTrue(engine.deltaEntered.await(WAIT_SECONDS, TimeUnit.SECONDS));
			final CountDownLatch nextTask = new CountDownLatch(1);
			scheduler.addTask(nextTask::countDown);
			Assertions.assertTrue(nextTask.await(5, TimeUnit.SECONDS), "the delta holds the cron thread");
		} finally {
			engine.deltaRelease.countDown();
			scheduler.stop();
		}
		awaitIdle(service);
	}

	private static List<Path> archivesIn(final Path store) throws IOException {
		try (Stream<Path> files = Files.list(store)) {
			return files.filter(file -> file.getFileName().toString().endsWith(".tar.gz")).sorted().toList();
		}
	}

	/**
	 * The whole way on Mongo, from the service to {@code restoreLatest}: dated, undated and counter documents, no media,
	 * no file for a delta with nothing new. What the delta can not carry stays in the same test: a document removed comes
	 * back.
	 */
	@Test
	public void testFullThenDeltaRestoreTheLatestState() throws Exception {
		final Path media = this.folder.resolve("media");
		Files.createDirectories(media);
		ConfigBaseVariable.setDataFolder(media.toString());
		final MongoDatabase db = ConfigureDb.da.getInterface().getDatabase();
		final String undated = COLLECTION + "Undated";
		final List<String> collections = List.of(COLLECTION, undated, "counters", "KAR_migration");
		for (final String name : collections) {
			db.getCollection(name).drop();
		}
		final Date old = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
		db.getCollection(COLLECTION).insertMany(List.of(//
				new Document("_id", "same").append("value", 1).append("createdAt", old).append("updatedAt", old), //
				new Document("_id", "updated").append("value", 1).append("createdAt", old).append("updatedAt", old), //
				new Document("_id", "softDeleted").append("value", 1).append("deleted", false).append("createdAt", old)
						.append("updatedAt", old), //
				new Document("_id", "hardDeleted").append("value", 1).append("createdAt", old).append("updatedAt",
						old)));
		db.getCollection(undated).insertOne(new Document("_id", "undated").append("value", 1));
		db.getCollection("counters").insertOne(new Document("_id", "sequence").append("value", 10L));

		final Path store = this.folder.resolve("store");
		final BackupService service = BackupServiceBridge.on(store, BASE_NAME, List.of(Migration.class),
				List.of(COLLECTION, undated));
		assertStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success());

		// A file is written after the full: a delta that took the media would hold a data/ entry.
		Files.writeString(media.resolve("picture.jpg"), "private");
		final Date now = new Date();
		db.getCollection(COLLECTION).updateOne(new Document("_id", "updated"),
				new Document("$set", new Document("value", 2).append("updatedAt", now)));
		db.getCollection(COLLECTION).updateOne(new Document("_id", "softDeleted"),
				new Document("$set", new Document("deleted", true).append("updatedAt", now)));
		db.getCollection(COLLECTION).deleteOne(new Document("_id", "hardDeleted"));
		db.getCollection(COLLECTION).insertOne(
				new Document("_id", "created").append("value", 2).append("createdAt", now).append("updatedAt", now));
		db.getCollection(undated).updateOne(new Document("_id", "undated"),
				new Document("$set", new Document("value", 2)));
		db.getCollection("counters").updateOne(new Document("_id", "sequence"),
				new Document("$set", new Document("value", 11L)));

		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertTrue(service.getState().lastAttempt().success());
		BackupState state = service.getState();
		Assertions.assertEquals(1, countOf(state, BackupArchiveType.DELTA));
		final Path delta = archivesIn(store).stream()
				.filter(file -> file.getFileName().toString().endsWith("_delta.tar.gz")).findFirst().orElseThrow();
		Assertions.assertEquals(Set.of(COLLECTION + ".json", undated + ".json", "counters.json", "KAR_migration.json"),
				new HashSet<>(entriesOf(delta)));
		Assertions.assertTrue(
				BackupTestTools.countLines(BackupTestTools.extractTarGzToMap(delta).get(COLLECTION + ".json")) == 3,
				"only what changed");

		// Nothing new: an attempt, and no file.
		assertDeltaStarts(service);
		awaitIdle(service);
		state = service.getState();
		Assertions.assertTrue(state.lastAttempt().success());
		Assertions.assertEquals(1, countOf(state, BackupArchiveType.DELTA));
		Assertions.assertFalse(state.lastAttempt().date().equals(state.archives().get(0).date()));

		// A later change: the chain moves to the last delta, which holds everything since the full.
		db.getCollection(COLLECTION).updateOne(new Document("_id", "updated"),
				new Document("$set", new Document("value", 3).append("updatedAt", new Date())));
		assertDeltaStarts(service);
		awaitIdle(service);
		Assertions.assertEquals(2, countOf(service.getState(), BackupArchiveType.DELTA));

		for (final String name : collections) {
			db.getCollection(name).drop();
		}
		final BackupEngine reader = new BackupEngine(store, BASE_NAME, EngineBackupType.JSON_EXTENDED);
		reader.setEnableStoreOrRestoreData(false);
		Assertions.assertEquals(2, reader.getLatestRestoreChain().size());
		Assertions.assertTrue(reader.restoreLatest());

		Assertions.assertEquals(1, db.getCollection(COLLECTION).find(new Document("_id", "same")).first().get("value"));
		Assertions.assertEquals(3,
				db.getCollection(COLLECTION).find(new Document("_id", "updated")).first().get("value"));
		Assertions.assertEquals(2,
				db.getCollection(COLLECTION).find(new Document("_id", "created")).first().get("value"));
		Assertions.assertEquals(true,
				db.getCollection(COLLECTION).find(new Document("_id", "softDeleted")).first().get("deleted"));
		Assertions.assertEquals(2, db.getCollection(undated).find(new Document("_id", "undated")).first().get("value"));
		Assertions.assertEquals(11L,
				db.getCollection("counters").find(new Document("_id", "sequence")).first().get("value"));
		// The limit written in the documentation.
		Assertions.assertNotNull(db.getCollection(COLLECTION).find(new Document("_id", "hardDeleted")).first());
	}
}
