Getting Started Tutorial
========================

This tutorial builds, step by step, the project found in
[`sample/archidata/basic`](../sample/archidata/basic): a REST server exposing a secured CRUD on a
`Task` entity stored in MongoDB. Each step points to the matching file of the sample, which is
compiled and tested against the current archidata version: when in doubt, the sample is the
reference.


Prerequisites
-------------

- **Java 25** (the version archidata is compiled for)
- **Maven 3.9+**
- **MongoDB 6+** on `localhost:27017`, **as a replica set** (archidata uses transactions)

### MongoDB with Docker

```bash
docker run -d --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=base_db_password \
  mongo:8 --replSet rs0

# Initialize the replica set (once)
docker exec -it mongodb mongosh -u root -p base_db_password --eval "rs.initiate()"
```


Step 1: Project setup
---------------------

Layout (standard Maven):

```
basic/
├── pom.xml
└── src/
    ├── main/java/sample/archidata/basic/
    │   ├── WebLauncher.java            start-up (production)
    │   ├── WebLauncherLocal.java       start-up (development)
    │   ├── api/TaskResource.java       REST resource
    │   ├── filter/SampleAuthenticationFilter.java
    │   ├── migration/Initialization.java
    │   └── model/Task.java
    ├── main/resources/logback.xml
    └── test/java/sample/archidata/basic/SampleTest.java
```

The [`pom.xml`](../sample/archidata/basic/pom.xml) needs two dependencies:

```xml
<properties>
	<maven.compiler.release>25</maven.compiler.release>
</properties>

<dependencies>
	<dependency>
		<groupId>org.atria-soft</groupId>
		<artifactId>archidata</artifactId>
		<version>0.50.3-SNAPSHOT</version>
	</dependency>
	<!-- archidata does not bring an SLF4J backend: add one -->
	<dependency>
		<groupId>ch.qos.logback</groupId>
		<artifactId>logback-classic</artifactId>
		<version>1.6.3</version>
	</dependency>
</dependencies>
```

archidata brings Jersey, Grizzly, Jackson, the MongoDB driver, Hibernate Validator and
`jul-to-slf4j`. A released version is on Maven Central; a `-SNAPSHOT` version must first be
installed from the archidata root with `mvn install -DskipTests`.

Without a `src/main/resources/logback.xml`, Logback logs everything at DEBUG, MongoDB driver
included: the sample ships a short one at INFO.


Step 2: The data model
----------------------

File: [`model/Task.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/model/Task.java)

```java
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@Index({ "done", "-createdAt" })
public class Task extends GenericDataSoftDelete {
	@NotNull
	@Size(min = 1, max = 256)
	@Schema(description = "Short title of the task")
	private String title;

	@Column(length = 0)
	@Size(max = 4096)
	@Schema(description = "Free text description")
	private String description;

	@NotNull
	@Schema(description = "True when the task is finished")
	private Boolean done;

	// constructors, getters and setters
}
```

What the base class gives:

| Field | Source | Behaviour |
|-------|--------|-----------|
| `id` (Long) | `GenericData` | Generated on insert, stored as `_id`. Must be absent on create (`GroupCreate`). |
| `createdAt`, `updatedAt` | `GenericTiming` | Filled by the data access layer. Marked `@DataNotRead`: not returned by default reads. |
| `deleted` | `GenericDataSoftDelete` | Soft delete: `deleteById` sets the flag; default reads skip deleted documents. |

Other base classes exist: `OIDGenericData[SoftDelete]` (ObjectId id, field `oid`) and
`UUIDGenericData[SoftDelete]`. See [Data Model](data_model.md).

The collection is named after the class (`Task`); `@Table(name = "...")` changes it.

`@Index` declares a MongoDB index; it is applied by `IndexEngine` at start-up (step 5), not by the
migrations. Note that `@Column(unique = true)` creates no index: use `@Indexed(unique = true)`.
See [Indexes](indexes.md).


Step 3: The first migration
---------------------------

File: [`migration/Initialization.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/migration/Initialization.java)

```java
public class Initialization extends MigrationStep {
	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		addAction((final DBAccessMongo da) -> {
			da.insertMultiple(List.of(//
					new Task("Read the archidata README", "doc/index.md lists every topic"), //
					new Task("Write a second migration", "See doc/migration.md")));
		});
	}
}
```

The initialization runs only on an empty database, and it builds the state of the **current**
model: the engine then marks every later migration as already applied. See [Migration](migration.md).


Step 4: The REST resource
-------------------------

