# Indexes

Indexes are declared next to the entities and synchronized with the database at startup: what the
code declares is what the database holds — no more accumulating the leftovers of every past
version, and no more index that only exists on the machine where someone created it by hand.

## Declaring

Three equivalent ways, mixable, so a project can keep its declarations where it prefers.

### On the class — compound indexes

```java
@Index({"companyId", "-createdAt"})          // '-' makes the key descending
@Index(value = "email", unique = true)
public class User extends OIDGenericData {
    public ObjectId companyId;
    public Date createdAt;
    public String email;
}
```

### On a property — the single-key short form

```java
public class User extends OIDGenericData {
    @Indexed(unique = true)
    public String login;

    @Indexed(expireAfterSeconds = 3600)      // TTL: MongoDB removes the document 1h after the date
    public Date sessionExpireAt;
}
```

### Programmatically — type-safe

```java
IndexRegistry.declare(User.class,
        IndexSpec.asc(User::getCompanyId).thenDesc(User::getCreatedAt),
        IndexSpec.asc(User::getEmail).unique());
```

`asc`/`desc` open the specification, `thenAsc`/`thenDesc` append the following keys. Java forbids a
static and an instance method with the same signature, and the distinct names also make the key
order — which matters for a compound index — obvious at the call site.

### Options

| Option | Meaning |
|--------|---------|
| `unique` | rejects duplicated values on the keys |
| `sparse` | skips the documents that do not carry the fields |
| `expireAfterSeconds` | TTL index: requires a single date key |
| `partialFilter` | restricts the index to the documents matching a JSON filter |
| `name` | explicit name, always prefixed by `kar_` |

Field names are the **structural** ones — the name of `@Column(name = "...")` when present, the
same ones `FilterValue` uses. A dotted path indexes inside an embedded sub-document
(`"address.city"`).

> `@Column(unique = true)` creates **nothing**. `@Column` carries JPA metadata archidata does not
> act upon, so the constraint would exist only in the source. A non-primary-key field relying on it
> raises a warning naming the annotation to use instead.

## Synchronizing

```java
final IndexEngine indexes = new IndexEngine();
indexes.addClass(User.class, Media.class);
indexes.synchronize();
```

For every registered collection, the engine compares the declarations with what
`listIndexes()` returns and applies the difference:

| Situation | Action |
|---|---|
| declared, missing in the database | **create** |
| declared, present, same definition | nothing |
| declared, present, different definition | **replace** (MongoDB cannot alter an index in place) |
| present, not declared any more | **drop** |
| `_id_` | **never touched** |
| collection not registered | **never touched** |

Creations are applied before drops: a crash in the middle leaves the collection over-indexed —
slow — rather than under-indexed, where queries run without an index at all.

### Reviewing before applying

On a large collection, creating an index is not free. `plan()` computes the whole difference
without touching anything:

```java
final IndexPlan plan = indexes.plan();
if (!plan.isUpToDate()) {
    LOGGER.info("indexes to update:\n{}", plan.describe());
}
```

```
user:
    CREATE kar_companyId_1_createdAt_-1 {"companyId": 1, "createdAt": -1}  <- missing
    REPLACE kar_email_1_3f2a91bc {"email": 1}                              <- definition changed
    DROP kar_oldField_1                                                    <- not declared any more
```

`plan()` always reads the real state of the database, cache or not.

### Where it runs

```java
public void migrateDB() throws Exception {
    migrationEngine.migrateWaitAdmin(GlobalConfiguration.dbConfig);

    final IndexEngine indexes = new IndexEngine();
    indexes.addClass(User.class, Media.class);
    indexes.synchronize();   // after the migrations: they may create or rename collections
}
```

The engine **never reads the migration state**. A server that receives a duplicated database and
never runs the migrations can still bring its indexes up to date — the only thing it needs is the
list of entities, which comes from the code.

## Options of the engine

