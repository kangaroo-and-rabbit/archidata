Tools
=====

The package `org.atriasoft.archidata.tools` holds the small shared pieces of a
server built on archidata: the central configuration (`ConfigBaseVariable`), a
REST client (`RESTApi`), the JWT helper (documented in [security.md](security.md)),
the identifier and date helpers, and a few utilities. Most of them are static
utility classes with a private constructor.

This page covers the configuration with the complete list of variables, then
each utility by theme.


Configuration: `ConfigBaseVariable`
-----------------------------------

All the technical settings of the framework live in one static holder. Each
value starts from an environment variable (read once, when the class is loaded),
can be overridden by a setter before the server starts, and is read through a
getter that applies the default.

```java
ConfigBaseVariable.setDbHost("mongo.internal");   // overrides DB_HOST
ConfigBaseVariable.setBdDatabase("my_app");
ConfigBaseVariable.setDbPassword(secret);
// ... start the server, then freeze the configuration:
ConfigBaseVariable.lock();
```

The setters take `String` for everything but the encryption key
(`byte[]`) and the entity list (`Class<?>[]`): booleans and numbers are passed
as text.

### Variables

| Environment variable | Setter | Getter | Default | Role |
|---|---|---|---|---|
| `DATA_TMP_FOLDER` | `setTmpDataFolder` | `getTmpDataFolder()` | `./data/tmp` | folder of the temporary uploads |
| `DATA_FOLDER` | `setDataFolder` | `getMediaDataFolder()` | `./data/media` | folder of the stored media files (also the folder saved by the [backup](backup.md)) |
| `DB_ABLE_TO_CREATE` | `setDbAbleToCreate` | `getDBAbleToCreate()` | `true` | whether the database may be created automatically. See the pitfall below: no code of the library reads it |
| `DB_HOST` | `setDbHost` | `getDBHost()` | `localhost` | MongoDB host |
| `DB_PORT` | `setDbPort` | `getDBPort()` (`int`) | `27017` | MongoDB port |
| `DB_USER` | `setDbUser` | `getDBLogin()` | `root` | MongoDB user |
| `DB_PASSWORD` | `setDbPassword` | `getDBPassword()` | none: **throws `IllegalStateException`** when unset | MongoDB password |
| `DB_DATABASE` | `setBdDatabase` | `getDBName()` | none (`null`) | database name |
| `DB_KEEP_CONNECTED` | `setDbKeepConnected` | `getDBKeepConnected()` | `false` | keep the connection open between operations (see [Connection management](connectionManagement.md)) |
| `API_ADDRESS` | `setApiAddress` | `getLocalAddress()` | `http://localhost:80/api/` | base URL the server listens on; also the base URL of the clients built in tests |
| `SSO_ADDRESS` | `setSsoAddress` | `getSSOAddress()` | none (`null`) | URL of the SSO that publishes the JWT public key (`UpdateJwtPublicKey`, `JWTWrapper.initLocalTokenRemote`) |
| `SSO_TOKEN` | `setSsoToken` | `ssoToken()` | none (`null`) | token sent to the SSO when fetching its public key |
| `TEST_MODE` | `setTestMode` | `getTestMode()` | `false` | test flag, exposed by the health check of the applications |
| `EMAIL_FROM`, `EMAIL_LOGIN`, `EMAIL_PASSWORD` | `setEMailFrom`, `setEMailLogin`, `setEMailPassword` | `getEMailConfig()` | none | outgoing mail account. The getter returns a record `EMailConfig(from, login, password)`, or `null` unless **all three** are set |
| `THUMBNAIL_FORMAT` | `setThumbnailFormat` | `getThumbnailFormat()` | `png` | format of the generated thumbnails |
| `THUMBNAIL_WIDTH` | `setThumbnailWidth` | `getThumbnailWidth()` (`int`) | `256` | width in pixels of the thumbnails |
| `DATA_DECRYPT_ENABLE` | `setDataDecryptEnable` | `getDataDecryptEnabled()` | `false` | allow the decryption of `@DataEncrypt` fields on read; when off the field comes back unset (see [Field encryption](field_encryption.md)) |
| `DATA_ENCRYPT_KEY` or `DATA_ENCRYPT_KEY_FILE` | `setEncryptDefaultKey(byte[])` | `getEncryptDefaultKey()` | none | key material of the logical key `default`. The `_FILE` variant holds the path of a file (Docker secret) and wins over the inline value |
| `DATA_ENCRYPT_KEYS_DIR` | `setEncryptKeysDir` | `getEncryptKeysDir()` | none | folder scanned for additional named keys |
| `QUERY_STATISTICS_FILE` | `setQueryStatisticsFile` | `getQueryStatisticsFile()` | none (mode off) | enables the [query statistics](statistics.md); path of the report |
| `QUERY_STATISTICS_PERIOD` | `setQueryStatisticsPeriod` | `getQueryStatisticsPeriod()` | `*/10 * * * *` | cron expression of the report flush |
| (none) | `setBbInterfacesClasses(Class<?>[])` | `getBbInterfacesClasses()` | empty array | the entity classes the data layer manages (used by `DbConfig`) |

