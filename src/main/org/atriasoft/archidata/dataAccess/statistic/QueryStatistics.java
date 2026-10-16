package org.atriasoft.archidata.dataAccess.statistic;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.atriasoft.archidata.cron.CronScheduler;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Counts how many times each <i>query shape</i> is executed, to decide which MongoDB indexes are
 * worth creating and in which field order.
 *
 * <p>
 * The whole mode is off unless the environment variable {@code QUERY_STATISTICS_FILE} points to a
 * writable path. When it is off, {@link #record} costs a single field read and returns, so it is
 * safe to leave the calls in the hot path of production code.
 * </p>
 *
 * <p><b>How it works.</b> Every filtered query is reduced to a {@link QuerySignature} — the field
 * names, stripped of their values, ordered by the ESR rule. The occurrences are accumulated in
 * memory in a lock-free map, and flushed to the JSON file periodically (see
 * {@code QUERY_STATISTICS_PERIOD}, every 10 minutes by default) and on JVM shutdown. At start-up
 * the file is read back, so the counters keep growing across restarts instead of starting over.</p>
 *
 * <p><b>Reading the output.</b> The entries are sorted by decreasing {@code count}. Take the top
 * ones and create the {@code suggestedIndex} they carry:</p>
 * <pre>{@code
 * // {"signature": "user|FIND|E:companyId|S:createdAt:-1|R:", "count": 148203,
 * //  "suggestedIndex": "{\"companyId\": 1, \"createdAt\": -1}"}
 * db.user.createIndex({companyId: 1, createdAt: -1})
 * }</pre>
 * <p>
 * Two caveats worth knowing before acting on the numbers:
 * </p>
 * <ul>
 * <li>A high {@code count} is not proof that an index is needed: a query run a million times over a
 * 50-document collection needs nothing. Cross-check with the real collection size.</li>
 * <li>An entry flagged {@code containsOr} cannot be served by one compound index; MongoDB will need
 * one index per {@code $or} branch.</li>
 * </ul>
 *
 * <p>
 * The file is written atomically (a private temporary file created in the same directory, then an atomic
 * move), so it can be read at any time without catching a half-written document. The file is readable by its
 * owner only ({@code rw-------} on a POSIX file system): keep it in a directory reserved to the service.
 * </p>
 * <p>
 * The number of distinct query shapes is bounded ({@link #setMaxEntries(int)}, {@value #DEFAULT_MAX_ENTRIES}
 * by default): beyond it, new shapes are counted together under {@value #OVERFLOW_KEY}. An application that
 * builds filters on dynamic keys ({@code meta.<clientKey>}) cannot make the table grow without end.
 * </p>
 *
 * @see QuerySignature
 */
public final class QueryStatistics {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryStatistics.class);
	private static final String CRON_TASK_NAME = "archidata-query-statistics-flush";

	/** Default maximum number of distinct query shapes kept. */
	public static final int DEFAULT_MAX_ENTRIES = 10_000;
	/** Key of the entry counting the shapes met once the table is full. */
	public static final String OVERFLOW_KEY = "__other__";
	private static final Set<PosixFilePermission> OWNER_FILE = PosixFilePermissions.fromString("rw-------");
	private static volatile int maxEntries = DEFAULT_MAX_ENTRIES;

	/** Aggregated occurrences, keyed by {@link QuerySignature#getKey()}. */
	private static final Map<String, Counter> COUNTERS = new ConcurrentHashMap<>();
	private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	/** Separate from {@link #STARTED}: a {@link #reset()} must not install a second shutdown hook. */
	private static final AtomicBoolean HOOK_INSTALLED = new AtomicBoolean(false);
	private static CronScheduler scheduler = null;

	/** A query shape and its occurrence counter. */
	private static final class Counter {
		/** Set as soon as the shape is met at runtime; stays {@code null} for a shape only read back from disk. */
		volatile QuerySignature signature = null;
		/** The entry read back from disk, kept to re-emit the description of a shape not met again since. */
		volatile QueryStatEntry loaded = null;
		final LongAdder count = new LongAdder();
	}

	private QueryStatistics() {
		// Utility class
	}

	/**
	 * Sets the maximum number of distinct query shapes kept; the next shapes are counted under
	 * {@value #OVERFLOW_KEY}.
	 * @param value the maximum, {@code >= 1}
	 * @throws IllegalArgumentException if {@code value < 1}
	 */
	public static void setMaxEntries(final int value) {
		if (value < 1) {
			throw new IllegalArgumentException("Query statistics max entries must be >= 1, got: " + value);
		}
		maxEntries = value;
	}

	/**
	 * Returns the maximum number of distinct query shapes kept.
	 * @return the maximum
	 */
	public static int getMaxEntries() {
		return maxEntries;
	}

	/**
	 * Returns the counter of a shape, or the overflow counter when the table is full.
	 * @param key the shape key
	 * @return the counter to increment
	 */
	private static Counter counterFor(final String key) {
		final Counter existing = COUNTERS.get(key);
		if (existing != null) {
			return existing;
		}
		if (COUNTERS.size() >= maxEntries) {
			return COUNTERS.computeIfAbsent(OVERFLOW_KEY, it -> new Counter());
		}
		return COUNTERS.computeIfAbsent(key, it -> new Counter());
	}

	/**
	 * Tells whether the query-statistics mode is enabled.
	 * @return {@code true} when {@code QUERY_STATISTICS_FILE} is set
	 */
	public static boolean isEnabled() {
		return ConfigBaseVariable.getQueryStatisticsFile() != null;
	}

	/**
	 * Records one execution of a query shape. Does nothing when the mode is disabled.
	 *
	 * <p>
	 * This method never propagates an exception: a statistics failure must never break the query it
	 * is measuring.
	 * </p>
	 *
	 * @param collection the collection the query runs on
	 * @param operation the kind of operation
	 * @param filter the user filter, <b>before</b> the soft-delete exclusion is merged in, or
	 *        {@code null} when the query has no filter
	 * @param sort the sort document ({@code field -> 1|-1}, in order), or {@code null} when unsorted
	 * @param softDeleteField the soft-delete field implicitly excluded, or {@code null}
	 */
	public static void record(
			final String collection,
			final QuerySignature.Operation operation,
			final Bson filter,
			final Document sort,
			final String softDeleteField) {
		if (!isEnabled()) {
			return;
		}
		try {
			ensureStarted();
			final QuerySignature signature = QuerySignature.of(collection, operation, filter, sort, softDeleteField);
			final Counter counter = counterFor(signature.getKey());
			// The overflow counter mixes many shapes: it keeps no description.
			if (counter.signature == null && counter != COUNTERS.get(OVERFLOW_KEY)) {
				counter.signature = signature;
			}
			counter.count.increment();
		} catch (final Exception ex) {
			LOGGER.warn("Failed to record query statistics on collection '{}': {}", collection, ex.getMessage());
		}
	}

	/**
	 * Loads the previous counters and schedules the periodic flush, once.
	 */
	private static void ensureStarted() {
		if (!STARTED.compareAndSet(false, true)) {
			return;
		}
		final Path path = Path.of(ConfigBaseVariable.getQueryStatisticsFile());
		load(path);
		final String period = ConfigBaseVariable.getQueryStatisticsPeriod();
		LOGGER.info("Query statistics enabled: file={} period='{}'", path, period);
		try {
			scheduler = new CronScheduler();
			// The application does not own this scheduler: it must not keep the JVM alive (the shutdown hook
			// below writes the last report).
			scheduler.setDaemonThreads(true);
			scheduler.addTask(CRON_TASK_NAME, period, QueryStatistics::flush);
			scheduler.start();
		} catch (final Exception ex) {
			LOGGER.error("Failed to schedule the query-statistics flush ('{}'): {} — statistics will only be "
					+ "written on shutdown", period, ex.getMessage());
		}
		if (HOOK_INSTALLED.compareAndSet(false, true)) {
			Runtime.getRuntime().addShutdownHook(new Thread(QueryStatistics::flush, "query-statistics-flush"));
		}
	}

	/**
	 * Reads back the counters of a previous run, so the occurrences cumulate across restarts.
	 * A missing or unreadable file simply starts from zero.
	 *
	 * @param path the JSON file to read
	 */
	private static void load(final Path path) {
		if (!Files.isRegularFile(path)) {
			return;
		}
		try {
			final QueryStatReport report = new ObjectMapper().readValue(path.toFile(), QueryStatReport.class);
			if (report.queries == null) {
				return;
			}
			for (final QueryStatEntry entry : report.queries) {
				if (entry.signature == null) {
					continue;
				}
				final Counter counter = counterFor(entry.signature);
				if (counter == COUNTERS.get(OVERFLOW_KEY) && !OVERFLOW_KEY.equals(entry.signature)) {
					counter.count.add(entry.count);
					continue;
				}
				counter.loaded = entry;
				counter.count.add(entry.count);
			}
			LOGGER.info("Query statistics: resumed {} query shapes from {}", report.queries.size(), path);
		} catch (final IOException ex) {
			LOGGER.warn("Query statistics: cannot read back '{}', starting from scratch: {}", path, ex.getMessage());
		}
	}

	/**
	 * Writes the current counters to the JSON file, atomically. Does nothing when the mode is
	 * disabled. Never propagates an exception. Synchronized: the periodic flush and the shutdown hook
	 * share the same temporary file.
	 */
	public static synchronized void flush() {
		final String file = ConfigBaseVariable.getQueryStatisticsFile();
		if (file == null) {
			return;
		}
		final Path path = Path.of(file);
		try {
			final QueryStatReport report = buildReport();
			final Path target = path.toAbsolutePath();
			final Path parent = target.getParent();
			Files.createDirectories(parent);
			// Unpredictable private name in the same directory (never follows a planted link), then atomic move.
			final boolean posix = parent.getFileSystem().supportedFileAttributeViews().contains("posix");
			final String prefix = "." + target.getFileName() + ".";
			final Path tmp = posix
					? Files.createTempFile(parent, prefix, ".tmp", PosixFilePermissions.asFileAttribute(OWNER_FILE))
					: Files.createTempFile(parent, prefix, ".tmp");
			try {
				try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING)) {
					new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(out, report);
				}
				try {
					Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				} catch (final AtomicMoveNotSupportedException ex) {
					Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
				}
			} finally {
				Files.deleteIfExists(tmp);
			}
			LOGGER.debug("Query statistics: flushed {} query shapes to {}", report.queries.size(), path);
		} catch (final Exception ex) {
			LOGGER.error("Query statistics: failed to write '{}': {}", path, ex.getMessage());
		}
	}

	/**
	 * Builds the report of the current counters, sorted by decreasing number of occurrences.
	 *
	 * @return the report, ready to be serialized
	 */
	public static QueryStatReport buildReport() {
		final QueryStatReport report = new QueryStatReport();
		report.generatedAt = Instant.now().toString();
		final List<QueryStatEntry> entries = new ArrayList<>();
		for (final Map.Entry<String, Counter> element : COUNTERS.entrySet()) {
			final Counter counter = element.getValue();
			final QueryStatEntry entry = new QueryStatEntry();
			entry.signature = element.getKey();
			entry.count = counter.count.sum();
			final QuerySignature signature = counter.signature;
			if (signature != null) {
				entry.collection = signature.getCollection();
				entry.operation = signature.getOperation().name();
				entry.equality = signature.getEquality();
				entry.sort = signature.getSort();
				entry.range = signature.getRange();
				entry.softDeleteField = signature.getSoftDeleteField();
				entry.containsOr = signature.isContainsOr();
				entry.suggestedIndex = signature.getSuggestedIndex();
			} else if (counter.loaded != null) {
				// Shape read back from disk and not met again since: keep its description as-is.
				final QueryStatEntry previous = counter.loaded;
				entry.collection = previous.collection;
				entry.operation = previous.operation;
				entry.equality = previous.equality;
				entry.sort = previous.sort;
				entry.range = previous.range;
				entry.softDeleteField = previous.softDeleteField;
				entry.containsOr = previous.containsOr;
				entry.suggestedIndex = previous.suggestedIndex;
			}
			entries.add(entry);
		}
		entries.sort(Comparator.comparingLong((final QueryStatEntry it) -> it.count).reversed()
				.thenComparing(it -> it.signature));
		report.queries = entries;
		return report;
	}

	/**
	 * Drops every in-memory counter and stops the periodic flush, without touching the file.
	 * Intended for tests.
	 */
	public static void reset() {
		if (scheduler != null) {
			scheduler.stop();
			scheduler = null;
		}
		COUNTERS.clear();
		STARTED.set(false);
	}
}
