Getting Started Tutorial
========================

This tutorial walks you through building a simple REST API with archidata. By the end, you will have a running server with CRUD endpoints for a "Task" model, backed by MongoDB.


Prerequisites
-------------

- **Java 21+** — archidata requires at least Java 21 (25 recommended)
- **Maven 3.8+** — for building and dependency management
- **MongoDB 6+** — running locally on port 27017 with replica set enabled

### Setting up MongoDB with Docker

The easiest way to get a MongoDB instance with replica set:

```bash
docker run -d --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=root \
  -e MONGO_INITDB_ROOT_PASSWORD=base_db_password \
  mongo:8 --replSet rs0

# Initialize the replica set
docker exec -it mongodb mongosh -u root -p base_db_password --eval "rs.initiate()"
```

### Setting up Java

Make sure Java 21+ is available:

```bash
export PATH=$(ls -d --color=never /usr/lib/jvm/java-2*-openjdk)/bin:$PATH
java -version
```


Step 1: Project Setup
---------------------

Create a new Maven project with the following `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.example</groupId>
	<artifactId>my-app</artifactId>
	<version>1.0.0-SNAPSHOT</version>
	<packaging>jar</packaging>

	<properties>
		<maven.compiler.source>21</maven.compiler.source>
		<maven.compiler.target>21</maven.compiler.target>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.atria-soft</groupId>
			<artifactId>archidata</artifactId>
			<version>0.40.9-SNAPSHOT</version>
		</dependency>
	</dependencies>
</project>
```

Create the source directory structure:

```
my-app/
├── pom.xml
└── src/main/java/com/example/myapp/
    ├── WebLauncher.java
    ├── model/
    │   └── Task.java
    ├── api/
    │   └── TaskResource.java
    └── migration/
        └── Initialization.java
```


Step 2: Define the Data Model
-----------------------------

Create `src/main/java/com/example/myapp/model/Task.java`:

```java
package com.example.myapp.model;

import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task extends OIDGenericDataSoftDelete {
	@Column(nullable = false, length = 200)
	public String title;

	@Column(length = 0)
	public String description;

	@Column(nullable = false)
	public Boolean completed = false;
}
```

This model inherits:
- `oid` — auto-generated ObjectId primary key (stored as `_id` in MongoDB)
- `createdAt` / `updatedAt` — automatic timestamps
- `deleted` — soft delete flag

And adds three custom fields: `title`, `description`, and `completed`.


Step 3: Create the Migration
-----------------------------

Create `src/main/java/com/example/myapp/migration/Initialization.java`:

```java
package com.example.myapp.migration;

import org.atriasoft.archidata.migration.MigrationStep;

public class Initialization extends MigrationStep {
	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		// No initial data needed — collections are created automatically
		// You can add seed data here if needed:
		//
		// addAction((final DBAccessMongo da) -> {
		//     final Task task = new Task();
		//     task.title = "Welcome task";
		//     task.completed = false;
		//     da.insert(task);
		// });
	}
}
```

The migration engine tracks which migrations have been applied. On a fresh database, only the initialization runs. See [Migration](migration.md) for details.


Step 4: Create the REST Resource
---------------------------------

Create `src/main/java/com/example/myapp/api/TaskResource.java`:

```java
package com.example.myapp.api;

import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OrderItem;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.types.ObjectId;

import com.example.myapp.model.Task;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TaskResource {

	@GET
	@PermitAll
	public List<Task> list() throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.gets(Task.class,
			new OrderBy(new OrderItem("createdAt", Order.DESC)),
			new Limit(100)
		);
	}

	@GET
	@Path("/{id}")
	@PermitAll
	public Task get(@PathParam("id") final String id) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		final Task task = da.getById(Task.class, new ObjectId(id));
		if (task == null) {
			throw new NotFoundException("Task not found");
		}
		return task;
	}

	@POST
	@PermitAll
	public Task create(final Task task) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.insert(task);
	}

	@PUT
	@Path("/{id}")
	@PermitAll
	public Response update(
			@PathParam("id") final String id,
			final Task task) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		da.updateById(task, new ObjectId(id),
			new FilterValue("title", "description", "completed")
		);
		return Response.ok().build();
	}

	@DELETE
	@Path("/{id}")
	@PermitAll
	public Response delete(@PathParam("id") final String id) throws Exception {
		final DBAccessMongo da = DBAccessMongo.createInterface();
		da.deleteById(Task.class, new ObjectId(id));
		return Response.ok().build();
	}
}
```

> **Note:** This example uses `@PermitAll` for simplicity. In a real application, you would use `@RolesAllowed` with proper authentication. See [Security](security.md).


Step 5: Create the Launcher
----------------------------

Create `src/main/java/com/example/myapp/WebLauncher.java`:

