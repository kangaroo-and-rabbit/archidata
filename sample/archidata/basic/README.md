archidata sample: basic
=======================

The smallest complete application built on archidata: one MongoDB entity, one secured CRUD
resource, one migration, one Grizzly/Jersey launcher. Copy it to start a new project; the
step-by-step explanation is in [doc/TUTORIAL_01_first_install.md](../../../doc/TUTORIAL_01_first_install.md).

Run it
------

Requirements: Java 25, Maven, a MongoDB replica set on `localhost:27017` (user `root`, password
`base_db_password`, see the tutorial for a Docker command).

```bash
# 1. From the archidata root: install the current archidata version in ~/.m2
mvn install -DskipTests
# 2. Build and test the sample (the tests need neither MongoDB nor a server)
mvn -f sample/archidata/basic/pom.xml test
# 3. Start it with the local configuration; the log prints a USER and an ADMIN token
mvn -f sample/archidata/basic/pom.xml exec:java@local
```

```bash
curl http://127.0.0.1:9000/sample/api/task/count                         # public
curl -H "Authorization: Bearer $USER_TOKEN" http://127.0.0.1:9000/sample/api/task
curl -X POST -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
     -d '{"title": "Buy milk", "done": false}' http://127.0.0.1:9000/sample/api/task
```

What each file shows
--------------------

| File | Shows |
|------|-------|
| `pom.xml` | The only dependencies needed: `archidata` and an SLF4J backend (Logback). |
| `src/main/java/.../model/Task.java` | An entity extending `GenericDataSoftDelete` (Long id, timestamps, soft delete), Bean Validation constraints, a compound `@Index`. |
| `src/main/java/.../api/TaskResource.java` | CRUD with `DataAccess`, `@PermitAll` / `@RolesAllowed`, `@Valid` + `@ValidGroup(GroupCreate/GroupUpdate)`, 404 through `NotFoundException`. |
| `src/main/java/.../filter/SampleAuthenticationFilter.java` | The JWT filter: an `AuthenticationFilter` with the application name. |
| `src/main/java/.../migration/Initialization.java` | The first `MigrationStep`, seeding data with `addAction`. |
| `src/main/java/.../WebLauncher.java` | Production start-up: migrations, index synchronization, Jersey configuration (CORS from `CORS_ALLOWED_ORIGINS`, `SecurityHeadersFilter`), Grizzly server. Configuration from environment variables. |
| `src/main/java/.../WebLauncherLocal.java` | Development start-up: hard-coded configuration, local RSA key and generated tokens (no SSO, the JWT key updater is disabled), CORS limited to the local front (`http://localhost:4200`). Never deploy it: it logs a valid ADMIN token, and refuses to start on a non-loopback address. |
| `src/main/resources/logback.xml` | Logging at INFO (Logback defaults to DEBUG). |
| `src/test/java/.../SampleTest.java` | Infrastructure-free checks: access annotations, validation groups, token round trip. |
