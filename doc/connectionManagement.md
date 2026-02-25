Connection Management
=====================

By default, connection handling is very simple:  
Just use `DataAccess`, and it will **open and close a connection for every call**.

This works well, but creates a **new connection every time `DataAccess` is invoked**, even within the same thread.

Persistent Connection for a Thread
----------------------------------

Internally, we use `@ref DataAccessConnectionContext`, which manages a `ThreadLocal` to store a persistent connection throughout a request.  
This means that **all database calls within the same context share the same DB socket**, improving performance and reducing overhead.

To benefit from this more efficient connection management, you have two main approaches:


Manual Connection Context
-------------------------

Use this pattern for services or internal periodic jobs:

```java
try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
    DBAccess db = ctx.get();
    // Your database operations
}
```

This ensures that all operations inside the block use the **same database connection**.


Automatic Connection via API Decoration
---------------------------------------

For Jakarta REST APIs, you can **automatically activate a shared DB connection per request** using the `@DataAccessSingleConnection` annotation.

This mechanism is backed by the filter:  
**`DataAccessRetentionConnectionFilter`**

### Register the Filter

You must register this filter in your JAX-RS application configuration:

```java
final ResourceConfig rc = new ResourceConfig();
// Required to support @DataAccessSingleConnection
rc.register(DataAccessRetentionConnectionFilter.class);
```

### Annotate Your Endpoints

Then annotate your endpoint methods or classes as needed:

```java
@Path("/users")
public class UserResource {

    @GET
    @Path("/{id}")
    @DataAccessSingleConnection
    public User getUser(@PathParam("id") String id) {
        DBAccess db = DataAccessConnectionContext.getConnection();
        return db.queryUserById(id);
    }
}
```


Accessing the Database Connection
---------------------------------

Once `DataAccessConnectionContext` is active (via manual context or annotation), you can retrieve the connection like this:

```java
DBAccess db = DataAccessConnectionContext.getConnection();
```

Alternatively, calling `DataAccess.query(...)` or similar methods will automatically use the shared connection **if a context is active**.

The explicit form `getConnection()` is useful if you need to:
  - Perform DB-specific initialization steps
  - Interact with advanced connection features


Transactions
------------

Archidata supports MongoDB multi-document transactions. All CRUD operations on a `DBAccessMongo` instance can participate in a transaction when one is active.

**Important:** MongoDB transactions require a **replica set** (even a single-node replica set). Standalone MongoDB instances do not support transactions.


### Manual Transaction Control

Use `startTransaction()`, `commitTransaction()`, and `abortTransaction()` directly on `DBAccessMongo`:

```java
final DBAccessMongo db = DBAccessMongo.createInterface();
db.startTransaction();
try {
    db.insert(entity1);
    db.insert(entity2);
    db.commitTransaction();
} catch (Exception ex) {
    db.abortTransaction();
    throw ex;
} finally {
    db.close();
}
```


### TransactionContext (try-with-resources)

`TransactionContext` provides automatic abort if `commit()` is not called:

```java
try (TransactionContext tx = new TransactionContext(db)) {
    db.insert(entity1);
    db.insert(entity2);
    tx.commit();
}
// If commit() was not called (e.g., exception thrown), the transaction is auto-aborted.
```


### TransactionContext.getTransactionContext()

Within a `@DataAccessSingleConnection` context, you can use the convenience factory to auto-retrieve the current thread's connection:

```java
try (TransactionContext tx = TransactionContext.getTransactionContext()) {
    final DBAccessMongo db = DataAccessConnectionContext.getConnection();
    db.insert(entity1);
    db.insert(entity2);
    tx.commit();
}
```


### DataAccess.inTransaction()

The static facade provides a lambda-based API that manages the entire lifecycle (connection + transaction):

```java
DataAccess.inTransaction(db -> {
    db.insert(entity1);
    db.insert(entity2);
});
// Both inserts are committed atomically, or both are rolled back on error.
```


### Automatic Transactions via @DataAccessSingleConnection

For REST endpoints, you can enable automatic transactions using `@DataAccessSingleConnection(transactional = true)`. The filter starts a transaction at the beginning of the request and:
- **Commits** on success (2xx response)
- **Aborts** on error (non-2xx response or exception)

```java
@POST
@DataAccessSingleConnection(transactional = true)
public User createUserWithAccount(final UserRequest req) throws Exception {
    final DBAccessMongo db = DataAccessConnectionContext.getConnection();
    final User user = db.insert(new User(req.name));
    final Account account = new Account(user.getOid());
    db.insert(account);
    return user;
    // Auto-committed on success, auto-aborted on exception
}
```


Virtual Thread Support
----------------------

All connection management methods are compatible with virtual threads (Java 21+).

Each virtual thread has its own `ThreadLocal` storage, which is **not shared** with the carrier thread. This means database connections are safely isolated between virtual threads.

When using Grizzly with virtual threads for SSE scalability:

```java
for (NetworkListener listener : server.getListeners()) {
    var transport = listener.getTransport();
    transport.setWorkerThreadPool(Executors.newVirtualThreadPerTaskExecutor());
}
```

This allows thousands of concurrent connections (like SSE) without exhausting the thread pool, while maintaining proper connection isolation per request.
