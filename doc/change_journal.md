# Change journal (incremental capture of the modified documents)

The change journal periodically captures the documents modified in your collections and appends
them to a **single** journal collection. Each entry keeps the name of the source collection, the
serialized document, and the date it was recorded.

Unlike the [backup engine](../src/main/org/atriasoft/archidata/backup/BackupEngine.java) — which
produces `tar.gz` archives outside of the database — the journal stays *inside* MongoDB and is
meant for change history, auditing, and point-in-time inspection of a document.

## Overview

At every run, and for **each** journalized collection:

1. the per-collection marker is read: it holds the upper bound of the previous successful capture;
2. the documents modified since that date are selected (`updatedAt`, falling back to `createdAt`
   for documents that carry no `updatedAt`);
3. one entry per selected document is appended to the journal collection;
4. the marker moves forward to the run date — but only once the collection is fully written.

The journal is **append-only**: every captured version adds an entry, so the history of a document
is the set of entries sharing the same (`collectionName`, `sourceId`). Trimming that history is the
job of the [purge](#retention-purge), run separately.

## Data layout

### `changeJournal` — the single storage collection (`ChangeJournalEntry`)

| Field | Type | Meaning |
|-------|------|---------|
| `_id` | `ObjectId` | identifier of the journal entry |
| `collectionName` | `String` | name of the **source** collection (table) the document comes from |
| `sourceId` | `String` | string representation of the source document primary key |
| `data` | `String` | the source document serialized in **BSON extended JSON** (lossless) |
| `sourceUpdatedAt` | `Date` | `updatedAt` (or `createdAt`) carried by the document when captured |
| `recordedAt` | `Date` | date the entry was written — shared by every entry of the same run |

### `changeJournalMarker` — one date marker per table (`ChangeJournalMarker`)

| Field | Type | Meaning |
|-------|------|---------|
| `collectionName` | `String` | name of the journalized source collection (unique index) |
| `lastSavedAt` | `Date` | upper bound of the last successful capture — the next run starts strictly after it |
| `lastRunAt` | `Date` | date of the last successful run for this collection |
| `lastCount` | `long` | number of entries written by the last run |
| `totalCount` | `long` | cumulated number of entries written for this collection |

Both collection names can be changed:
`new ChangeJournalEngine("myJournal", "myJournalMarker")`.

## Capture

```java
final ChangeJournalEngine journal = new ChangeJournalEngine();
journal.addClass(User.class, Media.class);   // collection name resolved from the annotations
journal.addCollection("otherCollection");    // ... or given directly

// Run it now:
final ChangeJournalReport report = journal.run();
LOGGER.info("{} entries recorded at {}", report.totalCaptured(), report.runDate());
```

Registering nothing and calling `runAll()` journalizes **every** collection discovered in the
database, excluding the `system.*` ones and the journal's own collections.

### Scheduling it (the usual case)

```java
final CronScheduler scheduler = new CronScheduler();
scheduler.addTask("change-journal", "*/15 * * * *", journal.asCronTask());
scheduler.start();
```

`asCronTask()` never propagates a failure: it logs it. Use `asCronTask(true)` for the `runAll()`
variant.

### Report

`ChangeJournalReport` describes one run:

| Member | Meaning |
|--------|---------|
| `runDate()` | date the run started; also the `recordedAt` of every entry it wrote |
| `capturedByCollection()` | number of entries written, per source collection (a collection with nothing to capture is present with `0`) |
| `failedCollections()` | collections whose capture failed; their marker was left untouched, so the next run retries them |
| `totalCaptured()` | total number of entries written |
| `isSuccess()` | `true` when no collection failed |

Collections are independent: one failing collection does not stop the others, and it is not skipped
either — it is simply retried at the next run.

## Configuration

| Setter | Default | Effect |
|--------|---------|--------|
| `setInitialCapture(FULL\|SKIP)` | `FULL` | what the very first capture of a collection does: a full snapshot, or nothing but setting the marker |
| `setSafetyOverlapMillis(long)` | `0` | widens the lower bound of the selection — see below |
| `setBatchMaxDocuments(int)` | `1000` | maximum number of entries per `insertMany` |
| `setBatchMaxBytes(int)` | `8 MiB` | maximum accumulated serialized size per `insertMany` |

### Time bounds

The run date is frozen **before** reading anything and used as the upper bound of the selection
(`updatedAt > marker` and `updatedAt <= runDate`). A document modified while the run is in progress
is therefore dated after the bound: it is captured by the next run instead of being lost — and
never captured twice.

`setSafetyOverlapMillis(n)` subtracts `n` milliseconds from the marker when selecting. It is useful
when the `updatedAt` dates are produced by several hosts whose clocks may drift: a document dated a
few milliseconds in the past of the marker would otherwise be missed. The price is duplicated
entries for the documents modified inside the window — harmless in an append-only journal.

### Markers

```java
final List<ChangeJournalMarker> markers = journal.getMarkers();
journal.resetMarker("user");   // next capture of "user" behaves like a first capture
journal.resetAllMarkers();
```

## Reading the journal back

Entries are regular documents, readable with the standard data access:

```java
final List<ChangeJournalEntry> entries = DataAccess.gets(ChangeJournalEntry.class,
        new Condition(Filters.and(
                Filters.eq(ChangeJournalEntry::getCollectionName, "user"),
                Filters.eq(ChangeJournalEntry::getSourceId, userId.toHexString()))),
        OrderBy.desc(ChangeJournalEntry::getRecordedAt));

// The stored data round-trips back to the source document (extended JSON is lossless):
final Document restored = Document.parse(entries.get(0).getData());
```

## Retention (purge)

Retention runs as a **separate** job. Two rules are combined, per source document — that is per
(`collectionName`, `sourceId`) pair:

- the `minVersions` most recent entries are **always** kept, whatever their age;
- beyond those, an entry is removed once it is older than `maxAge`.

So a document modified once two years ago keeps its history (nothing else describes it), while a
document modified every hour keeps its recent versions plus everything inside the age window.

```java
final ChangeJournalPurge purge = new ChangeJournalPurge();

// Keep the 5 most recent versions of each document, drop the rest after 90 days:
final long removed = purge.purge(5, Duration.ofDays(90));

// Count what would be removed, without removing anything:
final long candidates = purge.purgeDryRun(5, Duration.ofDays(90));

// Scheduled, every night at 03:30:
scheduler.addTask("change-journal-purge", "30 3 * * *", purge.asCronTask(5, Duration.ofDays(90)));
```

`minVersions = 0` applies the age rule alone (even the last version of a document is removed once
it is older than `maxAge`).

## Indexes

Both engines create their indexes on first use:

| Collection | Index | Used by |
|------------|-------|---------|
| `changeJournal` | `{collectionName: 1, recordedAt: -1}` | reading the journal of one collection, most recent first |
| `changeJournal` | `{collectionName: 1, sourceId: 1, recordedAt: -1, _id: -1}` | reading the history of one document — and the exact order the purge walks |
| `changeJournalMarker` | `{collectionName: 1}` (unique) | one marker per collection, enforced by the database |

## Memory usage

Both jobs are streamed and bounded whatever the data size is: the capture writes by batches
(`setBatchMaxDocuments` / `setBatchMaxBytes`), and the purge walks a cursor with a projection
covered by its index and deletes by batches of 1000 identifiers.

## Limitations

Detection relies on the dates carried by the documents:

- a document modified in place **without** refreshing its `updatedAt` is not detected;
- **deletions are never detected** — nothing is written when a document disappears, and the entries
  of a deleted document stay in the journal;
- a document carrying neither `createdAt` nor `updatedAt` can only be captured by the initial full
  capture, never by the incremental runs — capturing it at every run would duplicate it forever.

Models extending `GenericTiming` (so `OIDGenericData`, `UUIDGenericData`, …) carry the required
timestamps automatically.