| Setter | Default | Effect |
|--------|---------|--------|
| `setDropUnmanaged(boolean)` | `true` | drop the indexes the code does not declare |
| `setFailFast(boolean)` | `true` | stop and propagate at the first index that cannot be applied |
| `setUseCache(boolean)` | `true` | skip the collections the cache proves are in sync |
| `setForceCheck(boolean)` | `false` | ignore the cache for this run |

### Foreign indexes

By default the database ends up strictly conform to the code, which means **an index created by
hand is dropped** — including a text or geospatial index, which this engine cannot declare. Two
ways out: `setDropUnmanaged(false)`, which restricts the engine to the `kar_*` indexes and
preserves everything else, or declaring the index in the code.

Managed indexes carry a canonical name prefixed by `kar_`. The name embeds a short hash of the
definition as soon as it cannot be represented literally (options set, dotted path, name too long),
so two different definitions never share a name, and a modified declaration produces a new name —
which is what makes the change be applied instead of leaving a stale index behind.

### Failure

By default, the first index that cannot be created stops the synchronization and propagates a
`DataAccessException` — a unique index rejected by duplicated data is a problem to fix, not to log
while the server starts anyway. `setFailFast(false)` collects the failures in the `IndexReport`
instead, and keeps going.

## The `KAR_index` collection

The engine caches, per collection, a fingerprint of what it synchronized:

```javascript
{ _id: "user", fingerprint: "a3f1…", indexes: ["kar_email_1"], syncedAt: ISODate(…), owner: "…" }
```

A run whose fingerprint matches skips the collection entirely — no `listIndexes`, no diff. The
fingerprint covers the declarations **and** `dropUnmanaged`, so turning the strict mode on
invalidates a cache written while foreign indexes were tolerated.

Be aware of what this cache is: it records what archidata *believes* it did, not what the database
holds. An index dropped by hand stays invisible until the declarations change. `setForceCheck(true)`
(or dropping the collection) forces a full verification.

The same collection holds a **lock**: two instances starting together do not drop and recreate the
same indexes concurrently — the second one skips its run and logs it. The lease expires after two
minutes, so an instance killed mid-synchronization does not block the next one.

When a collection is synchronized with a fingerprint different from the one written minutes before,
the engine logs a warning: two versions of the code are undoing each other's indexes at every
restart. It does not block — that would prevent a legitimate deployment — but the symptom becomes
visible instead of looking like an unexplained slowness.

## Soft-deleted entities: index only the living documents

An entity with a soft-delete field is read with `{deleted: false}`, so an index covering the whole
collection also indexes documents no query will ever return. A **partial index** holds only the
living ones — smaller, and cheaper to maintain:

```java
@Index(value = {"companyId", "-createdAt"}, partialFilter = "{\"deleted\": false}")
public class Article extends OIDGenericDataSoftDelete { … }
```

Do **not** index the `deleted` field alone. With two possible values it filters almost nothing: on
a collection where most documents are alive, walking the index then fetching 95 % of the documents
in random order costs more than a sequential scan. The soft-delete predicate rides for free on the
indexes of the fields you really query.

> This only works because the predicate is a plain equality. MongoDB uses a partial index only when
> the query implies its filter — an `$or` also accepting the documents without the field implies
> nothing, and such a query falls back to a collection scan. That is why archidata always writes
> the field on insert, and why a document written outside of archidata without it is invisible.

## Limitations

- Only ascending/descending keys are declarable: no text, geospatial, hashed or wildcard index.
  Create those by hand and use `setDropUnmanaged(false)`, otherwise the strict mode removes them.
- The change journal (`ChangeJournalEngine`) creates its own indexes at first use, deliberately:
  it must work without anyone registering its collections here. Do not declare them again through
  `IndexEngine` — the two would fight over the same data with different names.
- An index is created on a collection that does not exist yet, which creates it empty. That is the
  intended behaviour: the collection is born with its indexes.
