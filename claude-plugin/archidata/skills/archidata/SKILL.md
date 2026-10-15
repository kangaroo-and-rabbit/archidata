---
name: archidata
description: Use when working on a Java project that depends on archidata (Maven org.atria-soft:archidata, code importing org.atriasoft.archidata) - creating or changing an entity/model, a JAX-RS REST resource (CRUD, filters, pagination), relations, a MongoDB migration or index, security (JWT, @RolesAllowed, @RightAllowed, PartRight), media upload, TypeScript/Python/OpenAPI client generation, error handling, change streams, backup, cron, or debugging a Grizzly/Jersey launcher built on archidata. Gives the entry class and doc page for each task and the verified pitfalls.
---

# archidata

archidata is a Java 25 framework for REST servers on Jakarta JAX-RS (Jersey + Grizzly) and
MongoDB. Docs: <https://github.com/kangaroo-and-rabbit/archidata/tree/develop/doc> (pages named
below as `doc/<page>.md`). Reference project: `sample/archidata/basic` in the same repository.

## First reflex: read the documentation of the exact version

The API changes between versions and your training data is likely stale. Every archidata jar
embeds its own documentation:

1. Read the archidata version in the project's `pom.xml` (`<artifactId>archidata</artifactId>`,
   or a `${...}` property).
2. Read `META-INF/archidata/llms-full.txt` from that jar in the local Maven repository:

```bash
V=0.50.2   # the version from the pom
unzip -p ~/.m2/repository/org/atria-soft/archidata/$V/archidata-$V.jar META-INF/archidata/llms-full.txt > /tmp/archidata-$V-llms-full.txt
grep -n "^FILE: " /tmp/archidata-$V-llms-full.txt   # table of contents (one page per FILE: separator)
```

If the jar is missing, run `mvn dependency:resolve` first. Jars older than 0.50.3 do not
contain it: use the `-sources.jar` (javadoc) or the docs on GitHub at the matching tag.
`META-INF/archidata/AGENTS.md` in the same jar lists the pitfalls below.

## Recipes

Each recipe gives the entry point and the page to read. Imports are under `org.atriasoft.archidata`.

### Application launcher (Grizzly + Jersey)
Copy `sample/archidata/basic/src/main/java/sample/archidata/basic/WebLauncher.java`. Required
registrations on the `ResourceConfig rc`:
```java
rc.register(OptionFilter.class);           // CORS preflight
rc.register(CORSFilter.class);
rc.register(MyAuthenticationFilter.class); // subclass of filter.AuthenticationFilter
GenericCatcher.addAll(rc);                 // catcher.*: JSON errors with the right status
ContextGenericTools.addJsr310(rc);         // Jackson mapper + ValidGroupInterceptor
rc.register(JacksonFeature.class);
rc.register(ValidationFeature.class);      // Jersey Bean Validation for @Valid
rc.register(MyResource.class);
```
Before starting: `MigrationEngine` then `IndexEngine.synchronize()` (see below). Docs:
`doc/TUTORIAL_01_first_install.md`, `doc/converters_and_beans.md`.

### Entity
Extend a base class of `model`: `OIDGenericDataSoftDelete` (ObjectId id, timestamps, soft delete,
recommended), `OIDGenericData`, `GenericDataSoftDelete` (Long id). Annotate with
`@jakarta.persistence.Entity` (collection = class name, or `@Table(name = "...")`), Bean
Validation constraints, `@Schema`/`@ApiDoc` for the generated clients. Server-managed fields get
`@ApiReadOnly`; create/update-only rules use the groups `GroupCreate` / `GroupUpdate`
(`annotation.checker`). Docs: `doc/data_model.md`, `doc/annotations.md`.

### CRUD resource
Static facade `dataAccess.DataAccess`: `insert(obj)`, `gets(Class, options...)`,
`getById(Class, id)`, `updateById(obj, id, new FilterValue(List.of("title")))`,
`deleteById(Class, id)` (soft delete when the entity has `@DataDeleted`), `count(...)`.
Every method needs `@PermitAll`, `@RolesAllowed` or `@RightAllowed`. Body parameters:
`@Valid @ValidGroup(GroupCreate.class) final Task task`. Model:
`sample/.../api/TaskResource.java`. Docs: `doc/database_access.md`.