File: [`api/TaskResource.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/api/TaskResource.java)

```java
@Path("/task")
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {

	@GET
	@Path("count")
	@PermitAll
	public long countOpen() throws Exception {
		return DataAccess.count(Task.class, new Condition(Filters.eq("done", false)));
	}

	@GET
	@RolesAllowed("USER")
	public List<@NotNull Task> gets() throws Exception {
		return DataAccess.gets(Task.class);
	}

	@GET
	@Path("{id}")
	@RolesAllowed("USER")
	public Task get(@PathParam("id") final Long id) throws Exception {
		final Task task = DataAccess.getById(Task.class, id);
		if (task == null) {
			throw new NotFoundException("Task not found: " + id);
		}
		return task;
	}

	@POST
	@RolesAllowed("ADMIN")
	@Consumes(MediaType.APPLICATION_JSON)
	public Task post(@Valid @ValidGroup(GroupCreate.class) final Task task) throws Exception {
		return DataAccess.insert(task);
	}

	@PUT
	@Path("{id}")
	@RolesAllowed("ADMIN")
	@Consumes(MediaType.APPLICATION_JSON)
	public Task put(@PathParam("id") final Long id, @Valid @ValidGroup(GroupUpdate.class) final Task task)
			throws Exception {
		final long updated = DataAccess.updateById(task, id);
		if (updated == 0) {
			throw new NotFoundException("Task not found: " + id);
		}
		return DataAccess.getById(Task.class, id);
	}

	@DELETE
	@Path("{id}")
	@RolesAllowed("ADMIN")
	public void remove(@PathParam("id") final Long id) throws Exception {
		DataAccess.deleteById(Task.class, id);
	}
}
```

Points to know:

- **Every endpoint declares its access** with `@PermitAll`, `@RolesAllowed` or `@RightAllowed`.
  The authentication filter answers 403 to an endpoint that declares none.
- `DataAccess` is the static facade over `DBAccessMongo`: it opens (or reuses) the connection for
  the call. See [Database Access](database_access.md).
- `@Valid` checks the Default-group constraints of the model (`@NotNull`, `@Size`).
  `@ValidGroup(GroupCreate.class)` checks the create-only ones, e.g. "`id` must be null". Both are
  needed: `@ValidGroup` validates *only* the listed groups.
- `updateById` without a `FilterValue` option writes every editable field, `null` values included
  (full replacement). Pass `new FilterValue("title")` to update only some fields.
- Exceptions (`NotFoundException`, `InputException`, validation errors...) become a JSON error body
  with the right HTTP status through the catchers registered in step 5.


Step 5: Authentication filter and launcher
------------------------------------------

File: [`filter/SampleAuthenticationFilter.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/filter/SampleAuthenticationFilter.java)

```java
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SampleAuthenticationFilter extends AuthenticationFilter {
	public static final String APPLICATION_NAME = "sample";

	public SampleAuthenticationFilter() {
		super(APPLICATION_NAME);
	}
}
```

The base class validates the `Authorization: Bearer <JWT>` header and enforces the access
annotations. The application name selects the group of roles read in the token:
`@RolesAllowed("ADMIN")` requires `{"roles": {"sample": {"ADMIN": 3}}}` (3 = `PartRight.READ_WRITE`).
`@RolesAllowed("USER")` only requires the `sample` group to be present. See [Security](security.md).