Empty strings are treated as "unset" only for the thumbnail and query-statistics
values and the encryption key; for the others an empty variable is a value.

### Other environment variables

Two variables are read outside `ConfigBaseVariable`:

| Variable | Read by | Role |
|---|---|---|
| `UUID_SERVER_ID` | `UuidUtils` (once, at class loading) | numeric id of this server, folded into the UUIDs so that several instances do not collide. Default: 1 |
| `ARCHIDATA_GENERATE_ZOD_ENUM` | the TypeScript generator | when set (any value), enums are generated as `zod.enum([...])` |

### Lock

| Method | Role |
|---|---|
| `lock()` | forbid any further setter call (they throw `IllegalStateException`); `isLocked()` reads the state |
| `allowReconfiguration(boolean)` | call **before** `lock()`; tests only |
| `unlock()` | only works if reconfiguration was allowed |
| `clearAllValue()` | reload every value from the environment, reset the entity list, unlock |

Locking after the startup prevents code from changing the database or SSO
settings later. The unlock is deliberately brutal: if reconfiguration was not
allowed, `unlock()` (and `clearAllValue()` on a locked configuration) logs a
security violation with the caller's stack trace and halts the JVM
(`Runtime.halt(1)`, no shutdown hook). Test code calls
`ConfigBaseVariable.allowReconfiguration(true)` first.


REST client: `RESTApi` and `RESTApiRequest`
-------------------------------------------

A fluent client on top of `java.net.http.HttpClient`, mainly used by the tests
and by server-to-server calls. It serializes with the archidata
`ObjectMapper` (see `ContextGenericTools` below), so `ObjectId`, `Date` and
`OffsetDateTime` use the same format as on the server.

```java
final RESTApi api = new RESTApi(ConfigBaseVariable.getLocalAddress());
api.setToken(jwt);                                  // "Authorization: Bearer <jwt>"

final Article created = api.request("articles").post().bodyJson(newArticle).fetch(Article.class);
final List<Article> page = api.request("articles").get()
        .queryParam("limit", 20).queryParam("from", someDate).fetchList(Article.class);
api.request("articles", created.oid).delete().fetch();
```

`RESTApi`: `RESTApi(String baseUrl)`, `setToken(String)`, `setToken(String token, String scheme)`,
`showIOStream()` (log every request and response), `request()` and
`request(Object... pathSegments)` (segments are joined with `/`, `null` skipped,
trailing slashes removed). It returns a new `RESTApiRequest` each call.

`RESTApiRequest`:

| Group | Methods |
|---|---|
| verb | `get()`, `post()`, `put()`, `patch()`, `delete()`, `archive()`, `restore()`, `call()`, `verb(String)`. Default is `GET`. `archive`, `restore` and `call` send the custom HTTP methods `ARCHIVE`, `RESTORE`, `CALL` used by archidata resources |
| body | `bodyJson(Object)`, `bodyAsJson(String)`, `bodyMap(Map<String,Object>)`, `bodyString(String)` / `bodyString(String, contentType)`, `bodyByte(byte[], contentType)`, `bodyMultipart(Map<String,Object>)` (a `File` value is sent as binary, anything else as text) |
| query | `queryParam(String key, Object... values)`: repeats allowed, `Date` and `ZonedDateTime` formatted in ISO, `null` becomes the text `null` |
| header | `header(String key, String value)`: adds a custom header (same key replaces the previous value) |
| debug | `showIOStream()`, `formatBody()` (pretty-print the JSON body) |
| send | `fetch(Class<T>)`, `fetchList(Class<T>)`, `fetch()` (no body), `fetchString()`, `fetchByte()`, `generateRequest()` (build the `HttpRequest` without sending) |

Errors: `fetch`, `fetch(Class)` and `fetchList` throw `RESTErrorResponseException`
for a non-2xx status whose body is the archidata error JSON (see
[error_handling.md](error_handling.md)), and `IOException` when the error body
is not JSON. The status code is in the exception's `status` field.

Pitfalls of the client:

- `fetchString()` and `fetchByte()` are raw accessors: they return the
  response whatever the status and never throw on 4xx/5xx (despite the
  `throws` clause, kept for compatibility). Check `statusCode()` yourself.
- A custom header added with `header(...)` is sent in addition to
  `Authorization` and `Content-Type`; it does not replace them.
- `fetch(Class)` with `String.class` returns the raw body; with `void.class`
  or `Void.class` it returns `null`.
- The HTTP version is forced to HTTP/1.1, and a new `HttpClient` is created for
  each call.
- `bodyJson` does not clear a body previously set as bytes (and the reverse);
  set one body per request.
- The response headers (`X-Total-Count`, `Link`) are only reachable through
  `fetchString()`; see [pagination.md](pagination.md) for the paginated
  contract.


JWT: `JWTWrapper`
-----------------

Token generation and validation, RSA key handling and the remote public key
refresh are described in [security.md](security.md). Nothing is repeated here.
The nested class `JWTWrapper.PublicKey` is the DTO of the public key exchange;
the top-level `tools.PublicKey` has the same shape, is not used by the
library and is deprecated for removal: use `JWTWrapper.PublicKey`.


Identifiers and text
--------------------

### `UuidUtils`

| Method | Role |
|---|---|
| `nextUUID()` | next UUID of the process-wide generator |
| `asUuid(BigInteger)`, `asUuid(byte[])` (16 bytes, else `DataAccessException`) | build a `UUID` |
| `asBytes(UUID)` | the 16 bytes |

The generator builds time-based UUIDs: the most significant half is the number
of milliseconds since the process started (strictly increasing, one per call
even in the same millisecond), the least significant half is made of the server
id (`UUID_SERVER_ID`, 16 bits) and the number of seconds between 2024-03-19 and
the process start. UUIDs are unique inside one process. Between processes, they
are unique as long as two processes with the same `UUID_SERVER_ID` do not start
within the same second. Set a distinct `UUID_SERVER_ID` on each instance.

The most significant half is byte-reversed, so the UUIDs are **not** sortable
by creation time.

### `StringTools`

`randGeneratedStr(int length)` (letters, digits, accented letters and a few
punctuation characters) and `generateToken(int length)` (ASCII letters, digits,
`_` and `-` only, URL-safe). Both use `SecureRandom`.

### `DateTools`

- `parseOffsetDateTime(String)` and `parseOffsetDateTime(String, boolean missingAsUTC)`:
  lenient ISO-8601 parser (`T` or space between date and time, optional
  fraction up to nanoseconds, offset as `Z`, `+05:00` or `+05`, `/` as date
  separator). The date **and** the time are required: `1999-01-30` and
  `18:16:17.123` throw `IOException`, as does any unknown format. A `null`
  input returns `null`. Without `missingAsUTC`, a string with no offset
  (`1999-01-30T18:16:17`) does not parse either.
- `parseDate(String)`: same with `missingAsUTC = true`, returned as
  `java.util.Date`.
