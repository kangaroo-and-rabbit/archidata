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
- **The FullDocument mode is global.** It is fixed by `start(..., mode)` and applied to every worker.
  There is one worker per collection, so a listener cannot get another mode: the `mode` argument
  of `registerListener(..., mode)`, `withMode(...)` and `CollectionWatchBuilder.fullDocument(...)`
  is **ignored**. These overloads are `@Deprecated` and log a warning when the requested mode
  differs from the global one.
  `start(database)`, `start(dbConfig)` and `start()` use `FullDocument.DEFAULT`.

  | Mode | INSERT / REPLACE | UPDATE | DELETE |
  |---|---|---|---|
  | `DEFAULT` | `getFullDocument()` set | `getFullDocument()` is `null`, use `getUpdatedFields()` | `null` |
  | `UPDATE_LOOKUP` | set | current document (may be `null` if deleted meanwhile) | `null` |
  | `WHEN_AVAILABLE` | set | post-image if the collection has change stream pre/post images enabled | `null` |

- **Events are dispatched on the worker thread**, one event at a time per collection, in stream
  order. For each event the manager calls the global listeners first, then the listeners of the
  collection. A client-side filter that returns `false` skips the listener; an exception thrown by a
  listener is caught and logged, and does not affect other listeners.
- **Resume tokens.** The worker keeps the token of the last event it handled. If the stream errors
  it re-opens with `resumeAfter(token)`. When a worker is stopped through `unwatchCollection(...)`
  the token is saved by the manager and reused if the collection is watched again. Tokens live in
  memory only: they do not survive a process restart or `stop()`.
- **Error handling in the worker loop** (see `WorkerStatus`):
  `MongoInterruptedException` and `InvalidResumeToken` (code 260) reset the token and retry after
  1 s; other `MongoCommandException` / `MongoException` set `RECONNECTING` and retry after 5 s;
  any other exception sets `ERROR` and retries after 5 s. The loop ends only on `stop()`
  (`STOPPED`). The delay is fixed (no exponential backoff, no retry limit).
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
| `start(MongoDatabase, FullDocument)` | throws `IllegalArgumentException` on `null`. If already running with the same mode: no-op. With another mode: `stop()` then restart, which **clears all listeners** |
| `start()`, `start(DbConfig)`, `start(DbConfig, FullDocument)` | build and own a `MongoClient`; throw `DataAccessException` |
| `stop()` | stops workers, shuts the pool down (1 s then `shutdownNow`), clears workers, listeners and resume tokens, closes an owned client, resets the global mode to `UPDATE_LOOKUP` |
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
| `registerListener(listener, String)` / `(listener, Set<String>)` | collection listeners; the overloads with a `FullDocument` argument ignore it |
| `createListenerBuilder(listener)` / `(listener, String)` / `(listener, Set<String>)` | returns a `ListenerRegistrationBuilder` (client-side filters) |
| `unregisterListener(listener)` | removes it everywhere (matched with `equals`) |
| `unregisterListener(listener, String)` | removes it from one collection only |
| `clearAllListeners()` | removes all listeners, keeps the workers running |
| `getListenerCount()` | |

Monitoring and configuration: `getWorkerStatuses()` (`Map<String, WorkerStatus>`),
`getCollectionModes()` (the global mode, per watched collection), `getTotalEventsProcessed()`,
`getDefaultFullDocumentMode()`, `setDefaultFullDocumentMode(FullDocument)` (only affects workers
created afterwards), `computeEffectiveMode(String)` (always the global mode).

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
- `getTimestamp()` (cluster time, or receive time if absent), `getResumeToken()`.
- `toJson()` / `toJsonExtended()` serialize the full document, or return `null` without one.

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

## Pitfalls and limits

- **Standalone MongoDB**: change streams are unavailable; the worker logs a `MongoException` every
  5 s. Check `getWorkerStatuses()` (stuck on `RECONNECTING`).
- **Mode is global and fixed at `start`.** Per-listener modes are ignored (deprecated API, a
  warning is logged). Calling
  `start(db, otherMode)` on a running manager restarts it and drops every listener (and resume
  token), so register listeners afterwards. `setDefaultFullDocumentMode` does not touch running workers.
- **`stop()` forgets everything**: listeners, resume tokens and the owned client. After a
  restart events produced while stopped are not replayed.
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
- `TestHelper`: polling helpers (`waitForEvents`, `waitForCondition`,
  `waitForStreamInitialization`) used instead of `Thread.sleep`.

Pattern: start the manager in `@BeforeAll` with the mode under test, register the listener, wait for
the stream to be ready with `TestHelper.waitForStreamInitialization(manager, collection, timeoutMs)`,
write through `DataAccess`, then `TestHelper.waitForEvents(events, n, timeoutMs)`. Clean up with
`clearAllListeners()` in `@AfterEach` and `stop()` in `@AfterAll`.