### Filters, sort, pagination
Options from `dataAccess.options`: `new Condition(Filters.eq("done", false))`
(`dataAccess.Filters`, Mongo-style builders), `new OrderBy(new OrderItem("createdAt", Order.DESC))`,
`new Limit(n)`, `new Skip(n)`, `FilterValue`/`FilterOmit` (field projection),
`ReadAllColumn` (also read `@DataNotRead` fields), `AccessDeletedItems`. Paginated endpoint:
parameter `@PaginationContext final PaginationContext page`, return `Pagination<T>`
(`dataAccess.model`), register `pagination.PaginationFeature`. Docs: `doc/database_access.md`,
`doc/pagination.md`.

### Relations
`@ManyToOneDoc(targetEntity = Parent.class, remoteField = "children")` on the child's
`ObjectId` field, `@OneToManyDoc(targetEntity = Child.class, remoteField = "parentId")` on the
parent's `List<ObjectId>`, `@ManyToManyDoc` for both sides; `@CheckForeignKey(X.class)` checks
the target exists. The reverse lists are maintained by the data layer. Docs: `doc/data_model.md`.

### Migration
Extend `migration.MigrationStep`, override `getName()` (stable string, never rename it) and
`generateStep()` (add steps). Launcher: `engine.setInit(new Initialization())`,
`engine.add(new Migration20261101())`, `engine.migrateWaitAdmin(new DbConfig())`. History in
the `KAR_migration` collection. Example: `sample/.../migration/Initialization.java`. Docs:
`doc/migration.md`.

### Indexes
`@Index({ "done", "-createdAt" })` on the class (compound, `-` = descending) or
`@Indexed(unique = true)` on a field; at startup `new IndexEngine()` + `addClass(...)` +
`synchronize()` (`plan()` to review first). Docs: `doc/indexes.md`.

### Security (roles and rights)
`filter.AuthenticationFilter` subclass with the application name (and issuer if not `"Karso"`).
`@RolesAllowed("ADMIN")` checks `roles.<app>.ADMIN`; `@RightAllowed(right = "articles", access = PartRight.WRITE)`
checks a fine-grained `PartRight` (`NONE`, `READ`, `WRITE`, `READ_WRITE`). Tokens:
`tools.JWTWrapper` (`initLocalToken`, `initLocalTokenRemote` for an SSO, `generateJWToken`).
Current user: `(GenericContext) securityContext.getUserPrincipal()` (`@Context SecurityContext`). Docs: `doc/security.md`.

### Media upload
Register `api.DataResource` (and Jersey `MultiPartFeature`): `POST /data/upload` (multipart
field `file`) returns the `ObjectId` of a `model.Data`; files are deduplicated by SHA-512 under
`ConfigBaseVariable.getMediaDataFolder()`. In your own resource: `DataTools.uploadData(stream, disposition)`,
`DataTools.uploadCover(...)`, `uploadDataFromUri` (SSRF-guarded). Docs: `doc/tools.md` (Data files).

### Client generation (TypeScript, Python, OpenAPI)
```java
final AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(TaskResource.class));
TsGenerateApi.generateApi(api, Paths.get("front/src/back-api"), true);     // TypeScript + zod
PythonGenerateApi.generateApi(api, Paths.get("client/src/my_api"), true);  // Pydantic package
```
OpenAPI: register `api.openApiResource` and call `openApiResource.configure(api, "title", "1.0.0")`
(serves `GET /openapi/swagger.json`). `@ApiGenerationMode(create = true, update = true)`
on a model generates `XxxCreate` / `XxxUpdate` types. Docs: `doc/typescript_api_generation.md`,
`doc/python_api_generation.md`, `doc/openapi_generation.md`.

### Errors
Throw `exception.FailException(Response.Status.NOT_FOUND, "message")` (one-argument form = 400),
`InputException` (bad field, 400), `SystemException`, or JAX-RS exceptions (`NotFoundException`).
`GenericCatcher.addAll(rc)` turns them into `catcher.RestErrorResponse` JSON. Docs:
`doc/error_handling.md`.

