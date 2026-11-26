package test.atriasoft.archidata.dataStreamEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.atriasoft.archidata.dataStreamEvent.ChangeEvent;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationListener;
import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.atriasoft.archidata.dataStreamEvent.ChangeStreamWorker;
import org.atriasoft.archidata.dataStreamEvent.WorkerStatus;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.FullDocument;

import test.atriasoft.archidata.ConfigureDb;

/**
 * An event a worker has read is not lost when the worker is stopped before it dispatches it.
 *
 * <p>
 * The worker used to keep the token of an event as the position to resume from as soon as it read the event, before
 * the manager dispatched it. A worker stopped by {@code unwatchCollection()} between the two then left that token to
 * the manager, did not dispatch the event (it was no longer the worker of its collection), and the next worker of the
 * collection resumed after the event: nobody received it. A worker that had dispatched nothing had no token at all,
 * and the next worker started from the time its own stream opened, after the event too.
 * </p>
 *
 * <p>
 * The interleaving is forced, not hoped for: the manager is started on a database whose {@code getCollection()} runs a
 * hook for one collection name. The manager calls it with its write lock held, so the hook holds the worker of the
 * watched collection between the read of an event and the dispatch (the worker waits for the read lock), and stops it
 * there with a reentrant {@code unwatchCollection()}. The same database reports when the change stream of a collection
 * is idle (a {@code tryNext()} that returns no change), and asks the server to wait {@link #AWAIT_MS} ms instead of
 * 1 s before such an answer, which only makes the tests faster.
 * </p>
 *
 * <p>
 * A client filter that throws is caught like a listener that throws: it used to leave the dispatch, and the event
 * never reached the listeners after it. A listener that throws an {@link Error} ends the worker, as before, but no
 * longer in silence: the worker logs it and reports {@link WorkerStatus#STOPPED} instead of RUNNING.
 * </p>
 */
public class TestChangeStreamNoEventLost {
	private static final String COLLECTION_PREFIX = "TestChangeStreamNoEventLost_";
	private static final long WAIT_SECONDS = 10;
	private static final long AWAIT_MS = 20;
	private static final AtomicInteger RUN = new AtomicInteger();

	/** Collection whose {@code getCollection()} runs {@link #hook}, once. */
	private static volatile String hookCollection = null;
	private static volatile Runnable hook = null;
	/** Released at the first idle poll of the change stream of each collection. */
	private static final Map<String, CountDownLatch> IDLE = new ConcurrentHashMap<>();

	private static MongoDatabase database;
	private static ChangeNotificationManager manager;

	@BeforeAll
	public static void configureDatabase() throws Exception {
		ConfigureDb.configure();
		manager = ChangeNotificationManager.getInstance();
		if (manager.isRunning()) {
			manager.stop();
		}
		database = ConfigureDb.da.getInterface().getDatabase();
		manager.start(hookedDatabase(database), FullDocument.UPDATE_LOOKUP);
	}

	@AfterAll
	public static void cleanupDatabase() throws Exception {
		if (manager != null && manager.isRunning()) {
			manager.stop();
		}
		ConfigureDb.clear();
	}

	@AfterEach
	public void clearListeners() {
		manager.clearAllListeners();
	}

	private static Object invoke(final Object target, final Method method, final Object[] args) throws Throwable {
		try {
			return method.invoke(target, args);
		} catch (final InvocationTargetException ex) {
			throw ex.getCause();
		}
	}

	/** A database that runs {@link #hook} when the manager asks it for {@link #hookCollection}. */
	private static MongoDatabase hookedDatabase(final MongoDatabase real) {
		final InvocationHandler handler = (proxy, method, args) -> {
			if ("getCollection".equals(method.getName()) && args != null && args.length == 1) {
				if (args[0].equals(hookCollection)) {
					final Runnable toRun = hook;
					hook = null;
					hookCollection = null;
					if (toRun != null) {
						toRun.run();
					}
				}
				return observed(invoke(real, method, args), MongoCollection.class, (String) args[0]);
			}
			return invoke(real, method, args);
		};
		return (MongoDatabase) Proxy.newProxyInstance(TestChangeStreamNoEventLost.class.getClassLoader(),
				new Class<?>[] { MongoDatabase.class }, handler);
	}

	/**
	 * Wraps a collection, a change stream iterable or a change stream cursor of the driver: its change streams wait
	 * {@link #AWAIT_MS} ms for a change, and report their idle polls in {@link #IDLE}.
	 */
	private static Object observed(final Object target, final Class<?> type, final String collection) {
		final InvocationHandler handler = (proxy, method, args) -> {
			Object receiver = target;
			if (target instanceof final ChangeStreamIterable<?> iterable && "cursor".equals(method.getName())) {
				receiver = iterable.maxAwaitTime(AWAIT_MS, TimeUnit.MILLISECONDS);
			}
			final Object result = invoke(receiver, method, args);
			if (target instanceof MongoChangeStreamCursor && "tryNext".equals(method.getName()) && result == null) {
				idle(collection).countDown();
			}
			if (result instanceof MongoChangeStreamCursor) {
				return observed(result, MongoChangeStreamCursor.class, collection);
			}
			if (result instanceof ChangeStreamIterable) {
				return observed(result, ChangeStreamIterable.class, collection);
			}
			if (result instanceof MongoCollection) {
				return observed(result, MongoCollection.class, collection);
			}
			return result;
		};
		return Proxy.newProxyInstance(TestChangeStreamNoEventLost.class.getClassLoader(), new Class<?>[] { type },
				handler);
	}

