Backup and restore
==================

`BackupEngine` (`org.atriasoft.archidata.backup`) writes the MongoDB collections
(and optionally the media files of the data folder) into a single `.tar.gz`
archive, restores such an archive into an empty database, and prunes old
archives with a retention policy. It can also make an in-memory snapshot of the
collections, which is meant for tests.

This page covers the archive layout and naming, full and delta backups,
restore, retention, the snapshot API, the traps around file names, and
`BackupService`, the shared job that applications plug in.


When to use it
--------------

Use it for scheduled application-level backups (nightly full backup, a delta
every few minutes) and for seeding a fresh database from an archive. Karideo
and karusic schedule a nightly `BackupJob` with it; karideo's `RestoreBackup`
and `Initialization` restore from it. For a nightly full backup, use
[`BackupService`](#backupservice-the-nightly-job-of-an-application) rather than
writing such a job again.

It is not a replacement for `mongodump` on a very large deployment: it reads
the collections through a cursor and writes JSON. It does not capture indexes
(declare them in the code and let the `IndexEngine` of
[Indexes](indexes.md) recreate them) and it does not capture users, roles or any database configuration.
For the incremental capture of modified documents see also the
[Change Journal](change_journal.md).


Minimal example
---------------

```java
final BackupEngine engine = new BackupEngine(
        Path.of(ConfigVariable.getBackupFolder()),   // where the archives live
        ConfigBaseVariable.getDBName(),              // archive prefix AND database name
        BackupEngine.EngineBackupType.JSON_EXTENDED);
engine.setEnableStoreOrRestoreData(false);           // collections only, no media files
engine.addClass(User.class, Media.class);            // or addCollection("counters")

// nightly, off the cron thread: see BackupService below
final String sequence = ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS")) + "_full";
engine.store(sequence);                              // -> <dir>/<db>_<sequence>.tar.gz

// weekly pruning
engine.clean(new RetentionPolicy());                 // 7 days all, 3 months daily, 24 months weekly
```

Restore into an empty database:

```java
engine.restoreLatest();   // latest full archive, then the latest delta based on it
```


Archive format
--------------

- File name: `<baseName>_<sequence>.tar.gz`, in the directory given to the
  constructor (created if missing). The archive is first written to a hidden
  file `.<name>_tmp` then moved into place, so a crash never leaves a truncated
  archive under the final name. A failed store deletes this file: it is a
  partial dump in clear, and neither `clean(...)` nor a listing sees it.
- One tar entry per collection: `<collection>.json`, newline-delimited JSON, one
  document per line. A big collection is split in chunks
  `<collection>__000001.json`, `__000002.json`... when a chunk reaches
  `setChunkMaxBytes` (default 32 MiB) or `setChunkMaxDocuments` (default
  100 000). The memory used by a backup is bounded by the chunk size, not by the
  collection size.
- Media files, when enabled, are stored under `data/` and restored into
  `ConfigBaseVariable.getMediaDataFolder()` (see [tools.md](tools.md)).
- A SHA-256 sum is written next to the archive, `<archive>.sha256` (`sha256sum`
  format), and checked before any restore (`restoreFile`, `restoreDeltaFile`,
  `restore`, `restoreLatest`): a mismatch throws `IOException` and nothing is
  restored. An archive without sidecar (older version, hand-made archive) is
  restored with a warning, unless `setRequireChecksum(true)`. `clean(...)`
  deletes the sidecar with its archive. The sum detects a corruption or an
  accidental change, not a deliberate one by someone who can write both files.

Security
--------

**An archive holds the whole database in clear**: every field of every
document, `@DataNotRead` ones included (password hashes, tokens), the change
journal, and the media files. Only the `@DataEncrypt` fields stay encrypted, as
long as their keys are not stored with the archives. archidata does not encrypt
the archives yet.

- The archive and its sidecar are created `rw-------` (owner only) on a POSIX
  file system; a store directory created by the engine is `rwx------`. An
  existing directory keeps its permissions: make it private to the service.
- Do not store the archives on a volume shared with other services or users,
  and encrypt them (outside archidata) before they leave the server.

Restoring an archive whose origin is not certain is bounded:

- Media entries are extracted only inside the media folder: the target path is
  normalized and compared as a `Path` (a sibling folder sharing the prefix,
  `/data/media-other`, is refused), and an existing symbolic link of the media
  folder that resolves elsewhere is never followed. Symbolic and hard link
  entries, and special entries, are skipped with a warning.
- The declared size of each entry (`setRestoreMaxEntryBytes`, 64 GiB by
  default) and the total of an archive (`setRestoreMaxTotalBytes`, 1 TiB) are
  bounded, as is the length of one document line (`setRestoreMaxLineChars`,
  64 Mi characters): beyond them the restore stops with an `IOException`.
- A collection name must match `[A-Za-z0-9_-][A-Za-z0-9_.-]{0,119}` and must not
  be `system.*` (`system.js` holds functions run by `$where`): otherwise the
  restore stops with an `IOException`.
- A sequence containing `/`, `\`, `..` or a NUL is refused
  (`IllegalArgumentException`): it can never leave the store directory.

A delta restore still replaces existing documents by `_id`: restore only the
archives you produced.

`EngineBackupType` selects the JSON dialect:

| Value | Output | Round trip |
|---|---|---|
| `JSON_EXTENDED` | BSON extended JSON | lossless (ObjectId, dates, longs keep their type). Recommended, used by every kar* back |
| `JSON_STANDARD` | BSON relaxed JSON | types may be widened (a long can come back as an int) |
| `JSON_EXTERNAL` | Jackson via `ContextGenericTools.createObjectMapper()`: dates as ISO strings, ObjectId as plain strings | lossy, not suitable for a faithful restore |

(The constant name `JSON_STANDARD` selects relaxed mode in the code, not the
canonical mode of the BSON specification.)


The sequence string
-------------------

`sequence` is free text chosen by the caller and put in the file name, but the
engine **parses a date from it** to sort the archives, apply the retention and
find the latest full backup. Supported prefixes:

```
yyyy-MM-dd
yyyy-MM-dd'T'HH-mm-ss
yyyy-MM-dd_HH-mm-ss
yyyy-MM-dd_HH:mm:ss
yyyy-MM-dd_HH:mm:ss.SSS
```

Every trailing alphabetic suffix is stripped before parsing (`_full`, `_delta`,
`_partial`..., possibly several: `2026-10-15_full_delta` parses as
2026-10-15). Suffixes the engine understands:

| Suffix | Meaning |
|---|---|
| `_delta` | incremental archive. Appended automatically by `storeDelta` / `storeAllDelta` when missing |
| `_partial` | a subset of the collections: never chosen as the base of a restore. The engine never adds it, the caller writes it into the sequence |
| anything else | ignored (`_full` is only a convention) |

Static helpers `parseDateFromSequence(String)` (returns `LocalDate` or `null`)
and `parseDateTimeFromSequence(String)` (returns `LocalDateTime` or `null`)
expose the parsing. Archives whose name cannot be parsed are ignored with a
warning by retention and restore-chain resolution. They are never deleted.


Full, delta and restore
-----------------------

```java
final Date fullDate = engine.storeAll("2026-10-15_03:00:00.000_full");
// ... later, only what changed since the full backup:
engine.storeAllDelta("2026-10-15_03:15:00.000", fullDate);   // becomes ..._delta
```

A delta contains the documents whose `updatedAt` or `createdAt` is at or after
`since`, plus every document that has neither field. Media files are filtered
on their modification time. Restoring is `restoreFile(full)` then
`restoreDeltaFile(delta)`, or simply `restoreLatest()`, which pairs the latest
full with the latest delta newer than it (`getLatestRestoreChain()` shows the
pair). `getLastFullBackupDate()` returns the date to pass as `since`.

Limits of a delta, by design: it never records deletions (a restore does not
remove documents), and a document without `updatedAt` that is modified in place
cannot be detected (it is however always included because it has no date).

On restore the target collections must be empty. `restoreFile(path, null)`
returns `false` (and logs an error) when the database already has collections;
with a collection name it restores only that collection. By default the inserts
use a relaxed write concern (`w:1`, no journal, unordered), which is much
faster and safe because a failed restore is re-run from scratch;
`setRestoreRelaxedWriteConcern(false)` restores the connection default.
When media are restored by `restoreFile`, the previous content of the media
folder is first moved into `<media>/history_restore/<timestamp>/`.


Retention
---------

`RetentionPolicy(int keepAllDays, int keepDailyMonths, int keepWeeklyMonths)`
is a record; its compact constructor rejects negative values and
`keepWeeklyMonths < keepDailyMonths`. `new RetentionPolicy()` is `(7, 3, 24)`.

| Age of the archive | Kept |
|---|---|
| younger than `keepAllDays` days | all of them |
| up to `keepDailyMonths` months | the latest one of each day |
| up to `keepWeeklyMonths` months | the latest one of each week (week fields of the default `Locale`) |
| older | none |

Delta archives are outside this table: a delta is kept only if it is newer than
the latest full backup, because a newer full supersedes it.

`clean(policy)` and `clean(policy, referenceDate)` delete and return the removed
paths; `cleanDryRun(policy)` and `cleanDryRun(policy, referenceDate)` return the
same list without deleting anything. Run a dry run first when you change the
policy.


Snapshots (tests and small data sets)
-------------------------------------

```java
final BackupSnapshot before = engine.snapshotAll();   // every collection but system.*
// ... a test mutates the database ...
engine.restore(before);                               // drops and refills those collections
```

`snapshot()` captures only the registered collections, `snapshotAll()` all of
them. A `BackupSnapshot` is a record holding a read-only
`Map<String, byte[]> collections()` (newline-delimited JSON bytes, same format as
the archive entries; the arrays are not copied, do not write to them) with
`size()` and `isEmpty()`. Unlike the archive restore, `restore(BackupSnapshot)`
**drops** every collection present in the snapshot first; collections absent
from it are left alone. The whole content is held in memory: do not use it on
large data.


BackupService: the nightly job of an application
------------------------------------------------

`BackupService` (`org.atriasoft.archidata.backup`) is the job every application
needs, so that it only declares its folder, its classes and its extra
collections:

```java
final BackupService backup = new BackupService(
        Path.of(ConfigVariable.getBackupFolder()),   // created if missing, rwx------
        ConfigBaseVariable.getDBName(),              // archive prefix AND database name
        Initialization.CLASSES_BASE,                 // model classes
        List.of());                                  // collections no class describes
scheduler.addTask("backup", "0 0 * * *", backup::startAsync);
```

- Database only: `setEnableStoreOrRestoreData(false)`, the media files are not
  in these archives.
- `counters` and the migration collection are always added, because a restored
  base needs them. There is no need to list them.
- A collection whose name a restore refuses (see [Security](#security)) makes
  the constructor throw `IOException`: the server does not start, rather than
  writing archives that cannot be restored.
- It writes `<baseName>_yyyy-MM-dd_HH:mm:ss.SSS_full.tar.gz`, the date in UTC.
- The constructor deletes the `.<baseName>_<sequence>.tar.gz_tmp` and
  `.<baseName>_<sequence>.tar.gz.sha256_tmp` files a killed process left, only
  regular files, only this exact form with a date in the sequence: another base
  in the same folder keeps its files. Do not start a second server on a folder
  where another one is writing: it would delete the file in progress.
- **One backup at a time.** `startAsync()` is the only way in, for the cron task
  as for a manual start. It works in a daemon thread and returns at once: the
  dump must not hold the single consumer thread of the scheduler, which would
  delay every other task for minutes (see [cron.md](cron.md)). It returns
  `STARTED`, `ALREADY_RUNNING` or `INSUFFICIENT_SPACE` (`BackupStart`). The lock
  is per process: two instances on the same folder do not see each other.
- **Free space.** Before each start, the volume must have twice the size of the
  last full archive plus `BackupService.SPACE_MARGIN_BYTES` (1 GiB) free,
  otherwise nothing starts. There is no limit on how often a backup can start,
  only this check. A refused start is logged and recorded as a failed attempt,
  whoever asked: the cron task drops the answer of `startAsync()`, so the state
  is all that is left of it.
- A folder or a free space that cannot be read throws `UncheckedIOException`,
  also recorded as a failed attempt. A missing folder is not created again here:
  it may be a volume no longer mounted, and the archive would land on what lies
  under it.
- **Failures** are caught, `Error` included, logged with their stack trace, and
  shown as `success = false`, never with their text.
- **No archive is deleted.** The service has no retention, and archives pile up
  until something calls `clean(...)`. It has no restore either.

`getState()` returns a `BackupState`, `listArchives()` only its list. Neither
holds a path, a file name or the text of an exception:

| `BackupState` | |
|---|---|
| `running` | a backup is being written |
| `lastAttempt` | `BackupAttempt(date, success)`, `null` until the first attempt finishes. Kept in memory: lost at a restart |
| `archives` | list of `BackupArchive`, most recent first |

| `BackupArchive` | |
|---|---|
| `date` | `Instant`, read from the sequence as UTC |
| `size` | in bytes |
| `type` | `FULL`, `DELTA` or `PARTIAL` (`BackupArchiveType`), read from the suffix of the sequence |
| `hasChecksum` | the `.sha256` file is there |

A file `.<name>_tmp` is an archive being written and is not listed. A name that
cannot be parsed, a directory or a symbolic link is left out with a warning in
the log. The date is not validated: the parser is lenient, and an impossible day
it accepts (`2026-02-30`) is listed as the day it resolves to. A date with no
time zone is read as UTC: the archives of the former
`BackupJob` were written in the default time zone of the JVM, which the
applications set to UTC. A folder that cannot be read or does not exist any more
throws `IOException`, not an empty list, which would hide a volume no longer
mounted. Its message names the folder: it must not reach a client.

On a read-only container, the folder must be a volume: the constructor throws
`IOException` when it cannot create it.


BackupResource: list and start from an administration page
----------------------------------------------------------

`BackupResource` (`org.atriasoft.archidata.api`) publishes the service:

```java
// at startup, next to the scheduler
BackupResource.setService(backup);
// with the other resources of the application
rc.register(BackupResource.class);
```

Add `BackupResource.class` to the resources given to `AnalyzeApi` to generate the
client. A setter, because Jersey builds the resource by itself and the
generators need the class.

| Route | Answer |
|---|---|
| `GET /backups` | 200 and the `BackupState`, described above |
| `POST /backups` | 202 with an empty body and the backup runs in the background, 409 if one is already running, 507 if the volume has too little free space. Read `GET /backups` to follow it |

**No route exposes the content of an archive.** There is no download, no restore,
no deletion, and no parameter that names a file: an archive holds the password
hashes and the whole catalog in clear, and neither its content, its name nor its
path leave the server. Keep it that way: a new route here is a security decision,
not a convenience.

- `GET` needs `@RolesAllowed("ADMIN:r")` (`READ` or `READ_WRITE`), `POST` needs
  `@RolesAllowed("ADMIN:w")` (`WRITE` or `READ_WRITE`), checked by the
  `AuthenticationFilter` of the application: without such a filter the annotation
  is ignored (see [security.md](security.md)), so each route also checks its role
  itself, and an application that forgets the filter gets 403 on both. No token
  answers 401, a token without the role 403, whether the service is configured or
  not. A token in the URI is refused (no `@PermitTokenInURI`).
- A service that was never given answers 503 `Backups are not configured`.
- 409 means wait, 507 means free some space: both carry the usual JSON error
  body, without any number or path.
- Each manual start logs `Manual backup by user <id>: <result>`, `STARTED` at
  `INFO`; `ALREADY_RUNNING`, `INSUFFICIENT_SPACE` and `FAILED` at `WARN`. The
  identifier comes from the `SecurityContext`, never a token. The nightly task
  does not pass here: such a line always names a person.
- A folder that cannot be read, or that disappeared, answers the generic 500 of
  `ExceptionCatcher`, on both routes, which keeps the message in the log. A failed
  backup shows only as `lastAttempt.success = false`.
- The failure of the last attempt is known only until the server restarts.


Reference
---------

`BackupEngine`

| Member | Role |
|---|---|
| `BackupEngine(Path pathToStoreDB, String baseName, EngineBackupType type)` | create an engine |
| `addClass(Class<?>...)` (throws `DataAccessException`), `addCollection(String...)` | register the collections for `store`, `storeDelta`, `snapshot`; registered twice, a collection is written once |
| `setEnableStoreOrRestoreData(boolean)` | include the media folder (default `true`) |
| `setChunkMaxBytes(int)`, `setChunkMaxDocuments(int)` | archive chunk bounds |
| `setRestoreBatchMaxBytes(int)`, `setRestoreBatchMaxDocuments(int)` | `insertMany` batch bounds at restore |
| `setRestoreRelaxedWriteConcern(boolean)` | see above |
| `setRequireChecksum(boolean)` | refuse an archive without `.sha256` sidecar (default `false`: warning) |
| `setRestoreMaxEntryBytes(long)`, `setRestoreMaxTotalBytes(long)`, `setRestoreMaxLineChars(int)` | restore bounds (64 GiB, 1 TiB, 64 Mi characters) |
| `checksumPath(Path)` (static), `SUFFIX_SHA256` | path of the sidecar of an archive |
| `store(String)`, `storeDelta(String, Date)` | back up the registered collections (full, delta) |
| `storeAll(String)`, `storeAllDelta(String, Date)` | same on every collection but `system.*`, no registration needed |
| `restoreFile(Path, String)`, `restoreDeltaFile(Path)`, `restore(String sequence)`, `restoreLatest()` | restore from archives |
| `getLastFullBackupDate()`, `getLatestRestoreChain()` | inspect the archive directory |
| `snapshot()`, `snapshotAll()`, `restore(BackupSnapshot)` | in-memory snapshots |
| `parseDateFromSequence(String)`, `parseDateTimeFromSequence(String)` | static sequence parsing |
| `clean(...)`, `cleanDryRun(...)` | retention |
| constants `DEFAULT_CHUNK_MAX_BYTES`, `DEFAULT_CHUNK_MAX_DOCUMENTS`, `DEFAULT_RESTORE_BATCH_MAX_BYTES`, `DEFAULT_RESTORE_BATCH_MAX_DOCUMENTS` | defaults (32 MiB, 100 000, 32 MiB, 10 000) |

All the `store*` methods return the `Date` at which the backup started: pass it
as `since` of the next delta. `BackupFileInfo` is package-private.


Pitfalls
--------

- The date in the sequence drives everything: an archive whose sequence does
  not start with one of the formats above is invisible to the retention and to
  `restoreLatest()`.
- `getLastFullBackupDate()` reads the sequence as **UTC**. Format your sequences
  in UTC, otherwise `since` is shifted by your offset and a delta can miss
  documents.
- Two backups with the same sequence overwrite each other (`REPLACE_EXISTING`).
- Registration is not automatic for `store` / `storeDelta`: forgetting a
  collection (or `counters` and the migration collection, as karideo's job
  does explicitly) silently leaves it out of the archive. `storeAll` avoids
  the problem.
- `baseName` is both the archive prefix and the database name used for the
  "already exists" check of `restoreFile`. Pass the real database name
  (`ConfigBaseVariable.getDBName()`).
- Restoring on a non-empty database does nothing and returns `false`; callers
  that ignore the return value believe the restore happened.
- `restoreDeltaFile` performs no consistency check: applying a delta on a
  database that is not the restored base full silently replaces newer documents
  with the archived ones.
- Media are included by default. On a server whose media folder is large or
  managed elsewhere, call `setEnableStoreOrRestoreData(false)`.
- `store()` runs in the calling thread and can take minutes: neither a request
  thread nor the cron task should wait for it, the scheduler has a single
  consumer thread (see [cron.md](cron.md)). `BackupService.startAsync()` gives it
  a thread of its own.
