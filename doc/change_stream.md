# Change streams (real-time change notifications)

The `dataStreamEvent` package turns MongoDB change streams into plain Java callbacks: you register a
listener on a collection and it is called each time a document is inserted, updated, replaced or
deleted — by this process, by another service, or by a shell session. Nothing is polled.

This page covers the prerequisites, the real runtime behaviour, the public API, a few short recipes,
and the limits you should know before relying on it. For history/audit stored *inside* MongoDB, see
[change_journal.md](change_journal.md) instead; the two features are independent.

## Prerequisites

- MongoDB **6+ running as a replica set**. A standalone `mongod` rejects `watch()` and the worker
  will loop on `MongoException` (see [Pitfalls](#pitfalls-and-limits)). A single-node replica set is
  enough; `TUTORIAL_01_first_install.md` shows how to start one with Docker.
- The user must be allowed to run `changeStream` on the watched collections.
- Collection names are what Archidata uses for entities, i.e. the simple class name
  (`TestChangeStreamEntity` in the tests). There is no entity-to-collection resolution in this
  package: you pass the collection name as a `String`.

## Quick start

```java
final ChangeNotificationManager manager = ChangeNotificationManager.getInstance();

// 1. Start with an existing database handle and a FullDocument mode.
manager.start(database, FullDocument.UPDATE_LOOKUP);

// 2. Listen to one collection.
manager.createListenerBuilder(event -> {
    if (event.isInsert()) {
        LOGGER.info("new {} {}", event.getCollectionName(), event.getOid());
    } else if (event.isUpdate()) {
        LOGGER.info("updated fields {}", event.getUpdatedFields());
    } else if (event.isDelete()) {
        LOGGER.info("deleted {}", event.getOid());
    }
}, "User").register();

// 3. Stop on shutdown.
manager.stop();
```

Listeners may be registered before or after `start(...)`. Registering on a collection while the
manager is running creates the worker for that collection on the spot. Registering before `start(...)`
only records the listener; the workers are created when `start(...)` is called.

Without a `MongoDatabase` in hand, `start()` builds a client from the `DB_*` environment variables
(`new DbConfig()`), and `start(DbConfig)` from an explicit configuration. In both cases the manager
owns the client and closes it in `stop()`.

## Concepts and architecture

```
MongoDB  ──change stream──>  ChangeStreamWorker (1 thread per collection)
                                   │  ChangeStreamDocument -> ChangeEvent
                                   v
                     ChangeNotificationManager.notifyChange()
                          │                       │
                global listeners       listeners of that collection
                (client filter, then onNotification)
```

- **One singleton manager.** `ChangeNotificationManager.getInstance()`. State is protected by a
  `ReentrantReadWriteLock`; registration is safe from any thread.
- **One worker per watched collection.** Each `ChangeStreamWorker` is a `Runnable` submitted to a
  cached thread pool created by `start(...)`. The thread is named `ChangeStreamWorker_<collection>`.
- **A collection is watched when** a listener is registered for it, when `watchCollection(...)` /
  `watch(name).start()` is called, or after `watchAllCollections()`. Global listeners (registered
  without a collection) do not create workers on their own: they only receive events from
  collections that are already watched.
- **A stream begins when its worker opens it, so registering waits for it.** The worker runs the
  `aggregate` that opens the change stream on its own thread, and a stream opened without a start
  point only sees the changes made after that. `start(...)`, `registerListener(...)`, `register()` of
  a `ListenerRegistrationBuilder`, `watchCollection(...)` and `watch(name).start()` therefore return
  only once the streams of the collections they start are open (at most 10 s in all, then a WARN
  names the collection): a write made after they return is notified. Opening a stream takes a
  connection of its own, a few milliseconds; `registerListener(listener, Set<String>)` opens the
  streams of its collections in parallel.
- **The FullDocument mode is global.** It is fixed by `start(..., mode)` and applied to every worker.
  There is one worker per collection, so a listener cannot get another mode: the `mode` argument
  of `registerListener(..., mode)`, `withMode(...)` and `CollectionWatchBuilder.fullDocument(...)`
  is **ignored**. These overloads are `@Deprecated` and log a warning as soon as a mode is passed,
  before or after `start()`: only the mode given to `start()` counts.
  `start(database)`, `start(dbConfig)` and `start()` use `FullDocument.DEFAULT`.

  | Mode | INSERT / REPLACE | UPDATE | DELETE |
  |---|---|---|---|
  | `DEFAULT` | `getFullDocument()` set | `getFullDocument()` is `null`, use `getUpdatedFields()` | `null` |
  | `UPDATE_LOOKUP` | set | current document (may be `null` if deleted meanwhile) | `null` |
  | `WHEN_AVAILABLE` | set | post-image if the collection has change stream pre/post images enabled | `null` |

- **Events are dispatched on the worker thread**, one event at a time per collection, in stream
  order. For each event the manager calls the global listeners first, then the listeners of the
  collection. A client-side filter that returns `false` skips the listener; an exception thrown by a
  client-side filter or by a listener is caught and logged, skips that listener for that event, and
  does not affect other listeners. An `Error` is not caught there: it ends the worker (see below).
- **Resume tokens.** The worker keeps the token of the last event it read: if its stream errors it
  re-opens with `resumeAfter(token)`. When `unwatchCollection(...)` stops a worker, the manager saves
  another position: the token of the last event **dispatched** to the listeners (it moves once they
  returned, under the manager's read lock, so a stop sees each event either dispatched in full or not
  dispatched at all), or the position of the stream when it was last idle (a `getMore` that returned no
  change: every event read before was handled), whichever is the latest. If the collection is watched
  again, its new worker resumes from there: the writes made meanwhile are notified, and an event the
  stopped worker had read but not dispatched is dispatched once, by the new worker. The writes made
  meanwhile are replayed as long as that position is still in the oplog: after a long unwatch, every one
  of them is dispatched on the new worker's thread, one after the other, a burst that holds back the
  listeners of that collection (the new writes included) until it is drained.
  **Exception:** a worker started without a saved token has no position until it dispatches an event
  or sees its stream idle once (at most the server's await time after the stream opened, 1 s by
  default): the driver gives no token for a stream just opened. Stopped before, it saves nothing, and
  the next worker starts when its own stream opens: an event it had read and the writes made until the
  next worker opens are not notified. Tokens live in memory only: they do not survive a process restart
  or `stop()`.
- **Error handling in the worker loop** (see `WorkerStatus`):
  `MongoInterruptedException` and `InvalidResumeToken` (code 260) reset the token and retry after
  1 s; other `MongoCommandException` / `MongoException` set `RECONNECTING` and retry after 5 s;
  any other exception sets `ERROR` and retries after 5 s. The loop ends on `stop()` (`STOPPED`).
  The delay is fixed (no exponential backoff, no retry limit).
- **An `Error` ends the worker.** An `Error` thrown on the worker thread (by a listener, an
  `AssertionError` included, or by the JVM: `OutOfMemoryError`, `StackOverflowError`) is not retried,
  since the JVM may be in a broken state: the worker logs it at ERROR with the collection name and the
  throwable, reports `STOPPED`, and reads its stream no more, while the collection stays watched
  (`isWatching` is still `true`). The listeners after the one that threw do not receive the event.
  The dead worker stays registered: `watchCollection(name)` and `registerListener(..., name)` do not
  start a new one (they log the WARN "Change stream of collection ... not open"). To start a new
  worker, call `unwatchCollection(name)` then `watchCollection(name)`: once the old worker had a
  position (see the resume tokens above), the new one resumes before the event the `Error` interrupted
  and dispatches it to every listener, those that already had it included. If a listener throws an
  `Error` again on that event, the new worker ends too; `stop()` then `start()`, which forget the
  positions, get past it (the new workers start from the time their streams open).
- **Stopping.** A worker reads its stream with `tryNext()`: a `getMore` with no change returns after
  the server's await time (1 s by default), and the worker then sees it was stopped. `stop()` waits
  for every worker to end (so about 1 s), interrupts the ones still running after 5 s (a listener
  that does not return), waits 5 s more and logs an ERROR naming the ones left. An event a stopped
  worker still reads is never dispatched (the manager checks that the worker is still the one of its
  collection), so `stop()` then `start()` is safe, even right away in the same JVM.
- **Two kinds of filtering.** Server-side (`CollectionWatchBuilder`, an aggregation `$match`
  pipeline) reduces network traffic but is attached to the collection, not to a listener.
  Client-side (`ListenerRegistrationBuilder`, a `Predicate<ChangeEvent>`) is per listener and can
  express anything, but runs after MongoDB has sent the event.

## API reference

All classes are in `org.atriasoft.archidata.dataStreamEvent`.

### ChangeNotificationManager

Lifecycle:

| Method | Notes |
|---|---|
| `static getInstance()` | the singleton |
| `start(MongoDatabase)` | mode `DEFAULT` |
| `start(MongoDatabase, FullDocument)` | throws `IllegalArgumentException` on `null`. If already running with the same mode: no-op. With another mode: `stop()` then restart, which **clears all listeners**. With listeners already registered, returns once their streams are open (at most 10 s) |
| `start()`, `start(DbConfig)`, `start(DbConfig, FullDocument)` | build and own a `MongoClient`; throw `DataAccessException`. If already running with the same mode, the new client is closed and the manager keeps its own |
| `stop()` | stops the workers and clears workers, listeners and resume tokens, then waits for the workers to end (about 1 s; interrupt after 5 s, ERROR after 5 s more), closes an owned client, resets the global mode to `UPDATE_LOOKUP` |
| `isRunning()` | |

Collections:

| Method | Notes |
|---|---|
| `watchCollection(String...)` | starts a worker per collection (if running) |
| `unwatchCollection(String)` | stops the worker and removes its server-side pipeline; listeners stay registered |
| `watchAllCollections()` | every collection except `system.*`; `IllegalStateException` before `start` |
| `unwatchAllCollections()` | |
| `watch(String)` | returns a `CollectionWatchBuilder` |
| `isWatching(String)`, `getWatchedCollections()` | |

Listeners:

| Method | Notes |
|---|---|
| `registerListener(listener)` | global listener (all watched collections) |
| `registerListener(listener, String)` / `(listener, Set<String>)` | collection listeners; the overloads with a `FullDocument` argument ignore it. When running, returns once the streams of the collections are open |
| `createListenerBuilder(listener)` / `(listener, String)` / `(listener, Set<String>)` | returns a `ListenerRegistrationBuilder` (client-side filters) |
| `unregisterListener(listener)` | removes it everywhere (matched with `equals`) |
| `unregisterListener(listener, String)` | removes it from one collection only |
| `clearAllListeners()` | removes all listeners, keeps the workers running |
| `getListenerCount()` | |

Monitoring and configuration: `getWorkerStatuses()` (`Map<String, WorkerStatus>`),
`getCollectionModes()` (the global mode, per watched collection), `getTotalEventsProcessed()`,
`getDefaultFullDocumentMode()`, `setDefaultFullDocumentMode(FullDocument)` (only affects workers
created afterwards).

### ChangeNotificationListener

`@FunctionalInterface` with a single method, `void onNotification(ChangeEvent event)`. Lambdas and
method references work; keep a reference to the instance if you want to unregister it later.

### ChangeEvent

Immutable value object built by the worker.

- `getOperationType()` (`com.mongodb.client.model.changestream.OperationType`), and shortcuts
  `isInsert()`, `isUpdate()`, `isReplace()`, `isDelete()`.
- `getCollectionName()`, `getEntityName()` (currently identical to the collection name).
- `getOid()` is the `_id` of the document as a raw `Object`: the worker takes it from the change
  stream `documentKey`, so it is a `org.bson.BsonValue` (for example `BsonObjectId`), not an
  `ObjectId`. `getOidAs(Class<T>)` is an unchecked cast to the type you pass.
- `getFullDocument()` (`org.bson.Document`, may be `null`), `hasFullDocument()`.
- `getUpdatedFields()` / `getRemovedFields()` return names of changed/removed fields for updates,
  empty sets otherwise. `getUpdateDescription()` gives the driver object.
- `getTimestamp()` (cluster time, or receive time if absent), `getResumeToken()` (a copy: a listener
  cannot move the position the others resume from).
- `toJson()` / `toJsonExtended()` serialize the full document, or return `null` without one.
- The full document is the **raw stored document**: no archidata read rule applies to it. Its
  `@DataNotRead` fields (password hashes, tokens) are present in clear, and its `@DataEncrypt`
  fields are present in their stored, encrypted form. Never relay `getFullDocument()` or `toJson()`
  as is to a client.

### ListenerRegistrationBuilder

Obtained from `createListenerBuilder(...)`. Every filter is combined with AND; call `register()` last.

- `filterField(String name, Object expected)`: `fullDocument.get(name)` equals `expected`; fails if
  the event has no full document.
- `filter(Predicate<ChangeEvent>)`, `filterOperation(OperationType...)`,
  `filterCollection(String...)` (useful on global listeners).
- `withMode(FullDocument)`: deprecated, ignored (see above).
- `register()`.

### CollectionWatchBuilder

Obtained from `manager.watch(name)`. Builds an aggregation pipeline applied by MongoDB; call
`start()` last. It only configures and starts the worker: **you still register listeners.**

- `filter(Bson)`, `onlyInserts()`, `onlyUpdates()`, `onlyDeletes()`, `onOperations(String...)`
  (e.g. `"insert"`),
- `whereField(String, Object)` and `whereFieldExists(String)` (match on `fullDocument.<field>`),
- `filterOnUpdate(Bson)` and `whenFieldUpdated(String)` (match on `updateDescription.updatedFields.<field>`),
- `fullDocument(FullDocument)`: deprecated, ignored (see above).

The pipeline is stored per collection and used when the worker is (re)created. Calling `start()` a
second time while the worker already runs replaces the stored pipeline but does not restart the
worker: unwatch first.

### ChangeStreamWorker and WorkerStatus

`ChangeStreamWorker` is created by the manager; its constructor is public but you should not need
it. Useful getters: `getStatus()`, `getEventsProcessed()`, `getCollectionName()`,
`getResumeToken()`, `getFullDocumentMode()`. `WorkerStatus` values: `STARTING`, `RUNNING`,
`RECONNECTING`, `ERROR`, `STOPPED`.

## Recipes

### Invalidate a cache

```java
manager.createListenerBuilder(event -> cache.invalidate(((BsonObjectId) event.getOid()).getValue()), "Article")
        .filterOperation(OperationType.UPDATE, OperationType.REPLACE, OperationType.DELETE)
        .register();
```

This works in `DEFAULT` mode since only the id is needed. Do it in every instance of your service:
each one has its own stream, so each local cache is invalidated.

### React to one field change

```java
manager.watch("User").onlyUpdates().whenFieldUpdated("email").start();
manager.registerListener(event -> sendVerificationMail(event), "User");
```

The server only sends updates that touched `email`. `whenFieldUpdated` matches top-level field
names as they appear in `updateDescription.updatedFields`.

### Filter on document content

```java
manager.createListenerBuilder(event -> audit(event), "User")
        .filterField("role", "admin")
        .register();
```

Needs `UPDATE_LOOKUP` for update events, otherwise they are dropped (no full document).

### Observe all collections

```java
manager.watchAllCollections();
manager.registerListener(event -> LOGGER.debug("{}", event));
```

`watchAllCollections()` only covers collections that exist at the time of the call.

### Push to browsers

There is no WebSocket or SSE integration in this package. To push events to clients, register a
listener that hands the event to your own transport (a JAX-RS `Sse` sink, a WebSocket session
set, a queue) and do the delivery outside the listener, since listeners run on the worker thread.
Send only the fields the client may read (the identifier, or a DTO built from them): the event
carries the raw document, `@DataNotRead` fields included.

## Pitfalls and limits

- **Standalone MongoDB**: change streams are unavailable; the worker logs a `MongoException` every
  5 s. Check `getWorkerStatuses()` (stuck on `RECONNECTING`).
- **Mode is global and fixed at `start`.** Per-listener modes are ignored (deprecated API, a
  warning is logged). Calling
  `start(db, otherMode)` on a running manager restarts it and drops every listener (and resume
  token), so register listeners afterwards. `setDefaultFullDocumentMode` does not touch running workers.
- **`stop()` forgets everything**: listeners, resume tokens and the owned client. After a
  restart events produced while stopped are not replayed.
- **Registering blocks until the stream is open**, a few milliseconds per collection (a
  connection is opened for each stream). Registering 50 collections one call at a time opens them
  one after the other; pass a `Set<String>` to open them in parallel. If MongoDB does not answer, each
  registration waits at most 10 s and logs a WARN.
- **Events can be lost** across restarts or when the oplog window is exceeded; resume tokens are
  not persisted. Treat notifications as hints (invalidate, refresh), not as a durable log. Use the
  [change journal](change_journal.md) if you need history.
- **Slow listeners block their collection.** Dispatch is synchronous on the worker thread; push
  heavy work to an executor.
- **Server-side filters on `fullDocument.*`** (`whereField`, `whereFieldExists`) do not match
  update events in `DEFAULT` mode, because no full document is sent.
- **`getOid()` is a `BsonValue`**, not an `ObjectId`. Convert it (for example
  `event.getOid()` cast to `BsonObjectId`, then `.getValue()`) before comparing with entity ids or
  using it as a cache key.
- **`unregisterListener(listener)` uses `equals`**. Two distinct lambdas never match; keep the
  instance you registered.
- **Listener registered twice** with different filters is called once per matching registration;
  registrations with the same listener and equal filter collapse into one.
- **Singleton and tests**: it is process-wide state. Call `clearAllListeners()` between tests and
  `stop()` at the end.
- **Multiple instances** of your service all receive every event; do not use a listener for work
  that must run once cluster-wide without your own coordination.

## Tests

Tests live in `src/test/test/atriasoft/archidata/dataStreamEvent/` and need the MongoDB replica set
used by the other integration tests (`ConfigureDb.configure()`):

- `ChangeNotificationManagerTest`: lifecycle, registration, counts.
- `BasicCRUDOperationsTest`: insert, update, delete events.
- `ClientSideFilteringTest`: `ListenerRegistrationBuilder` filters.
- `MultiCollectionTest`: several collections and global listeners.
- `FullDocumentModesTest`: effect of the global mode (one test is `@Disabled` as flaky).
- `ChangeNotificationIntegrationTest`: end-to-end behaviour including worker restarts.
- `TestChangeStreamRestart`: a write right after `registerListener()` is notified, also after
  `stop()` then `start()` in the same JVM, and `stop()` returns once its workers have ended.
- `TestChangeStreamNoEventLost`: an event read by a worker that `unwatchCollection()` stops before it
  dispatches it reaches the listeners once, through the next worker, also when it is the first event of
  that worker (the interleaving is forced through a database whose `getCollection()` runs code with the
  manager's write lock held); a client filter that throws does not take the event from the other
  listeners; a listener that throws an `AssertionError` ends the worker with an ERROR log and the
  `STOPPED` status, and the event reaches the listeners once the collection is watched again.
- `TestHelper`: polling helpers (`waitForEvents`, `waitForCondition`,
  `waitForStreamInitialization`) used instead of `Thread.sleep`.

Pattern: start the manager in `@BeforeAll` with the mode under test, register the listener, wait for
the stream to be ready with `TestHelper.waitForStreamInitialization(manager, collection, timeoutMs)`,
write through `DataAccess`, then `TestHelper.waitForEvents(events, n, timeoutMs)`. Clean up with
`clearAllListeners()` in `@AfterEach` and `stop()` in `@AfterAll`.