File: [`WebLauncher.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/WebLauncher.java)

The launcher runs, in order: migrations, index synchronization, HTTP server.

```java
public void migrateDB() throws Exception {
	final MigrationEngine migrationEngine = new MigrationEngine();
	migrationEngine.setInit(new Initialization());
	// migrationEngine.add(new Migration20261101());
	migrationEngine.migrateWaitAdmin(new DbConfig());
}

public void synchronizeIndexes() throws Exception {
	final IndexEngine indexes = new IndexEngine();
	indexes.addClass(ENTITIES);
	indexes.synchronize();
}

public void process() throws Exception {
	final ResourceConfig rc = new ResourceConfig();
	rc.register(OptionFilter.class);
	rc.register(CORSFilter.class);
	rc.register(SampleAuthenticationFilter.class);
	GenericCatcher.addAll(rc);
	rc.register(TaskResource.class);
	ContextGenericTools.addJsr310(rc);
	rc.register(JacksonFeature.class);
	rc.register(ValidationFeature.class);

	// false: the server is created but not started yet
	this.server = GrizzlyHttpServerFactory.createHttpServer(getBaseURI(), rc, false);
	startJwtKeyUpdater(); // new UpdateJwtPublicKey().start(), overridden by WebLauncherLocal
	this.server.start();
}
```

| Registration | Role |
|--------------|------|
| `OptionFilter`, `CORSFilter` | Answer the CORS pre-flight `OPTIONS` requests, add the CORS headers. |
| `SampleAuthenticationFilter` | JWT check and access annotations. Without it, `@RolesAllowed` is ignored. |
| `GenericCatcher.addAll(rc)` | All the exception catchers: errors become JSON with the right status. |
| `ContextGenericTools.addJsr310(rc)` | archidata's Jackson mapper (java.time, ObjectId) **and** the `@ValidGroup` interceptor. Without it, `@ValidGroup` is silently ignored. |
| `ValidationFeature` | Bean Validation of the `@Valid` parameters. |
| `UpdateJwtPublicKey` | Thread fetching the JWT public key from `SSO_ADDRESS`; stops at once when it is not set. Started by `startJwtKeyUpdater()`, before the server. |

### Configuration

`ConfigBaseVariable` reads the environment once, at class loading, and exposes setters for code
configuration:

| Variable | Setter | Default |
|----------|--------|---------|
| `DB_HOST` | `setDbHost` | `localhost` |
| `DB_PORT` | `setDbPort` | `27017` |
| `DB_USER` | `setDbUser` | `root` |
| `DB_PASSWORD` | `setDbPassword` | none: **mandatory** |
| `DB_DATABASE` | `setBdDatabase` | none (the sample sets `sample_basic`) |
| `API_ADDRESS` | `setApiAddress` | `http://localhost:80/api/` (address the server listens on) |
| `SSO_ADDRESS` | `setSsoAddress` | none |

`new DbConfig()` builds the database configuration from these values.

### Local launcher

File: [`WebLauncherLocal.java`](../sample/archidata/basic/src/main/java/sample/archidata/basic/WebLauncherLocal.java)

For development without SSO, it hard-codes the configuration, generates a local RSA key pair with
`JWTWrapper.initLocalToken(null)`, and prints a USER and an ADMIN token signed with it. It overrides `startJwtKeyUpdater()` to do nothing,
otherwise `UpdateJwtPublicKey` would replace the local key by the SSO one when `SSO_ADDRESS` is set:

```java
final Map<String, Object> roles = Map.of("sample", Map.of(role, PartRight.READ_WRITE.getValue()));
return JWTWrapper.generateJWToken(new ObjectId(), login, "Karso", "sample", roles, null, 24 * 60);
```

The subject must be an `ObjectId` and the issuer must be the one expected by the filter (`Karso`
by default). Role values must be `PartRight` codes (int) or names: a `Boolean` is rejected.


Step 6: Build, test and run
---------------------------

From the archidata root:

```bash
mvn install -DskipTests                               # current archidata in ~/.m2
mvn -f sample/archidata/basic/pom.xml test            # compile + tests (no MongoDB needed)
mvn -f sample/archidata/basic/pom.xml exec:java@local # start with WebLauncherLocal
```

`mvn exec:java` (without `@local`) starts `WebLauncher`, configured by the environment.

The log shows the migration, the index creation, the two tokens, then
`Server started at http://127.0.0.1:9000/sample/api/`.


Step 7: Try it with curl
------------------------

Copy the two tokens from the log into `USER_TOKEN` and `ADMIN_TOKEN`.

```bash
API=http://127.0.0.1:9000/sample/api/task

# Public endpoint
curl $API/count
# 2

# No token -> 401; USER token -> the two tasks created by the migration
curl -H "Authorization: Bearer $USER_TOKEN" $API
# [{"id":1,"title":"Read the archidata README",...},{"id":2,...}]

# Create: USER token -> 403, ADMIN token -> created
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
     -d '{"title": "Buy milk", "done": false}' $API
# {"id":3,"title":"Buy milk","done":false}

# Validation errors -> 400 with the faulty field
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
     -d '{"done": false}' $API
# {"name":"Error on input='title'", ..., "status":400, "inputError":[{"path":"title","message":"must not be null"}]}

# Replace, then delete (soft delete: the next GET answers 404)
curl -X PUT -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
     -d '{"title": "Buy milk", "done": true}' $API/3
curl -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" $API/3
curl -H "Authorization: Bearer $USER_TOKEN" $API/3
```


Next steps
----------

- [Data Model](data_model.md): annotations, relations, base classes
- [Database Access](database_access.md): conditions, ordering, pagination, transactions
- [Migration](migration.md): evolving the database
- [Indexes](indexes.md): `@Index`, `@Indexed` and `IndexEngine`
- [Security](security.md): JWT, roles and fine-grained rights
- [Connection Management](connectionManagement.md)
