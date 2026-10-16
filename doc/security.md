Security & Authentication
========================

Archidata provides a complete authentication and authorization system based on JWT tokens, with role-based and resource-level access control.


Overview
--------

The security system consists of:
1. **AuthenticationFilter** — a JAX-RS filter that intercepts all requests and validates tokens
2. **JWTWrapper** — handles JWT token generation and validation (RSA 2048-bit, RS256)
3. **UserByToken** — represents the authenticated user extracted from the token
4. **MySecurityContext** — provides role and permission checks in your endpoints
5. **@RightAllowed** — annotation for fine-grained resource-level access control
6. **RightRoleMapper** — utility for generating rights from role mappings


Authentication Flow
-------------------

```
Client request
    │
    ▼
AuthenticationFilter
    ├── Check @DenyAll → 403 Forbidden
    ├── Check @PermitAll → pass through
    ├── Require @RolesAllowed or @RightAllowed (else 403)
    ├── Extract token from headers or URI
    ├── Validate token (JWT or API key)
    ├── Build UserByToken from claims (roles + rights)
    ├── Check @RolesAllowed → verify roles (OR logic)
    ├── Check @RightAllowed → verify resource right (bitmask)
    ├── Both must pass if both present (AND logic)
    └── Set SecurityContext on the request
    │
    ▼
Your endpoint method
```


Supported Token Formats
-----------------------

The filter accepts tokens in three ways:

| Method | Header / Parameter | Format |
|--------|--------------------|--------|
| Bearer JWT | `Authorization` header | `Bearer <jwt_token>` |
| API Key | `ApiKey` header | `<api_key_token>` |
| API Key in Auth | `Authorization` header | `ApiKey <api_key_token>` |
| URI token | Query parameter | `?Authorization=<token>` (requires `@PermitTokenInURI`) |


Setting Up JWT
--------------

### Generate a local key pair

For standalone servers that issue their own tokens:

```java
// Generate RSA key pair with a base UUID
JWTWrapper.initLocalToken("my-application-uuid");
```

### Connect to a remote SSO

For applications that validate tokens issued by a separate SSO server:

```java
JWTWrapper.initLocalTokenRemote("https://sso.example.com/karso/api/", "my-app");
```

`UpdateJwtPublicKey` (a daemon thread) calls it with `SSO_ADDRESS` and refreshes the key every 5 minutes.
The fetch is protected:

- the address must be `https://`; `http://` is accepted only for a loopback host (`localhost`, `127.x`,
  `::1`) or after `JWTWrapper.setAllowInsecureSsoAddress(true)` (plain HTTP exposes `SSO_TOKEN` and lets
  anyone on the path replace the key, then forge tokens);
- connection timeout 5 s, read timeout 10 s, answer bounded to 64 KiB, redirections not followed (they
  would carry `SSO_TOKEN` elsewhere);
- an RSA key under 2048 bits is refused; the thumbprint of a new key is logged at INFO;
- `SSO_TOKEN` and the answer are never logged.

A token with an unknown signature asks the thread for an immediate refresh (the SSO may have rotated its
key), at most once per `JWTWrapper.MIN_REFRESH_INTERVAL` (60 s): anonymous clients sending bad tokens
cannot make the server hammer the SSO. Invalid, expired or malformed tokens are logged at WARN without a
stack trace.

### Load a public key directly

For token validation only:

```java
JWTWrapper.initValidateToken(publicKeyString);
```

### Generate a token

