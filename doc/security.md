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
JWTWrapper.initLocalTokenRemote("https://sso.example.com", "my-app");
```

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
	userId,          // Object: user ID (ObjectId or Long)
	"john.doe",      // String: user login
	"my-app",        // String: issuer
	"my-app",        // String: application
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
  "iss": "KarAuth",
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
	@Column(nullable = false, length = 200)
	public String title;
	@Column(length = 0)
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

`createJwtTestToken()` also takes an additional `rights` parameter:

**Before:**
```java
JWTWrapper.createJwtTestToken(userId, login, issuer, app, roles);
```

**After:**
```java
JWTWrapper.createJwtTestToken(userId, login, issuer, app, roles, rights);
// rights can be null if not needed
```

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

Update all `createJwtTestToken()` calls to add the `rights` parameter (pass `null` if not needed):

**Before:**
```java
JWTWrapper.createJwtTestToken(16512, "test_user", "KarAuth", "myapp",
    Map.of("myapp", Map.of("USER", PartRight.READ)));
```

**After:**
```java
JWTWrapper.createJwtTestToken(16512, "test_user", "KarAuth", "myapp",
    Map.of("myapp", Map.of("USER", PartRight.READ)), null);
```

### 8. Force re-login

All users must re-authenticate after this migration. Existing JWT tokens contain `"right"` instead of `"roles"`, so the roles will not be found and access will be denied.