	private static CountDownLatch idle(final String collection) {
		return IDLE.computeIfAbsent(collection, key -> new CountDownLatch(1));
	}

	/** Records, in order, the ids of the documents a listener receives. */
	private static final class Received implements ChangeNotificationListener {
		private final List<ObjectId> ids = Collections.synchronizedList(new ArrayList<>());
		private final Map<ObjectId, CountDownLatch> latches = new ConcurrentHashMap<>();

		@Override
		public void onNotification(final ChangeEvent event) {
			if (event.getOid() instanceof final BsonObjectId id) {
				this.ids.add(id.getValue());
				latch(id.getValue()).countDown();
			}
		}

		private CountDownLatch latch(final ObjectId id) {
			return this.latches.computeIfAbsent(id, key -> new CountDownLatch(1));
		}

		void await(final ObjectId id) throws InterruptedException {
			Assertions.assertTrue(latch(id).await(WAIT_SECONDS, TimeUnit.SECONDS),
					"document " + id + " never notified, received: " + ids());
		}

		List<ObjectId> ids() {
			synchronized (this.ids) {
				return List.copyOf(this.ids);
			}
		}
	}

	private static ObjectId insert(final String collection) {
		final ObjectId id = new ObjectId();
		insert(collection, id);
		return id;
	}

	private static void insert(final String collection, final ObjectId id) {
		database.getCollection(collection).insertOne(new Document("_id", id));
	}

	/**
	 * Waits until the worker thread of a collection waits for the manager's read lock in {@code notifyChange()}: it
	 * has then read an event (and kept its token), and cannot dispatch it while the write lock is held.
	 */
	private static void awaitWorkerWaitingForReadLock(final String collection) {
		final String threadName = "ChangeStreamWorker_" + collection;
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
		while (System.nanoTime() < deadline) {
			for (final Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
				if (threadName.equals(entry.getKey().getName()) && waitsForReadLockInNotifyChange(entry.getValue())) {
					return;
				}
			}
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
		}
		Assertions.fail("the worker of " + collection + " never waited for the read lock to dispatch an event");
	}