```java
// Roles (high-level: ADMIN, USER, etc.)
final Map<String, Object> roles = new HashMap<>();
final Map<String, Object> appRoles = new HashMap<>();
appRoles.put("ADMIN", PartRight.READ_WRITE.getValue());
appRoles.put("USER", PartRight.READ_WRITE.getValue());
roles.put("myapp", appRoles);

// Rights (fine-grained resource-level: articles, users, etc.)
final Map<String, Object> rights = new HashMap<>();
final Map<String, Object> appRights = new HashMap<>();
appRights.put("articles", PartRight.READ_WRITE.getValue());
appRights.put("users", PartRight.READ.getValue());
rights.put("myapp", appRights);

final String token = JWTWrapper.generateJWToken(
	userId,          // Object: user ID (must be an ObjectId: AuthenticationFilter rejects other types)
	"john.doe",      // String: user login
	"my-app",        // String: issuer
	"my-app",        // String: application (also written as the standard "aud" claim)
	roles,           // Map<String, Object>: roles map (can be null)
	rights,          // Map<String, Object>: rights map (can be null)
	60               // int: timeout in minutes
);
```


Securing Endpoints
------------------

### @RolesAllowed

Annotation for role-based access control. Every endpoint should have at least one of `@RolesAllowed`, `@RightAllowed`, `@PermitAll`, or `@DenyAll`.

```java
@GET
@Path("/admin/stats")
@RolesAllowed("ADMIN")
public Response getStats() { ... }
```

### @RightAllowed

Annotation for fine-grained resource-level access control. Checks that the user has the required access level for a specific right using bitmask logic.

```java
@GET
@Path("/articles")
@RightAllowed(right = "articles", access = PartRight.READ)
public List<Article> listArticles() { ... }

@PUT
@Path("/articles/{id}")
@RightAllowed(right = "articles", access = PartRight.WRITE)
public Response updateArticle(@PathParam("id") final ObjectId id, final Article data) { ... }
```

### Combining @RolesAllowed and @RightAllowed

When both annotations are present, **both must pass** (AND logic):

```java
@DELETE
@Path("/articles/{id}")
@RolesAllowed("ADMIN")
@RightAllowed(right = "articles", access = PartRight.WRITE)
public Response deleteArticle(@PathParam("id") final ObjectId id) { ... }
// User must be ADMIN AND have articles:WRITE
```

### @PermitAll

Allow any authenticated user (token must be valid, but no specific role is required):

```java
@GET
@Path("/public/info")
@PermitAll
public Response getPublicInfo() { ... }
```

### @DenyAll

Block all access to an endpoint:

```java
@GET
@Path("/disabled")
@DenyAll
public Response disabled() { ... }
```

### @PermitTokenInURI

Allow the token to be passed as a query parameter instead of a header. Useful for download endpoints or streaming where headers cannot be set:

```java
@GET
@Path("/download/{fileId}")
@PermitTokenInURI
@RolesAllowed("USER")
public Response downloadFile(@PathParam("fileId") final ObjectId fileId) { ... }
```

The client sends: `GET /download/abc123?Authorization=Bearer+<token>`


Permission Model
================

### JWT Token Structure

The JWT token contains two separate permission maps:

```json
{
  "sub": "68e6d5bfbc9e2533fd0763a9",
  "login": "john.doe",
  "application": "myapp",
  "iss": "Karso",
  "roles": {
    "myapp": {
      "ADMIN": 3,
      "USER": 3
    }
  },
  "right": {
    "myapp": {
      "articles": 3,
      "users": 1
    }
  },
  "iat": 1774185262,
  "exp": 1774225222
}
```

- **`roles`** — high-level roles (ADMIN, USER) checked by `@RolesAllowed`
- **`right`** — fine-grained resource rights (articles, users) checked by `@RightAllowed`

Both are optional. An endpoint using only `@RolesAllowed` does not require `right` in the token, and vice versa.

### Structure

Both maps share the same hierarchical structure:

```
Map<group, Map<name, PartRight>>
```

### PartRight values

| Value | Name | Description |
|-------|------|-------------|
| 0 | `NONE` | No access |
| 1 | `READ` | Read-only access |
| 2 | `WRITE` | Write-only access |
| 3 | `READ_WRITE` | Full access |

### Role format in @RolesAllowed

The `@RolesAllowed` annotation supports an extended format for fine-grained permission checks:

| Format | Meaning |
|--------|---------|
| `"USER"` | Any authenticated user with at least one group |
| `"admin"` | User has the "admin" role in any group |
| `"myapp/users"` | User has any permission on "users" role in "myapp" group |
| `"myapp/users:r"` | User has READ permission on "users" in "myapp" |
| `"myapp/users:w"` | User has WRITE permission on "users" in "myapp" |
| `"myapp/users:rw"` | User has READ_WRITE permission on "users" in "myapp" |

Example:

```java
@PUT
@Path("/{id}")
@RolesAllowed("myapp/articles:w")
public Response updateArticle(
		@PathParam("id") final ObjectId id,
		final Article data) { ... }
```


Accessing User Info in Endpoints
================================

### From SecurityContext

```java
@GET
@Path("/me")
@RolesAllowed("USER")
public Response getCurrentUser(@Context final SecurityContext sc) {
	final MySecurityContext ctx = (MySecurityContext) sc;

	// Get user's ObjectId
	final ObjectId userId = ctx.getUserID();

	// Check group membership (roles)
	final boolean isAdmin = ctx.groupExist("admin");

	// Check specific role permissions
	final PartRight roleRight = ctx.getRightOfRoleInGroup("myapp", "ADMIN");

	// Check fine-grained resource right (programmatic)
	final boolean canEditArticles = ctx.hasResourceRight("myapp", "articles", PartRight.WRITE);

	// Get raw resource right value
	final PartRight articlesRight = ctx.getResourceRight("myapp", "articles");

	return Response.ok(da.getById(User.class, userId)).build();
}
```

### Available methods on MySecurityContext

#### Role checking (operates on `roles` claim)

| Method | Returns | Description |
|--------|---------|-------------|
| `getUserID()` | `ObjectId` | The authenticated user's ID |
| `getGroups()` | `Set<String>` | All role group names |
| `groupExist(group)` | `boolean` | Check if user has a specific group |
| `getRightOfRoleInGroup(group, role)` | `PartRight` | Get permission level for a role in a group |
| `checkRightInGroup(group, role, needRead, needWrite)` | `boolean` | Check if user has required read/write access for a role |
| `isUserInRole(role)` | `boolean` | Check role (supports extended format with `:r`, `:w`, `:rw` suffixes) |

#### Resource right checking (operates on `right` claim)

| Method | Returns | Description |
|--------|---------|-------------|
| `hasResourceRight(app, rightName, required)` | `boolean` | Check resource right with bitmask logic: `(user & required) == required` |
| `getResourceRight(app, rightName)` | `PartRight` | Get the raw right value for a resource |


RightRoleMapper — Generating Rights from Roles
===============================================

The `RightRoleMapper` utility generates fine-grained rights from a user's roles. This is typically used server-side (e.g., in karso) when generating JWT tokens, to automatically compute the `right` claim from the `roles`.

### Configuration

```java
// Define what rights each role grants
final RightRoleMapper mapper = new RightRoleMapper(Map.of(
    "ADMIN", List.of(
        new RightRoleMapper.RightDefinition("articles", PartRight.READ_WRITE),
        new RightRoleMapper.RightDefinition("users", PartRight.READ_WRITE),
        new RightRoleMapper.RightDefinition("settings", PartRight.READ_WRITE)
    ),
    "EDITOR", List.of(
        new RightRoleMapper.RightDefinition("articles", PartRight.READ_WRITE),
        new RightRoleMapper.RightDefinition("users", PartRight.READ)
    ),
    "USER", List.of(
        new RightRoleMapper.RightDefinition("articles", PartRight.READ),
        new RightRoleMapper.RightDefinition("users", PartRight.READ)
    )
));
```

### Usage during token generation

