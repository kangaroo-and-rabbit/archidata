package test.atriasoft.archidata.dataStreamEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.atriasoft.archidata.db.DbConfig;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.FullDocument;

import test.atriasoft.archidata.ConfigureDb;

/**
 * {@code stop()} then {@code start()} in the same JVM, as two test classes of an application do with the
 * singleton manager, and writes made right after a registration.
 *
 * <p>
 * A change stream begins when its worker runs the {@code aggregate} on the server, on the worker thread:
 * {@code registerListener()} used to return before, so a write made just after it was never notified, and
 * the next writes were. {@code stop()} used to return after 1.5 s with its workers still running (blocked
 * in a {@code getMore}, which neither the stop flag nor an interrupt ends). The manager is started from a
 * {@link DbConfig}, so that it owns its client, as an application does.
 * </p>
 */
public class TestChangeStreamRestart {
	private static final String COLLECTION_PREFIX = "TestChangeStreamRestart_";
	private static final int COLLECTIONS = 5;
	private static final long WAIT_SECONDS = 10;

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		stopManager();
		final MongoDatabase database = ConfigureDb.da.getInterface().getDatabase();
		for (final String name : collectionNames()) {
			database.createCollection(name);
		}
	}

	@AfterAll
	public static void cleanupDatabase() throws Exception {
		stopManager();
		ConfigureDb.clear();
	}

	@AfterEach
	public void stopAfterTest() {
		stopManager();
	}

	private static void stopManager() {
		final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
		if (manager.isRunning()) {
			manager.stop();
		}
	}

	private static List<String> collectionNames() {
		final List<String> names = new ArrayList<>();
		for (int index = 0; index < COLLECTIONS; index++) {
			names.add(COLLECTION_PREFIX + index);
		}
		return names;
	}

	/** Records the documents a listener receives, by id. */
	private static final class Received {
		private final Map<BsonValue, CountDownLatch> expected = new ConcurrentHashMap<>();

		CountDownLatch expect(final ObjectId id) {
			return this.expected.computeIfAbsent(new BsonObjectId(id), key -> new CountDownLatch(1));
		}

		void record(final Object id) {
			if (id instanceof final BsonObjectId bsonId) {
				expect(bsonId.getValue()).countDown();
			}
		}
	}

	/** Registers a listener on each collection, writes a document right after each registration returns. */
	private static Map<String, CountDownLatch> registerThenWriteAtOnce(final Received received) throws Exception {
		final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
		final MongoDatabase database = ConfigureDb.da.getInterface().getDatabase();
		final Map<String, CountDownLatch> written = new ConcurrentHashMap<>();
		for (final String name : collectionNames()) {
			manager.registerListener(event -> received.record(event.getOid()), name);
			final ObjectId id = new ObjectId();
			written.put(name, received.expect(id));
			database.getCollection(name).insertOne(new Document("_id", id).append("collection", name));
		}
		return written;
	}

	/**
	 * Registers a listener on each collection and writes until a write is notified: the streams are then
	 * open, whether {@code registerListener()} waits for it or not.
	 */
	private static void registerThenAwaitLive() throws Exception {
		final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
		final MongoDatabase database = ConfigureDb.da.getInterface().getDatabase();
		final Received received = new Received();
		for (final String name : collectionNames()) {
			manager.registerListener(event -> received.record(event.getOid()), name);
		}
		for (final String name : collectionNames()) {
			boolean live = false;
			for (int attempt = 0; attempt < WAIT_SECONDS && !live; attempt++) {
				final ObjectId id = new ObjectId();
				final CountDownLatch notified = received.expect(id);
				database.getCollection(name).insertOne(new Document("_id", id).append("collection", name));
				live = notified.await(1, TimeUnit.SECONDS);
			}
			Assertions.assertTrue(live, "the change stream of " + name + " never notified a write");
		}
	}

	private static void assertAllNotified(final Map<String, CountDownLatch> written, final String when)
			throws InterruptedException {
		final List<String> missing = new ArrayList<>();
		for (final Map.Entry<String, CountDownLatch> entry : written.entrySet()) {
			if (!entry.getValue().await(WAIT_SECONDS, TimeUnit.SECONDS)) {
				missing.add(entry.getKey());
			}
		}
		Assertions.assertTrue(missing.isEmpty(),
				when + ": a write made right after registerListener() returned was" + " never notified on " + missing);
	}

	private static List<String> workerThreadsAlive() {
		final List<String> alive = new ArrayList<>();
		for (final Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.isAlive() && thread.getName().startsWith("ChangeStreamWorker_" + COLLECTION_PREFIX)) {
				alive.add(thread.getName());
			}
		}
		return alive;
	}

	@Test
	public void aWriteRightAfterRegisterListenerIsNotified() throws Exception {
		ChangeNotificationManager.getInstance().start(new DbConfig(), FullDocument.UPDATE_LOOKUP);
		assertAllNotified(registerThenWriteAtOnce(new Received()), "first start");
	}

	@Test
	public void aWriteRightAfterStopThenStartIsNotified() throws Exception {
		final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
		manager.start(new DbConfig(), FullDocument.UPDATE_LOOKUP);
		registerThenAwaitLive();

		// The previous user of the singleton stops it, the next one starts it again at once
		manager.stop();
		manager.start(new DbConfig(), FullDocument.UPDATE_LOOKUP);
		assertAllNotified(registerThenWriteAtOnce(new Received()), "start right after stop()");
	}

	@Test
	public void stopReturnsOnceItsWorkersHaveEnded() throws Exception {
		final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();
		manager.start(new DbConfig(), FullDocument.UPDATE_LOOKUP);
		// Every worker is then waiting in a getMore of its open stream
		registerThenAwaitLive();

		manager.stop();
		Assertions.assertEquals(List.of(), workerThreadsAlive(),
				"stop() returned while workers of the stopped manager were still running");
	}
}