### Change streams
`ChangeNotificationManager.getInstance()`, `start(database, FullDocument.UPDATE_LOOKUP)`,
`createListenerBuilder(event -> ..., "Collection").register()` or `watch("Collection")...`,
`stop()` on shutdown. Docs: `doc/change_stream.md`.

### Backup
`backup.BackupEngine(dir, dbName, EngineBackupType.JSON_EXTENDED)`, `addClass(...)`,
`store(sequence)` / `storeDelta(sequence, since)`, `restoreLatest()`, `clean(new RetentionPolicy())`.
Docs: `doc/backup.md`. Incremental capture of modified documents: `journal.ChangeJournalEngine`
(`doc/change_journal.md`).

### Cron
`cron.CronScheduler`: `addTask("name", "0 3 * * *", runnable)`, `addTask(name, LocalDateTime, runnable)`
(one shot), `start()`, `stop()`. Docs: `doc/cron.md` (the syntax differs from Unix cron).

### Configuration
`tools.ConfigBaseVariable` reads `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_DATABASE`,
`API_ADDRESS`, `DATA_FOLDER`… from the environment; setters for tests. Docs: `doc/tools.md`.

## Pitfalls (verified in the code)

- **Launcher**: without `ContextGenericTools.addJsr310(rc)`, `@ValidGroup` is silently ignored
  and dates are serialized differently; without `GenericCatcher.addAll(rc)`, errors are not
  the archidata JSON with the right status.
- **`@ValidGroup` does not check the `Default` group**: also put `@Valid` on the parameter and
  register `ValidationFeature`, or `@NotNull`/`@Size` without groups are never checked.
- **403 everywhere**: `AuthenticationFilter` rejects any endpoint without `@PermitAll`,
  `@RolesAllowed` or `@RightAllowed`. Conversely, without an authentication filter registered,
  `@RolesAllowed` is ignored (archidata does not register `RolesAllowedDynamicFeature`).
- **JWT**: `sub` must be an `ObjectId` hex string; the default issuer is `"Karso"`; the `roles`
  / `right` claims are `{app: {name: code}}` with `PartRight` codes (int 0-3 or the name), never
  `Boolean`.
- **`createdAt` / `updatedAt` are `@DataNotRead`**: absent from default reads, use `ReadAllColumn`.
- **`@Column(unique = true)` creates no index**: use `@Index`/`@Indexed` and run
  `IndexEngine.synchronize()` at startup.
- **`updateById` without `FilterValue` rewrites every editable field, `null` included** (null =
  `$unset`): pass the list of fields for a partial update (PATCH).
- **Migrations**: collection `KAR_migration`. On an empty database only the init runs (the
  added migrations are marked done without running: the init must build the final state).
  Renaming a migration (`getName()`) makes the engine apply nothing, without error.
- **No SLF4J backend comes with archidata**: declare `ch.qos.logback:logback-classic` in the
  application, or there are no logs at all.
- **Change streams**: need a MongoDB replica set (single node is fine); only the `FullDocument`
  mode given to `start()` is used; listeners run on the collection worker thread, so a slow
  listener blocks that collection; `stop()` clears listeners and resume tokens.
- **Cron**: one consumer thread for all tasks, no catch-up of missed runs, one scheduler per
  process (N instances = N runs). Not Unix syntax: read `doc/cron.md`.
- **`ConfigBaseVariable` reads the environment when the class is loaded**; set values with the
  setters before first use. `getDBPassword()` throws if `DB_PASSWORD` is missing.
- **HTTP status of an error**: use `new FailException(Response.Status.XXX, msg)`; the
  one-argument constructor always gives 400.
- **Compile with `-parameters`** (`<maven.compiler.parameters>true</maven.compiler.parameters>`),
  or validation errors name the arguments `arg0`, `arg1`.

## Build tips for archidata applications

- Java 25 is required; set `JAVA_HOME` explicitly if the default `java` is older.
- Applications usually bind `exec-maven-plugin` to `package` (it starts the server): pass
  `-Dexec.skip=true` to build without running.
- Back-end tests start a real server and use the local MongoDB: do not run two test suites at once.
