Query statistics
================

The package `org.atriasoft.archidata.dataAccess.statistic` counts how many times
each *shape* of database query is executed and tells you which compound index
each shape would have used. It answers the question "which indexes are worth
declaring?" from real traffic instead of guesses.

The mode is **off by default** and costs one field read per query when off.


When to use it
--------------

Turn it on on a staging or production server for a few days, then read the file
and declare the indexes of the most frequent shapes (see
[Indexes](indexes.md)). Turn it off again once the indexes are in place: the
counters live in memory and the file is rewritten on a schedule.

It tells you what is queried, not what is slow. A shape run a million times on a
50-document collection needs no index; check the real size of the collection
before acting on a count.


Enabling it
-----------

Two settings, read from `ConfigBaseVariable` (see [tools.md](tools.md)):

| Environment variable | Setter | Default | Role |
|---|---|---|---|
| `QUERY_STATISTICS_FILE` | `setQueryStatisticsFile(String)` | unset: mode off | path of the JSON report; a non-empty value enables the mode |
| `QUERY_STATISTICS_PERIOD` | `setQueryStatisticsPeriod(String)` | `*/10 * * * *` | cron expression of the flush (see [cron.md](cron.md) for the syntax) |

```bash
QUERY_STATISTICS_FILE=./data/query-stats.json ./run-server.sh
```

There is no call to add in your code: `DBAccessMongo` records every filtered
read, count, update, soft delete, restore and hard delete itself.

On first use the statistics start a private `CronScheduler` (daemon threads:
it never keeps the JVM alive) that rewrites the file every period, and install
a shutdown hook that writes it one last time.
If the file already exists it is read back, so the counters keep growing across
restarts.


Reading the report
------------------

The file is a `QueryStatReport`: `generatedAt` and a list of `queries` sorted by
decreasing `count`. The write is atomic (temporary file then move), so it can be
read while the server runs.

```json
{
  "generatedAt": "2026-10-15T09:50:00.123Z",
  "queries": [
    {
      "signature": "user|FIND|E:companyId|S:createdAt:-1|R:",
      "collection": "user",
      "operation": "FIND",
      "equality": ["companyId"],
      "sort": ["createdAt:-1"],
      "range": [],
      "softDeleteField": "deleted",
      "containsOr": false,
      "count": 148203,
      "suggestedIndex": "{\"companyId\": 1, \"createdAt\": -1}"
    }
  ]
}
```

The shape is the filter with the values removed, split following the MongoDB
ESR rule (Equality, Sort, Range):

- `E:` fields matched with `$eq` (implicit or explicit), `$in`, `$all`, or by an
  exact sub-document; sorted alphabetically.
- `S:` the sort fields in their original order, each with its direction
  (`createdAt:-1`).
- `R:` fields matched by any other operator (`$gt`, `$lt`, `$ne`, `$exists`,
  `$regex`...); sorted alphabetically. A field with both an equality and a
  range predicate is classified as a range.
- `$elemMatch` conditions are reported as `field.subField`.

`suggestedIndex` is the compound index in ESR order, as a JSON object, or
`null` when the query neither filters nor sorts (a full scan no index can help).
Declare it with `@Index({"companyId", "-createdAt"})` on the entity, or with
`IndexRegistry.declare`.

`operation` is one of `FIND`, `COUNT`, `UPDATE`, `DELETE`, `DELETE_SOFT`,
`RESTORE` (`QuerySignature.Operation`).


Caveats
-------

- `containsOr: true`: the filter holds a `$or` or `$nor`; one compound index
  cannot serve it, MongoDB needs one index per branch. The reported fields are
  the union of all the branches, so `suggestedIndex` is only a starting point.
- The soft-delete field (`softDeleteField`) is left out of `suggestedIndex` on
  purpose. It is filtered with a `$or` (false or missing), which a plain
  compound index serves poorly. The usual answer is a partial index
  (`partialFilter`), decided per collection.
- A field used both in the sort and in a range keeps its first position in the
  suggested index (a key can appear once).


Reference
---------

| Class | Member | Role |
|---|---|---|
| `QueryStatistics` (final, static) | `isEnabled()` | `true` when `QUERY_STATISTICS_FILE` is set |
| | `record(String collection, QuerySignature.Operation, Bson filter, Document sort, String softDeleteField)` | count one execution; never throws |
| | `flush()` | write the report now (also called by the schedule and the shutdown hook) |
| | `buildReport()` | the current `QueryStatReport`, without writing it |
| | `reset()` | drop the in-memory counters and stop the flush schedule, leave the file untouched; for tests |
| `QuerySignature` | `of(collection, operation, filter, sort, softDeleteField)` | compute a shape |
| | `getKey()`, `getCollection()`, `getOperation()`, `getEquality()`, `getSort()`, `getRange()`, `getSoftDeleteField()`, `isContainsOr()`, `getSuggestedIndex()` | read it |
| `QueryStatReport` | `generatedAt`, `queries` | root of the file |
| `QueryStatEntry` | `signature`, `collection`, `operation`, `equality`, `sort`, `range`, `softDeleteField`, `containsOr`, `count`, `suggestedIndex` | one line of the file (public mutable fields, this is the on-disk format) |


Pitfalls
--------

- The signature key ignores `containsOr` and `softDeleteField`: two queries that
  differ only by those share one counter, and the entry shows the description
  of the first one met since start-up.
- The counters of a shape not met again since a restart keep the description
  read back from the file. If you rename a field, delete the file to drop the
  stale shapes.
- Calling `record` from your own code is possible but pointless: only the
  `DBAccessMongo` paths feed the report.
