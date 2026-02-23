Migration
=========

Archidata includes a forward-only migration engine that tracks database schema changes. Each migration is a named step with a sequence of actions. The engine records which migrations have been applied in a `Migration` collection, so it only runs new ones.

> **Note:** Reverse migration is not supported. Migrations run forward only.


How it Works
------------

1. On startup, `MigrationEngine` checks the `Migration` collection to see which migrations have already been applied
2. If the database is empty (no migration records), it runs the **initialization** migration
3. Then it runs all subsequent migrations in order
4. Each migration step is recorded in the database with logs and status


MigrationEngine
---------------

The engine manages the list of migrations and executes them in order.

```java
final MigrationEngine migrationEngine = new MigrationEngine();

// Set the initial migration (runs only on a fresh database)
migrationEngine.setInit(new Initialization());

// Add subsequent migrations in order
migrationEngine.add(new Migration20240101());
migrationEngine.add(new Migration20240215());
migrationEngine.add(new Migration20240301());

// Run all pending migrations
// If a migration fails, the server waits for admin intervention
migrationEngine.migrateWaitAdmin(GlobalConfiguration.dbConfig);
```

Key methods:

| Method | Description |
|--------|-------------|
| `setInit(migration)` | Set the initialization migration for fresh databases |
| `add(migration)` | Add a migration step (order matters) |
| `migrateWaitAdmin(config)` | Run pending migrations; block on failure |
| `getCurrentVersion(da)` | Get the last applied migration record |


MigrationStep
-------------

Each migration extends `MigrationStep` and implements `generateStep()` to define its actions.

```java
public class Initialization extends MigrationStep {

	@Override
	public String getName() {
		return "Initialization";
	}

	@Override
	public void generateStep() throws Exception {
		// Each addAction() defines one atomic operation
		addAction((final DBAccessMongo da) -> {
			// Create initial data, indexes, etc.
		});
	}
}
```

### addAction()

`addAction()` takes a lambda `(DBAccessMongo da) -> { ... }` that receives a database connection. Each action is executed sequentially, and progress is logged to the `Migration` collection.

```java
@Override
public void generateStep() throws Exception {
	// Action 1: Create an admin user
	addAction((final DBAccessMongo da) -> {
		final User admin = new User();
		admin.login = "admin";
		admin.email = "admin@example.com";
		admin.role = "admin";
		da.insert(admin);
	});

	// Action 2: Create default categories
	addAction((final DBAccessMongo da) -> {
		for (final String name : List.of("General", "Technology", "Science")) {
			final Category cat = new Category();
			cat.name = name;
			da.insert(cat);
		}
	});
}
```


Migration Examples
------------------

### Adding data in a migration

```java
public class Migration20240101 extends MigrationStep {

	@Override
	public String getName() {
		return "Migration20240101-AddDefaultRoles";
	}

	@Override
	public void generateStep() throws Exception {
		addAction((final DBAccessMongo da) -> {
			final Role role = new Role();
			role.name = "editor";
			role.description = "Can edit articles";
			da.insert(role);
		});
	}
}
```

### Updating existing documents

```java
addAction((final DBAccessMongo da) -> {
	// Set a default value for all existing users that don't have a status
	final List<User> users = da.gets(User.class,
		new Condition(new QueryNull("status"))
	);
	for (final User user : users) {
		final User update = new User();
		update.status = "active";
		da.updateById(update, user.oid, new FilterValue("status"));
	}
});
```

### Renaming a collection

```java
addAction((final DBAccessMongo da) -> {
	da.renameCollection("old_collection_name", "new_collection_name");
});
```


Best Practices
--------------

1. **Name migrations with a date prefix** to keep them ordered: `Migration20240101`, `Migration20240215`
2. **Keep actions small and atomic** — one logical change per `addAction()` call
3. **Always add new migrations at the end** — never modify or reorder existing migrations
4. **Test migrations** on a copy of your production database before deploying
5. **The initialization migration** should create all baseline data that the application expects