```java
// User has roles: {EDITOR: RW, USER: RW}
final Map<String, PartRight> userRoles = Map.of(
    "EDITOR", PartRight.READ_WRITE,
    "USER", PartRight.READ_WRITE
);

// Generate rights from roles (bitmask OR fusion)
final Map<String, PartRight> rights = mapper.generateRights(userRoles);
// Result: {articles: READ_WRITE, users: READ}

// Use both in token generation
final String token = JWTWrapper.generateJWToken(userId, login, issuer, app,
    rolesMap, rightsMap, timeoutMinutes);
```

When multiple roles grant the same right, access levels are merged using bitmask OR (e.g., `READ | WRITE = READ_WRITE`). Unknown roles are ignored.


Registering the Authentication Filter
======================================

In your `WebLauncher`, register the authentication filter with your JAX-RS application:

```java
final ResourceConfig rc = new ResourceConfig();
rc.register(AuthenticationFilter.class);
// ... other registrations
```

> **Note:** Without the filter registered, no authentication or authorization checks are performed.


Complete Example
================

### Model

```java
public class Article extends OIDGenericDataSoftDelete {
	@NotNull
	@Size(max = 200)
	public String title;
	public String content;
	@CheckForeignKey(User.class)
	public ObjectId authorId;
}
```

### Resource

```java
@Path("/articles")
public class ArticleResource {

	@GET
	@RolesAllowed("USER")
	public List<Article> list() throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.gets(Article.class,
			new OrderBy(new OrderItem("createdAt", Order.DESC)),
			new Limit(50)
		);
	}

	@POST
	@RolesAllowed("myapp/articles:w")
	public Article create(final Article data, @Context final SecurityContext sc) throws Exception {
		final MySecurityContext ctx = (MySecurityContext) sc;
		data.authorId = ctx.getUserID();
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.insert(data);
	}

	@GET
	@Path("/{id}")
	@RolesAllowed("USER")
	public Article get(@PathParam("id") final ObjectId id) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		final Article article = da.getById(Article.class, id);
		if (article == null) {
			throw new NotFoundException("Article not found");
		}
		return article;
	}

	@PUT
	@Path("/{id}")
	@RolesAllowed("myapp/articles:w")
	public Response update(
			@PathParam("id") final ObjectId id,
			final Article data) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		da.updateById(data, id, new FilterValue("title", "content"));
		return Response.ok().build();
	}

	@DELETE
	@Path("/{id}")
	@RolesAllowed("ADMIN")
	@RightAllowed(right = "articles", access = PartRight.WRITE)
	public Response delete(@PathParam("id") final ObjectId id) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		da.deleteById(Article.class, id);
		return Response.ok().build();
	}
}
```


Web hardening
=============

What archidata does to protect the API and its users, and what is left to the application.

### Token audience