	private static boolean waitsForReadLockInNotifyChange(final StackTraceElement[] stack) {
		for (int index = 0; index + 1 < stack.length; index++) {
			if (ReentrantReadWriteLock.ReadLock.class.getName().equals(stack[index].getClassName())
					&& "lock".equals(stack[index].getMethodName())
					&& ChangeNotificationManager.class.getName().equals(stack[index + 1].getClassName())
					&& "notifyChange".equals(stack[index + 1].getMethodName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Writes a document in {@code watched} while a manager call ({@code watchCollection(hooked)}) holds the write lock,
	 * waits until the worker of {@code watched} has read it and waits for the read lock, and unwatches {@code watched}
	 * there: the worker then drops the event, as a stopped worker.
	 */
	private static void unwatchWhileItsWorkerHoldsAnEvent(
			final String watched,
			final String hooked,
			final ObjectId id) {
		hookCollection = hooked;
		hook = () -> {
			insert(watched, id);
			awaitWorkerWaitingForReadLock(watched);
			manager.unwatchCollection(watched);
		};
		manager.watchCollection(hooked);
		Assertions.assertNull(hook, "the manager never asked the database for " + hooked);
	}

	@RepeatedTest(5)
	public void anEventReadByAWorkerStoppedBeforeItsDispatchReachesTheListenersOnce() throws Exception {
		final int run = RUN.incrementAndGet();
		final String watched = COLLECTION_PREFIX + run;
		final String hooked = COLLECTION_PREFIX + "hook_" + run;
		final Received received = new Received();
		manager.registerListener(received, watched);
		try {
			// The worker dispatches a first event
			final ObjectId first = insert(watched);
			received.await(first);

			// The worker reads the second event and waits for the read lock, the collection is unwatched meanwhile
			final ObjectId read = new ObjectId();
			unwatchWhileItsWorkerHoldsAnEvent(watched, hooked, read);

			// Watched again: the new worker resumes from the position the manager saved
			manager.watchCollection(watched);
			final ObjectId last = insert(watched);
			received.await(last);
			// The stream is ordered: the second event, if it is ever dispatched, is dispatched before the last one
			Assertions.assertEquals(List.of(first, read, last), received.ids(),
					"every event exactly once, in order (first, read by the stopped worker, last)");
		} finally {
			manager.unwatchCollection(watched);
			manager.unwatchCollection(hooked);
		}
	}

	@RepeatedTest(5)
	public void theFirstEventOfAWorkerStoppedBeforeItsDispatchReachesTheListenersOnce() throws Exception {
		final int run = RUN.incrementAndGet();
		final String watched = COLLECTION_PREFIX + "fresh_" + run;
		final String hooked = COLLECTION_PREFIX + "hook_fresh_" + run;
		final Received received = new Received();
		// A new collection: its worker starts with no saved token, and dispatches nothing before the event
		manager.registerListener(received, watched);
		try {
			// Its position is the one of its stream, known once the stream was idle
			Assertions.assertTrue(idle(watched).await(WAIT_SECONDS, TimeUnit.SECONDS),
					"the change stream of " + watched + " never polled");

			final ObjectId read = new ObjectId();
			unwatchWhileItsWorkerHoldsAnEvent(watched, hooked, read);

			manager.watchCollection(watched);
			final ObjectId last = insert(watched);
			received.await(last);
			Assertions.assertEquals(List.of(read, last), received.ids(),
					"every event exactly once, in order (read by the stopped worker, last)");
		} finally {
			manager.unwatchCollection(watched);
			manager.unwatchCollection(hooked);
		}
	}

	private static void awaitStatus(final String collection, final WorkerStatus expected) {
		final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
		while (System.nanoTime() < deadline) {
			if (manager.getWorkerStatuses().get(collection) == expected) {
				return;
			}
			LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
		}
		Assertions.fail("the worker of " + collection + " never reported " + expected + ", it reports "
				+ manager.getWorkerStatuses().get(collection));
	}

	@RepeatedTest(3)
	public void aListenerThatThrowsAnErrorEndsTheWorkerWithALogAndAStoppedStatus() throws Exception {
		final String watched = COLLECTION_PREFIX + "error_" + RUN.incrementAndGet();
		final Received received = new Received();
		final ObjectId poisoned = new ObjectId();
		final BsonObjectId poisonedKey = new BsonObjectId(poisoned);
		final AtomicBoolean thrown = new AtomicBoolean(false);
		final Logger workerLogger = (Logger) LoggerFactory.getLogger(ChangeStreamWorker.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		workerLogger.addAppender(appender);
		// The global listeners are called before the listeners of the collection: the Error takes the event from them
		manager.registerListener(event -> {
			if (poisonedKey.equals(event.getOid()) && thrown.compareAndSet(false, true)) {
				throw new AssertionError("listener failure raised by the test");
			}
		});
		manager.registerListener(received, watched);
		try {
			// The worker has a position before the event (see the test of the first event)
			Assertions.assertTrue(idle(watched).await(WAIT_SECONDS, TimeUnit.SECONDS),
					"the change stream of " + watched + " never polled");
			insert(watched, poisoned);

			// The status is written after the log: once it is seen, the log is there
			awaitStatus(watched, WorkerStatus.STOPPED);
			final List<ILoggingEvent> errors = appender.list.stream().filter(
					logged -> logged.getLevel() == Level.ERROR && logged.getFormattedMessage().contains(watched))
					.toList();
			Assertions.assertEquals(1, errors.size(), "one ERROR naming the collection");
			Assertions.assertNotNull(errors.get(0).getThrowableProxy(), "the ERROR carries the throwable");
			Assertions.assertEquals(AssertionError.class.getName(), errors.get(0).getThrowableProxy().getClassName());
			Assertions.assertTrue(manager.isWatching(watched), "the collection stays watched");
			Assertions.assertEquals(List.of(), received.ids(), "the Error took the event from the next listeners");

			// Unchanged semantics: a new worker resumes from the position before the event, and dispatches it
			manager.unwatchCollection(watched);
			manager.watchCollection(watched);
			final ObjectId last = insert(watched);
			received.await(last);
			Assertions.assertEquals(List.of(poisoned, last), received.ids(),
					"the new worker dispatches the event the Error interrupted, then the next one");
		} finally {
			workerLogger.detachAppender(appender);
			appender.stop();
			manager.unwatchCollection(watched);
		}
	}

	@Test
	public void aClientFilterThatThrowsDoesNotTakeTheEventFromTheOtherListeners() throws Exception {
		final String watched = COLLECTION_PREFIX + "filter";
		final Received received = new Received();
		final ObjectId poisoned = new ObjectId();
		final BsonObjectId poisonedKey = new BsonObjectId(poisoned);
		// The global listeners are called before the listeners of the collection
		manager.createListenerBuilder(event -> {}).filter(event -> {
			if (poisonedKey.equals(event.getOid())) {
				throw new IllegalStateException("client filter failure raised by the test");
			}
			return true;
		}).register();
		manager.registerListener(received, watched);
		try {
			insert(watched, poisoned);
			final ObjectId next = insert(watched);
			received.await(next);
			Assertions.assertEquals(List.of(poisoned, next), received.ids(),
					"the listener of the collection receives the event the filter of another listener failed on");
		} finally {
			manager.unwatchCollection(watched);
		}
	}
}
