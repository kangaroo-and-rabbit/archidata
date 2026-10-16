# AGENTS.md — working on archidata

Guide for AI agents (and humans) who modify the archidata library itself. If you only *use*
archidata in an application, read the [Pitfalls](#pitfalls-when-using-archidata) section, then
`llms.txt` (index) or `META-INF/archidata/llms-full.txt` inside the jar (the full documentation
of that exact version).

archidata (`org.atria-soft:archidata`, MPL 2.0) is a Java framework for REST servers on
Jakarta JAX-RS (Jersey + Grizzly) backed by MongoDB: entity introspection and CRUD, JWT
security, validation, migrations, indexes, backups, change streams, cron, and generation of
OpenAPI / TypeScript / Python clients. Public mirror: <https://github.com/kangaroo-and-rabbit/archidata>
(branch `develop`). Documentation: `doc/` (start at `doc/index.md`). Reference project that
compiles against the current version: `sample/archidata/basic`.

## Package map

All packages live under `org.atriasoft.archidata` in `src/main/`.

| Package | Content |
| --- | --- |
| `annotation` | Model annotations: `@Collection`, `@Field`, `@Id`, `@InitValue`, `@DataNotRead`, `@DataDeleted`, `@CreationTimestamp`, `@UpdateTimestamp`, `@Index`/`@Indexed`, `@ManyToOneDoc`/`@OneToManyDoc`/`@ManyToManyDoc`, `@DataEncrypt`; `AnnotationTools` (reflection helpers) |
| `annotation.apiGenerator` | `@ApiDoc`, `@ApiReadOnly`, `@ApiGenerationMode`, `@ApiOptionalIsNullable`… (client generation) |
| `annotation.checker` | Validation groups (`GroupCreate`, `GroupUpdate`, `GroupRead`, `GroupPersistant`), `@CheckForeignKey`, collection validators |
| `annotation.method` / `annotation.security` / `annotation.filter` | `@PaginationContext`, custom HTTP verbs (`@ARCHIVE`, `@RESTORE`, `@CALL`), `@RightAllowed`, `@PermitTokenInURI`, `@DataAccessSingleConnection` |
| `api` | Ready-made JAX-RS resources: `DataResource` (media upload/streaming), `openApiResource`, `FrontGeneric`, `ProxyResource`, `MediaStreamer` |
| `backup` | `BackupEngine` (tar.gz full/delta archives, restore), `RetentionPolicy`, `BackupSnapshot` |
| `bean` | Bean introspection used by the data layer (internal) |
| `catcher` | JAX-RS exception mappers (`GenericCatcher.addAll(rc)`), `RestErrorResponse` |
| `checker` | `ValidationContext`, `DataAccessConnectionContext` (per-request validation and connection context) |
| `converter` | Jackson / Jakarta param / Mongo codecs for custom types |
| `cron` | `CronScheduler`, `CronTask` (Unix/Vixie cron syntax, numeric fields only) |
| `crypto` | Field encryption back-end of `@DataEncrypt` |
| `dataAccess` | `DataAccess` (static facade), `DBAccessMongo`, `Filters`, `QueryOptions`, `TransactionContext` |
| `dataAccess.options` | Query options: `Condition`, `FilterValue`, `FilterOmit`, `OrderBy`, `Limit`, `Skip`, `AccessDeletedItems`, `ReadAllColumn`, `DirectData`… |
| `dataAccess.model` / `dataAccess.model.codec` / `dataAccess.mongo` / `dataAccess.addOn` | Entity model descriptors, Mongo codecs, relation add-ons, `Pagination<T>` / `PaginationContext` |
| `dataAccess.statistic` | `QueryStatistics` (per-query counters) |
| `dataStreamEvent` | `ChangeNotificationManager` (MongoDB change streams → Java listeners) |
| `db` | `DbConfig`, `DbIo`, `DbIoFactory` (connection pool) |
| `exception` | `FailException`, `InputException`, `DataAccessException`, `SystemException`, `UnAuthorizedException` |
| `externalRestApi` | `AnalyzeApi`, `OpenApiGenerateApi`, `TsGenerateApi`, `PythonGenerateApi`, `DotGenerateApi`, `DrawioGenerateApi` |
| `filter` | `AuthenticationFilter`, `CORSFilter`, `SecurityHeadersFilter`, `OptionFilter`, `PartRight`, `MySecurityContext`, `DataAccessRetentionConnectionFilter` |
| `index` | `IndexEngine` (declare + synchronize MongoDB indexes, `KAR_index` cache) |
| `interceptor` | `ValidGroupInterceptor` (`@ValidGroup`) |
| `journal` | `ChangeJournalEngine` (incremental capture of modified documents) |
| `migration` | `MigrationEngine`, `MigrationStep`, `MigrationInterface` (`KAR_migration` collection) |
| `model` | Base entities (`OIDGenericDataSoftDelete`, `GenericTiming`, `Data`, `User`…), `model.token` (JWT payload records) |
| `pagination` | `PaginationFeature` (offset pagination via HTTP headers + RFC 5988 `Link`) |
| `tools` | `ConfigBaseVariable`, `ContextGenericTools`, `JWTWrapper`, `RESTApi`, `DataTools`, `UuidUtils`, `SsrfGuard`… |

Tests live in `src/test/test/atriasoft/archidata/` (same package layout).

## Build and test

- **Java 25** is the target (`maven.compiler.source/target` = 25). Any newer JDK can build it;
  the default `java` of the machine may be too old, so set `JAVA_HOME` explicitly.
- **Non-standard source paths**: sources in `src/main`, tests in `src/test`, resources in
  `src/resources` and `src/testResources` (not `src/main/java`).
- The `formatter-maven-plugin` runs `validate` (fails on unformatted code). Format with
  `mvn formatter:format`, or skip with `-Dformatter.skip=true`. CheckStyle: `-Dcheckstyle.skip=true`.
- Release signing: skip with `-Dgpg.skip=true` for local builds.
- Applications built on archidata (karso, karideo, karusic…) bind `exec-maven-plugin` to
  `package`, which starts the server: always pass `-Dexec.skip=true` there. archidata itself has
  no exec plugin, so the flag is harmless here and keeps one command for every project.
- Only one `target/` directory is shared: never run two Maven builds of this repository at the
  same time (serialize them, e.g. with `flock`).

```bash
# compile
mvn compile -Dformatter.skip=true -Dcheckstyle.skip=true
# jar without tests (also generates META-INF/archidata/llms-full.txt)
mvn package -DskipTests -Dexec.skip=true -Dformatter.skip=true -Dcheckstyle.skip=true -Dgpg.skip=true
# one test class
mvn test -Dtest=TestTypeScriptApiGeneration -Dformatter.skip=true -Dcheckstyle.skip=true
# install in ~/.m2 for the sample and the applications
mvn install -DskipTests -Dgpg.skip=true
```

Tests need a **local MongoDB replica set** on `localhost:27017` (user `root`, password
`base_db_password`, database `test_db`, dropped at each run): start it with
`env_dev/docker-compose.yml`. The change stream tests require the replica set. The REST tests
start a Grizzly server on `127.0.0.1:12345`. Surefire runs the classes sequentially in one fork:
do not run two test sessions against the same MongoDB at once. Downstream applications
(karso, karideo, karusic, karentory) start their test server on `127.0.0.1:12342`: never run
their back-end tests in parallel either.

## Code conventions

Enforced by `CheckStyle.xml` and `Formatter.xml` at the root:

- No `var`: always write the explicit type.
- `final` on every parameter and local variable.
- `this.` prefix on every instance field access.
- TAB indentation, LF line endings, UTF-8.
- No wildcard imports (`import java.util.*` is rejected).
- Modifier order `public static final` (never `static public` / `final static`).
- Logging through SLF4J: `private static final Logger LOGGER = LoggerFactory.getLogger(X.class);`.
  Never `printStackTrace()` nor `System.out`.
- Utility classes (only static members) have a private constructor (CheckStyle
  `HideUtilityClassConstructor`).
- Javadoc on public classes and methods (`doclint=all` in `maven-javadoc-plugin`).

## Commit format

`[TYPE] (scope) message`, scope optional, message in English, imperative or descriptive:

```
[FEAT] A stream says the size its picture is meant to be shown at
[FIX] (externalRestApi) describe Pagination<T> in the OpenAPI specification
[DOC] (dataStreamEvent) Merge the three change stream pages into one, checked against the code
[DEV-OPS] (dependabot) Bump org.slf4j:jul-to-slf4j
[RELEASE] Release v0.50.2
[VERSION] update dev tag version
```

Types seen in the history: `FEAT`, `FIX`, `DOC`, `DEV-OPS`, `RELEASE`, `VERSION`. One subject
per commit; no AI attribution line.

## Documentation rules

Every new feature, or change of behaviour, updates the documentation in the same commit:

1. The relevant page in `doc/` (or a new page, linked from `doc/index.md`). Code examples must
   compile; prefer pointing to `sample/archidata/basic` over copying code.
2. `doc/annotations.md` when an annotation is added or changed.
3. `llms.txt` at the root: one link per doc page (absolute GitHub URL on `develop`).
4. A new page in `doc/` must also be added to the ordered list of the
   `embed-llm-documentation` execution in `pom.xml` (one `<llm-page file="doc/xxx.md"/>` line, the
   only place where the order is defined): a page of `doc/` that is not listed FAILS the build.
5. The javadoc of the public classes touched.

The jar ships `META-INF/archidata/llms.txt`, `META-INF/archidata/AGENTS.md` and
`META-INF/archidata/llms-full.txt` (README + `doc/index.md` + every page), generated at
`generate-resources` by `maven-antrun-plugin`.

## Pitfalls when using archidata

Each point was checked against the code; the doc page gives the details.

- **Launcher**: call `GenericCatcher.addAll(rc)` (JSON errors with the right HTTP status) and
  `ContextGenericTools.addJsr310(rc)`. The second one registers the Jackson mapper (ISO dates
  instead of timestamps, archidata modules, and the drop of `@ApiReadOnly` properties of request
  bodies) and the `ValidGroupInterceptor`: without it, `@ValidGroup` is silently ignored and the
  date format changes. Also register `rc.register(SecurityHeadersFilter.class)` (`nosniff`,
  `Referrer-Policy`, `X-Frame-Options`, HSTS over https only; a header already set by a resource is
  kept). See `sample/.../WebLauncher.java`.
- **`@ValidGroup` does not check the `Default` group** (the archidata groups do not extend it):
  also put `@Valid` on the parameter and register Jersey's `ValidationFeature`
  (`@Valid @ValidGroup(GroupCreate.class) final Task task`). See `doc/converters_and_beans.md`.
- **`AuthenticationFilter` answers 403 to any endpoint without `@PermitAll`, `@RolesAllowed` or
  `@RightAllowed`** (and to `@DenyAll`). Conversely, with no authentication filter registered,
  `@RolesAllowed` is ignored: archidata does not register Jersey's `RolesAllowedDynamicFeature`.
  See `doc/security.md`.
- **JWT**: the subject (`sub`) must be an `ObjectId` hex string; the default issuer is `"Karso"`
  (`new AuthenticationFilter(app)`, otherwise pass it); the `roles` / `right` claims are maps
  `{application: {name: code}}` whose values are `PartRight` codes (int `0..3` or the name
  `NONE`/`READ`/`WRITE`/`READ_WRITE`), never `Boolean`. Roles are checked as
  `applicationName/ROLE`.
- **`createdAt` and `updatedAt` carry `@DataNotRead`**: they are not returned by default reads.
  Use the `ReadAllColumn` option to get them. See `doc/data_model.md`.
- **Mapping annotations are archidata's own** (`org.atriasoft.archidata.annotation`):
  `@Collection("name")` (collection, default: class simple name), `@Field("name")` (field name in
  the document, nothing else), `@Id` (primary key), `@InitValue("...")` (value written on insert
  when `null`). The JPA annotations (`jakarta.persistence`: `@Table`, `@Column`, `@GeneratedValue`,
  `@ManyToOne`...) and a JAX-RS `@DefaultValue` on an entity field are not read: analysing the model
  of an entity that still carries one throws a `DataAccessException` naming the class, the element
  and the replacement. See `doc/annotations.md` and `MIGRATION.md`.
- **Unicity only exists through a unique index**: declare `@Index` / `@Indexed(unique = true)` and
  run `IndexEngine.addClass(...)` + `synchronize()` at startup. See `doc/indexes.md`.
- **`updateById` without `FilterValue` writes every editable field, `null` included** (a null
  field is `$unset`, unless it has an `@InitValue`), **except** the `@DataNotRead` and
  `@DataEncrypt` fields: those are written only when a `FilterValue` names them. The same holds
  inside embedded objects: an embedded object holding such fields is written sub-field by sub-field
  in dot notation (`credentials.user`; the object set to `null` is `$unset`), and a `List`/`Set`/`Map`
  of such objects is **not written** by a full update (name it in a `FilterValue`). Pass
  `new FilterValue(List.of("field1", "field2"))` for a partial update. `update(...)` with a
  `Condition` refuses to run without exactly one `FilterValue`. See `doc/database_access.md`.
- **`@ApiReadOnly`**: a property sent by a client in a request body is ignored (dropped by the
  server JSON provider registered by `addJsr310`; records and `@JsonCreator` constructors are not
  filtered). Server code can still write it with `insert`; `update` needs `ForceReadOnlyField`.
- **`MigrationEngine`**: the history is the `KAR_migration` collection. On an empty database
  only the initialization (`setInit`) runs, and the last migration is recorded as done (the
  migrations `add`ed are never played on a fresh install, so the init must create the final
  state). If the last recorded name is not found (migration renamed), the engine throws a
  `MigrationException` and the startup is blocked. Reverting is not supported: `revertSingle` /
  `revertTo` throw `UnsupportedOperationException` (write a new forward migration). See
  `doc/migration.md`.
- **SLF4J backend**: archidata's logback is test-scoped on purpose, so no backend comes
  transitively: the application must declare `ch.qos.logback:logback-classic` (or another
  backend), otherwise no log at all.
- **Change streams** (`ChangeNotificationManager`): they require MongoDB running as a replica
  set (a single-node one is fine); only the `FullDocument` mode passed to `start()` counts (the
  per-listener / per-watch mode is always ignored: those overloads are `@Deprecated` and log a
  warning); the retry delay after a failure is fixed (1 s / 5 s, no exponential backoff);
  listeners run synchronously on the collection's worker thread, so a slow listener blocks that
  collection (hand heavy work to an executor); `stop()` clears the listeners and the resume
  tokens (register them again after a restart). See `doc/change_stream.md`.
- **Cron** (`CronScheduler`): the syntax is Unix (Vixie) cron: `*/n` starts at the minimum of
  the field, Sunday is 0 or 7, day-of-month and day-of-week combine with OR. Names (`MON`,
  `JAN`), macros (`@daily`), `L`, `W` and `#` are not supported. A single consumer thread runs
  every task one after the other (a blocking task delays all the others); a run missed while
  the server was down is not caught up; the scheduler is per process (two instances run every
  task twice). See `doc/cron.md`.
- **Date types**: `OffsetDateTime` fields are persisted as a BSON date and read back in UTC
  (the offset is lost). `LocalDateTime` is not supported by the data layer.
- **`@ApiDoc(hidden = true)`** is honoured by every client generator.
- **REST client** (`tools.RESTApiRequest`): `fetch(Class)` / `fetchList` throw
  `RESTErrorResponseException` on an error status, whereas `fetchString()` / `fetchByte()` are
  raw accessors that do not throw on 4xx/5xx (check the status yourself). `header(key, value)`
  adds a request header.
- **Deprecated**: `tools.PublicKey` (`forRemoval`): use `JWTWrapper.PublicKey`.
- **`ConfigBaseVariable` reads the environment once, when the class is loaded** (static
  initializer): `System.setenv`-style tricks afterwards have no effect, use the setters before
  the first use (they are locked by `lock()`). `getDBPassword()` throws `IllegalStateException`
  when `DB_PASSWORD` is not set. `DB_ABLE_TO_CREATE` is read with `Boolean.parseBoolean`;
  `getDBPort()` returns an `int` validated in 1..65535 (`DbConfig` takes an `Integer` port). See `doc/tools.md`.
- **Errors with a precise HTTP status**: throw `new FailException(Response.Status.XXX, "message")`
  (the single-argument constructor answers 400), or a JAX-RS `WebApplicationException`
  (`NotFoundException`…). `GenericCatcher.addAll` registers `RequestBodyReaderInterceptor`
  and `RequestBodyExceptionCatcher`: malformed or mistyped JSON in a request body answers 400. Any
  other raw `JacksonException` (server-side parsing, serialization) answers 500
  (`JacksonExceptionCatcher`). `InputExceptionCatcher` reports the real status. See `doc/error_handling.md`.
- **Compile the application with `-parameters`** (`<maven.compiler.parameters>true</maven.compiler.parameters>`),
  otherwise the validation errors name the method arguments `arg0`, `arg1`….
- **Relation keys**: the key of a `@ManyToOneDoc`, `@OneToManyDoc` or `@ManyToManyDoc` field is an
  `ObjectId`, a `Long` or a `UUID` (a `List` of them on the collection side), or the target
  entity itself to expand it. Any other type (`String`...) is rejected with an error naming the
  class and the field when the model is analysed. See `doc/data_model.md`.
- **UUID primary keys** (`UUIDGenericData`, `UUIDGenericDataSoftDelete`) work for reads, filters
  and relations; the key is generated at insertion by `UuidUtils.nextUUID()`.

## Security behaviour of archidata (checked in the code; details in `doc/security.md`, changes in `MIGRATION.md`)

- **CORS**: `CORSFilter` reflects only the origins listed in `CORS_ALLOWED_ORIGINS` (comma
  separated), with `Allow-Credentials: true`. Without the list it sends `*` and no credentials.
  CORS only concerns browsers, not server-to-server calls.
- **Swagger and OpenAPI are off by default**: `openApiResource` and `SwaggerUiExternal` answer
  404 unless `API_DOC_ENABLED=true`. The Swagger UI files carry SRI hashes.
- **JWT**: the audience is checked. The `aud` claim must list the application name given to
  `AuthenticationFilter`; a token without `aud` is accepted only if its `application` claim equals
  it. `exp` is mandatory and `nbf` is checked. `SSO_ADDRESS` (public key) must be `https://`
  (`http://` only for `localhost` or a literal loopback IP, or for any host with
  `SSO_ALLOW_INSECURE=true`, e.g. `http://karso_back_service:80/karso/api/` on an internal Docker
  network; a single WARN at startup). Without it, such an address makes `new UpdateJwtPublicKey()`
  throw at startup (message naming the variable). The key refresh is rate-limited.
- **Limits**: `Limit` goes from 1 to 10 000 (`Limit.setMaxValue`, at most `Integer.MAX_VALUE`),
  `Skip` from 0 to `Integer.MAX_VALUE`. The expansion of relations stops at depth 5
  (`RelationExpansion.setMaxDepth`) and on cycles.
- **Writes without a condition**: `update`, `delete`, `deleteSoft`, `deleteHard`, `restore` and
  `updateBsonDocuments` with a missing or empty `Condition` throw, unless the call carries
  `new AllowMassOperation()`.
- **Client-supplied names and values in queries**: validate a field name with
  `Filters.checkFieldName(...)` or build the sort with `OrderItem.ofClientField(...)`; use
  `Filters.regexLiteral(...)` for a client value matched as text (`Filters.regex` keeps the
  pattern as is). `Filters.eq` writes an explicit `$eq`.
- **Upload and download limits** live in `DataTools`: `setMaxUploadSize` (1 GiB by default) and
  `setMaxRemoteDataSize` (50 MiB); beyond them the answer is 413 and the temporary file is
  removed. Remote URLs are fetched by `SafeHttpFetcher` (pinned IP, redirects revalidated).
  `RESTApiRequest` has a connection timeout of 10 s and a request timeout of 120 s by default.
- **Error answers**: a 500 and a 401 carry a generic message and an `oid`; the real cause is in
  the log under that `oid`. Throw a `FailException` for a message meant for the client.
- **Backups**: archives are created `rw-------` with a `<archive>.sha256` file checked at restore;
  they stay in clear (all fields, `@DataNotRead` included): protect the storage folder.
- **Database connection**: `DB_TLS` (true to use TLS) and `DB_AUTH_SOURCE` (authentication
  database) exist; a WARN is written once when the DB user is `root`.
- **Change streams and the journal**: a change event carries the raw document (`@DataNotRead`
  fields included): never relay it to a client. The change journal leaves `@DataNotRead` fields out.