`JWTWrapper.generateJWToken` writes the standard `aud` claim with the application name, next to the
`application` claim. `AuthenticationFilter` accepts a JWT only when it is meant for the application
name given to its constructor: `aud` must contain it, or, for a token without `aud` (issued by an older
archidata), `application` must equal it. Otherwise the answer is 401. Several applications that trust
the same SSO key therefore no longer accept each other's tokens. `JWTWrapper.validateToken(token,
issuer, application)` does the same check when `application` is not `null`
(`JWTWrapper.isForApplication(claims, application)` exposes it).

Test tokens must name the application of the filter of the test server, as `Common.USER_TOKEN` does
with `TestAuthenticationFilter.APPLICATION_NAME`.

### Token validation

`JWTWrapper.validateToken` checks the RSA signature (`alg=none` and HMAC tokens are refused), then the
expiration: a token **without** `exp` is refused (it would be valid forever), as is an expired one. A
`nbf` (not before) claim, when present, must not be later than now plus 30 s of clock tolerance
(`JWTWrapper.MAX_CLOCK_SKEW_MS`). Then come the issuer and the audience. There is no revocation list: keep
the token lifetime short.

### Server-side requests (SSRF)

`DataResource.uploadMediaFromUri` (`POST /data/uploadUri`), `DataTools.uploadCoverFromUri` and
`ProxyResource` fetch a URL given by the caller. They go through `SafeHttpFetcher`: only `http` and
`https`, the host resolved once and every address checked by `SsrfGuard.isForbidden` (private,
loopback, link-local, CGNAT, multicast, ULA, NAT64, IPv4-mapped…), the connection opened to that
validated address (no DNS rebinding), no automatic redirection (each `Location` is checked again, 3 at
most), timeouts (5 s to connect, 30 s per read, 120 s in all) and a size limit
(`DataTools.setMaxRemoteDataSize`, 50 MiB by default, checked on the `Content-Length` before reading,
then while reading). `ProxyResource` answers raster images with their type and anything
else as `application/octet-stream`, with `X-Content-Type-Options: nosniff`, and no longer sends
`Access-Control-Allow-Origin: *`. Any server-side fetch of a user-supplied URL in an application
should use `SafeHttpFetcher` too (see [tools](tools.md)).


### Serving uploaded files

A file uploaded by a user and served on the origin of the API must never run there: the front is
usually served by the same server and keeps its token in `localStorage`. `DataResource` (`GET
/data/{oid}`, `/data/{oid}/{name}`, `/data/thumbnail/{oid}`) adds to every answer
`X-Content-Type-Options: nosniff` and `Content-Security-Policy: sandbox; default-src 'none'`, and
`Content-Disposition: attachment` when the type is active (SVG, HTML, XHTML, XML, JavaScript). An
`<img src>` or `<video src>` still renders the file; opening its URL downloads it instead of running it.

Choice: SVG remains accepted for upload (covers), because these headers already keep it from running.
To refuse it, filter the type in the application before calling `DataTools.uploadData`.


### Upload limits

`POST /data/upload` (`DataTools.uploadData`) and `DataTools.uploadCover` stop copying the stream above
`DataTools.setMaxUploadSize` (1 GiB by default) and answer 413. The temporary file of an upload is
always removed (refused type, data already stored, error), and the copy uses a 64 KiB buffer instead of
one of 50 MiB per request. An application that copies a client stream itself should use the bounded
`DataTools.saveTemporaryFile(stream, id, maxSize)`. Grizzly and Jersey do not limit the request body by
themselves: put a limit in the reverse proxy too (`client_max_body_size` for nginx).


### Thumbnails

`GET /data/thumbnail/{oid}` reads the dimensions of a JPEG or PNG from its header before decoding it,
and answers 415 above 20 000 pixels on a side or 100 Mpx in all
(`DataResource.MAX_THUMBNAIL_SOURCE_DIMENSION`, `MAX_THUMBNAIL_SOURCE_PIXELS`): a PNG of a few KiB
can declare a picture of several GiB. The picture is then decoded sub-sampled, at about twice the
thumbnail width. The thumbnail is still computed on every request that the browser cache (ETag,
`Cache-Control: private, immutable`) does not absorb: there is no server-side cache.


### Static front files

`FrontGeneric` resolves the requested file against the real path of `baseFrontFolder` and compares
normalized `Path`s (`Path.startsWith(Path)`, whole name elements): `../front-old/config.json` is
refused even when the base is `/data/front`, and so is a symbolic link that leads out of the base. The
404 messages no longer repeat the requested name.


### CORS

`CORSFilter` reads the allowed origins from `CORS_ALLOWED_ORIGINS` (comma separated, see
[tools](tools.md); `ConfigBaseVariable.setCorsAllowedOrigins` in code):

| `CORS_ALLOWED_ORIGINS` | `Origin` of the request | Answer |
|---|---|---|
| not set | any | `Access-Control-Allow-Origin: *`, no `Access-Control-Allow-Credentials` |
| set | in the list | that origin, `Vary: Origin`, `Access-Control-Allow-Credentials: true` |
| set | not in the list, or absent | no CORS header at all |

The comparison is exact (scheme, host and port: `https://app.example.com`, without trailing slash).
Methods, allowed and exposed headers are sent only with an allowed origin. Override
`CORSFilter.allowedOrigins()` to take the list from elsewhere.