- `serializeMilliWithOriginalTimeZone`, `serializeMilliWithUTCTimeZone`,
  `serializeNanoWithUTCTimeZone`, each for `OffsetDateTime` and for `Date`: the canonical JSON formats, with the formatters
  `PATTERN_MS_TIME_WITH_ZONE`, `PATTERN_MS_TIME`, `PATTERN_NS_TIME` public.

These are the formats used by the JSON converters and the date parameter
converters (see [converters_and_beans.md](converters_and_beans.md)).


JSON and validation
-------------------

- `ContextGenericTools.createObjectMapper()`: a Jackson `ObjectMapper` with dates
  written as ISO strings, the archidata modules (`ObjectId`, `Date`,
  `OffsetDateTime`, BSON `Document`) and `JavaTimeModule`. The backup, the JWT
  wrapper and the REST client use it.
- `ContextGenericTools.addJsr310(ResourceConfig)`: registers that mapper as the
  Jersey JSON provider and registers `ValidGroupInterceptor`. Call it in every
  server launcher.
- `ValidationProxy.create(Class<I> interface, T target)`: wraps `target` in a
  dynamic proxy of `interface` that validates every argument annotated `@Valid`
  or `@ValidGroup` before calling the method, and throws
  `ConstraintViolationException`. It lets code that is not called through
  Jersey (a service layer, a test) get the same validation as a resource.


Data files: `DataTools`
-----------------------

Storage of binary uploads (images, audio, video) with SHA-512 deduplication:
a file is first saved in the temporary folder under a numeric id, then moved
to its permanent path once its `Data` record is inserted.

- Folders and temporary files: `createFolder`, `getTmpDataId`, `getTmpFileInData`,
  `getTmpFolder`, `saveTemporaryFile(InputStream | byte[], long id)`,
  `removeTemporaryFile`, `saveFile(InputStream | byte[], String path)`.
- `Data` records: `getWithSha512`, `getWithId`, `createNewData` (two overloads,
  the short one deduces the MIME type from the extension), `undelete`.
- Upload entry points: `uploadData(InputStream, FormDataContentDisposition)`,
  `uploadDataFromUri(String url)`, `uploadCover(...)`, `uploadCoverFromUri(...)`.
  `uploadDataFromUri` and `uploadCoverFromUri` fetch the URL with
  `SafeHttpFetcher` (see below); the body is bounded by
  `setMaxRemoteDataSize(long)` / `getMaxRemoteDataSize()` (default
  `DEFAULT_MAX_REMOTE_DATA_SIZE`, 50 MiB; a larger answer gives HTTP 413), which
  also bounds `ProxyResource`.
- Type detection: `getMimeType(byte[])`, `getMimeTypeFromFile(String)`,
  `bytesToHex`, `multipartCorrection`, and the constants
  `SUPPORTED_IMAGE_MIME_TYPE` (jpeg, png, webp, svg), `SUPPORTED_AUDIO_MIME_TYPE`,
  `SUPPORTED_VIDEO_MIME_TYPE`, `SUPPORTED_UPLOAD_MIME_TYPES`, `MIME_SVG`,
  `CHUNK_SIZE` (1 MiB), `CHUNK_SIZE_IN` (50 MiB).

SVG stays accepted for upload: rendered with `<img>`, it does not run scripts.
Opening its URL directly would run them on the origin of the API, so
`DataResource` serves every stored data through
`DataTools.addSafeServingHeaders(builder, mimeType)`: `X-Content-Type-Options:
nosniff` and `Content-Security-Policy: sandbox; default-src 'none'` on every
answer, plus `Content-Disposition: attachment` for an active type
(`DataTools.isActiveMimeType`: SVG, HTML, XHTML, XML and any `+xml` type,
JavaScript). An application that serves stored files with its own resource
should call `addSafeServingHeaders` too.

