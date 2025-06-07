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