CORS only concerns browsers: it decides whether a page of another origin may read the answer. A
server-to-server call (another back-end, a script, curl) ignores these headers and is not affected. It
is not an access control either: the endpoints keep their `@RolesAllowed`. Set the list in production
to the origins of the fronts, so that a page of another site visited by a user of the internal network
cannot read the API.

### Error messages

The error answers no longer carry internal details: an unexpected exception, a 5xx
`WebApplicationException` and a raw Jackson error answer `Internal server error`; an invalid request
body names the field or the position only; every 401 of `AuthenticationFilter` says `Missing or invalid
authentication`. The detail is logged with the `oid` of the answer. Messages thrown on purpose
(`FailException`, `InputException`, `SystemException`, validation) are unchanged. See
[error handling](error_handling.md#what-the-client-sees).


### API documentation

`openApiResource` (`/openapi/swagger.json`) and `SwaggerUiExternal` are public (`@PermitAll`) and answer 404
unless `API_DOC_ENABLED=true`. The Swagger UI page loads its scripts and stylesheet from cdnjs with an
`integrity` hash and `crossorigin="anonymous"`: a modified file on the CDN is not run on the origin of the
API.


### Security headers

Register `SecurityHeadersFilter` next to `CORSFilter` (`rc.register(SecurityHeadersFilter.class)`, as the
[sample](../sample/archidata/basic) does). It adds to every answer, unless the resource already set them:

| Header | Value | Why |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | the browser never guesses an executable type |
| `Referrer-Policy` | `no-referrer` | an API URL (possibly with a token in its query string) never leaks to another site |
| `X-Frame-Options` | `DENY` | no page of the server can be framed (clickjacking) |
| `Strict-Transport-Security` | `max-age=31536000`, over https only | the browser refuses plain http afterwards |

Behind a reverse proxy that terminates TLS, the server sees http and sends no HSTS: let the proxy send it.
The files served by `DataResource` also get a sandbox CSP (see above). A front served by `FrontGeneric` that
needs to be framed must set its own `X-Frame-Options`, or not register the filter.

### Logs

No credential is logged in clear: the JWT is truncated to its first 20 characters when refused,
`SSO_TOKEN` is never logged, and `RESTApiRequest.showIOStream()` masks the `Authorization`, `ApiKey`,
`Cookie` and `Set-Cookie` headers and the `Authorization` / `ApiKey` query parameters
(`RESTApiRequest.maskHeader`, `maskUrl`), and logs the request and response bodies only at TRACE.
`MySecurityContext.getRole` logs the caller at TRACE. The access log of the reverse proxy still records the
token of a `@PermitTokenInURI` request: exclude the query string from it.

### Not handled by archidata

- **Rate limiting**: none (brute force, enumeration, upload floods). Put it in the reverse proxy.
- **Class-level security annotations**: `AuthenticationFilter` only reads `@RolesAllowed`, `@PermitAll`,
  `@RightAllowed` and `@DenyAll` on the method (or the interface method). A method without one is refused
  with 403, even if its class carries one.
- **`MySecurityContext.isSecure()`** reflects the scheme seen by Grizzly: behind a TLS proxy it is false.


Migration Guide (Breaking Change)
==================================

This is a **breaking change**. All existing JWT tokens become invalid after upgrading.

### 1. JWT Token Format

The `"right"` claim has been renamed to `"roles"`. A new `"right"` claim has been added for fine-grained resource rights.

**Before:**
```json
{
  "right": {"myapp": {"ADMIN": 3, "USER": 3}}
}
```

**After:**
```json
{
  "roles": {"myapp": {"ADMIN": 3, "USER": 3}},
  "right": {"myapp": {"articles": 3, "users": 1}}
}
```

### 2. JWTWrapper API

`generateJWToken()` now takes two separate maps (roles + rights):

**Before:**
```java
JWTWrapper.generateJWToken(userId, login, issuer, app, rights, timeout);
```

**After:**
```java
JWTWrapper.generateJWToken(userId, login, issuer, app, roles, rights, timeout);
// roles and rights can each be null if not needed
```

`createJwtTestToken()` has been removed. Tests now use real RSA keys:

```java
// In your test Common class:
static {
    try {
        JWTWrapper.initLocalToken(null); // Ephemeral RSA key pair
    } catch (final Exception e) {
        throw new RuntimeException("Failed to init JWT keys for tests", e);
    }
}

// The subject must be an ObjectId and the role values PartRight codes:
// AuthenticationFilter rejects a Long subject or Boolean roles (401).
static final String TOKEN = JWTWrapper.generateJWToken(
    new ObjectId("000000000000000000004081"), "test_user", "Karso", "myapp",
    Map.of("myapp", Map.of("USER", PartRight.READ.getValue())), null, 3600);
```

archidata's own tests do the same in `src/test/.../apiExtern/Common.java` (`USER_TOKEN`,
`ADMIN_TOKEN`), with roles keyed by the application name of the test filter.