`SsrfGuard.validateUrl(String)` rejects, with an `InputException`, a missing
or malformed URL, any scheme other than http/https, a URL with credentials
(`user:pass@`), an unresolvable host, and a host of which **any** resolved
address is refused by `SsrfGuard.isForbidden(InetAddress)`: loopback, private
(10/8, 172.16/12, 192.168/16), CGNAT (100.64/10), `0.0.0.0/8`, link-local
(169.254/16, `fe80::/10`), multicast and reserved ranges, IPv6 ULA (`fc00::/7`),
NAT64 (`64:ff9b::/96`), 6to4, Teredo, IPv4-compatible and IPv4-mapped IPv6
addresses. The full list is in the javadoc of `SsrfGuard`.

Validating is not enough to fetch: an HTTP client may follow a redirection to an
internal address, or resolve the name a second time and get another answer
(DNS rebinding). `SafeHttpFetcher` does the fetch safely:

```java
final SafeHttpFetcher.Result result = new SafeHttpFetcher().get(url, 10L * 1024 * 1024);
if (result.status() == 200) {
	final byte[] body = result.body();
}
```

- it resolves the host once, checks every address, and opens the TCP connection to
  the validated address itself (for https the certificate is still checked
  against the host name);
- it never follows a redirection on its own: each `Location` goes through the same
  checks, at most 3 times (a further `3xx` is returned as is);
- connection timeout 5 s, read timeout 30 s, 120 s for the whole exchange;
- the body is bounded: a larger `Content-Length` or body throws a `FailException`
  with HTTP 413.

A refused URL throws `InputException`, a network failure or timeout
`IOException`. The constructor
`SafeHttpFetcher(Predicate<InetAddress> forbidden, connectMs, readMs, totalMs, maxRedirects)`
changes the policy and the limits (the tests use it to reach a local server).

`CleanupTools` purges old soft-deleted rows:

```java
// physically remove rows soft-deleted (@DataDeleted) and last updated before the threshold
final long removed = CleanupTools.cleanupSoftDeletedRecords(Media.class, thresholdDate);
CleanupTools.cleanupAsyncHardDeletedRecords(Media.class, thresholdDate);   // @DataAsyncHardDeleted flag
```

Both need an `@UpdateTimestamp` field on the entity and the matching flag field,
otherwise they throw `DataAccessException`. Run them from a [cron](cron.md) task.


Rights
------

`RightRoleMapper` computes the `right` claim of a token from the roles; it is
documented with its example in [security.md](security.md).


Small utilities
---------------

| Class | Method | Role |
|---|---|---|
| `ListTools` | `checkListIdentical(List<?>, List<?>)` | element-by-element `equals`, `null` elements allowed |
| `TypeUtils` | `listOf(Class<?>)` | a `ParameterizedType` for `List<clazz>`, to feed a generic reader |
| | `isSameClass(Class<?>, Class<?>)` | compare two classes by name (classes loaded by different class loaders) |
| `AnnotationCreator` | `createAnnotation(Class<A>, Object... nameValuePairs)` | build an annotation instance at runtime; unspecified members take their default |


Pitfalls
--------

- `DB_ABLE_TO_CREATE` does nothing: no code in the library reads
  `getDBAbleToCreate()`. The value is parsed with `Boolean.parseBoolean`
  (unset gives `true`; anything other than `true`, ignoring case, gives `false`).
- `getDBPort()` returns an `int` and throws `IllegalArgumentException` when
  `DB_PORT` is not an integer between 1 and 65535 (unchecked, like the other
  configuration accessors). `DbConfig` stores the port as `Integer`; its
  constructors throw `DataAccessException` for a port out of 1..65535, and
  `new DbConfig()` turns the `IllegalArgumentException` of `getDBPort()` into a
  `DataAccessException` (the original one is the cause).
- `getDBPassword()` throws when no password is configured, so a MongoDB without
  authentication still needs an explicit (possibly empty) `DB_PASSWORD`.
- The environment is read once at class loading. Setting an environment variable
  from code after the first use of `ConfigBaseVariable` has no effect; use the
  setters.
- After `lock()`, any setter call throws. Tests that change the configuration
  between runs must call `allowReconfiguration(true)` before the first `lock()`.
- `getLocalAddress()` ends with a slash by default. `RESTApi` strips trailing
  slashes from the base URL, so both forms work with it.