```java
package com.example.myapp;

import java.net.URI;

import org.atriasoft.archidata.GlobalConfiguration;
import org.atriasoft.archidata.catcher.ExceptionCatcher;
import org.atriasoft.archidata.catcher.FailExceptionCatcher;
import org.atriasoft.archidata.catcher.InputExceptionCatcher;
import org.atriasoft.archidata.catcher.SystemExceptionCatcher;
import org.atriasoft.archidata.filter.CORSFilter;
import org.atriasoft.archidata.filter.OptionFilter;
import org.atriasoft.archidata.migration.MigrationEngine;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.myapp.api.TaskResource;
import com.example.myapp.migration.Initialization;

import jakarta.ws.rs.core.UriBuilder;

public class WebLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebLauncher.class);
	private HttpServer server = null;

	public WebLauncher() {
		// Configure database connection
		ConfigBaseVariable.bdDatabase = "my_app";
		ConfigBaseVariable.apiAdress = "http://0.0.0.0:9000/api/";
	}

	public void migrateDB() throws Exception {
		LOGGER.info("Running database migrations...");
		final MigrationEngine migrationEngine = new MigrationEngine();
		migrationEngine.setInit(new Initialization());
		// Add future migrations here:
		// migrationEngine.add(new Migration20240101());
		migrationEngine.migrateWaitAdmin(GlobalConfiguration.dbConfig);
		LOGGER.info("Database migrations complete.");
	}

	public void start() throws Exception {
		// Configure JAX-RS
		final ResourceConfig rc = new ResourceConfig();

		// Framework filters
		rc.register(OptionFilter.class);        // Handle OPTION requests
		rc.register(CORSFilter.class);          // CORS headers

		// Exception catchers (normalize error responses to JSON)
		rc.register(InputExceptionCatcher.class);
		rc.register(SystemExceptionCatcher.class);
		rc.register(FailExceptionCatcher.class);
		rc.register(ExceptionCatcher.class);

		// JSON serialization
		rc.register(JacksonFeature.class);

		// Application resources
		rc.register(TaskResource.class);

		// Start the HTTP server
		final URI baseUri = UriBuilder
			.fromUri(ConfigBaseVariable.getlocalAddress())
			.build();
		this.server = GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);

		// Graceful shutdown
		final HttpServer serverRef = this.server;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			LOGGER.info("Stopping server...");
			serverRef.shutdownNow();
		}));

		this.server.start();
		LOGGER.info("Server started at {}", baseUri);
	}

	public static void main(final String[] args) throws Exception {
		final WebLauncher launcher = new WebLauncher();
		launcher.migrateDB();
		launcher.start();
		Thread.currentThread().join();
	}
}
```


Step 6: Build and Run
---------------------

```bash
# Compile
mvn compile

# Run the application
mvn exec:java -Dexec.mainClass="com.example.myapp.WebLauncher"
```

The server starts at `http://localhost:9000/api/`.


Step 7: Test with curl
----------------------

### Create a task

```bash
curl -X POST http://localhost:9000/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Buy groceries", "description": "Milk, eggs, bread", "completed": false}'
```

Response:
```json
{
  "oid": "65a1b2c3d4e5f6a7b8c9d0e1",
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "completed": false
}
```

### List all tasks

```bash
curl http://localhost:9000/api/tasks
```

### Get a single task

```bash
curl http://localhost:9000/api/tasks/65a1b2c3d4e5f6a7b8c9d0e1
```

### Update a task

```bash
curl -X PUT http://localhost:9000/api/tasks/65a1b2c3d4e5f6a7b8c9d0e1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

### Delete a task (soft delete)

```bash
curl -X DELETE http://localhost:9000/api/tasks/65a1b2c3d4e5f6a7b8c9d0e1
```

The task is not physically removed — its `deleted` flag is set to `true` and it no longer appears in list queries.


Step 8: Add Authentication (Optional)
--------------------------------------

To secure your endpoints, register the `AuthenticationFilter` and replace `@PermitAll` with `@RolesAllowed`:

```java
// In WebLauncher.start():
rc.register(AuthenticationFilter.class);

// Initialize JWT keys:
JWTWrapper.initLocalToken("my-app-uuid");
```

Then in your resource:

```java
@POST
@RolesAllowed("myapp/tasks:w")
public Task create(final Task task, @Context final SecurityContext sc) throws Exception {
	// ...
}
```

See [Security & Authentication](security.md) for the complete guide.


Next Steps
----------

- [Data Model](data_model.md) — Learn about all available annotations and relationships
- [Database Access](database_access.md) — Advanced queries with conditions, ordering, and pagination
- [Migration](migration.md) — Manage database schema changes
- [Security](security.md) — JWT authentication and role-based permissions
- [Connection Management](connectionManagement.md) — Optimize database connections