### 3. UserByToken API

The `right` field has been split into `roles` and `right`:

| Before | After | Description |
|--------|-------|-------------|
| `getRight()` | `getRoles()` | Get the roles map |
| `setRight(map)` | `setRoles(map)` | Set the roles map |
| `getRightForKey(group, key)` | `getRoleForKey(group, key)` | Get a role value |
| — | `getRight()` | Get the fine-grained rights map |
| — | `setRight(map)` | Set the fine-grained rights map |
| — | `getRightForKey(group, key)` | Get a right value |
| `getGroups()` | `getGroups()` | Now operates on roles |
| `groupExist(group)` | `groupExist(group)` | Now operates on roles |

### 4. AuthenticationFilter

- `checkRight()` has been renamed to `checkRole()` (if you override it in a subclass)
- `checkResourceRight()` has been added for `@RightAllowed` checks
- The security guard now accepts `@RolesAllowed` **or** `@RightAllowed` (previously only `@RolesAllowed`)

### 5. AuthenticationFilter subclasses

If your subclass calls `userByToken.getRight().put(...)` to add roles for API key tokens, change to `userByToken.getRoles().put(...)`:

**Before:**
```java
userByToken.getRight().put("myapp", Map.of("APPLICATION", PartRight.READ_WRITE));
```

**After:**
```java
userByToken.getRoles().put("myapp", Map.of("APPLICATION", PartRight.READ_WRITE));
```

### 6. MySecurityContext

New methods available for programmatic resource-right checking:

```java
final MySecurityContext ctx = (MySecurityContext) securityContext;
final boolean canRead = ctx.hasResourceRight("myapp", "articles", PartRight.READ);
final PartRight level = ctx.getResourceRight("myapp", "articles");
```

### 7. Test tokens

`createJwtTestToken()` has been removed. Replace with real RSA-signed tokens:

**Before:**
```java
ConfigBaseVariable.testMode = "true";
JWTWrapper.createJwtTestToken(16512, "test_user", "Karso", "myapp",
    Map.of("myapp", Map.of("USER", PartRight.READ)), null);
```

**After:**
```java
// In a static block (before token creation):
JWTWrapper.initLocalToken(null);

// Then generate tokens with real RSA signatures:
JWTWrapper.generateJWToken(new ObjectId(), "test_user", "Karso", "myapp",
    Map.of("myapp", Map.of("USER", PartRight.READ.getValue())), null, 3600);
```

### 8. Force re-login

All users must re-authenticate after this migration. Existing JWT tokens contain `"right"` instead of `"roles"`, so the roles will not be found and access will be denied.
