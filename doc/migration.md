Migration
=========

Archidata includes a forward-only migration engine. Each migration is a named step made of a
sequence of actions. The engine records every migration it runs in the `KAR_migration`
collection, so that each one runs only once.

The working example is [`sample/archidata/basic`](../sample/archidata/basic): `WebLauncher.migrateDB()`
and `migration/Initialization.java`.

> **Note:** Reverse migration is not supported. `MigrationInterface` has no reverse operation:
> `MigrationEngine.revertSingle()` always throws `UnsupportedOperationException`, and so does
> `revertTo()` as soon as one migration would have to be reverted. Write a new forward migration
> instead.


How it works
------------

On start-up, `MigrationEngine` reads the last record of `KAR_migration`:

1. **Empty database** (no record): it runs the **initialization** step only, then writes a
   placeholder record carrying the name of the **last** registered migration. Every registered
   migration is therefore considered applied: the initialization must build the state of the
   *current* model. (Without an initialization step, all the migrations run in order.)
2. **Last record not terminated** (a previous run failed in the middle): it refuses to go on and
   raises a `MigrationException` naming the migration and the failing action.
3. **Otherwise**: it runs, in order, every migration registered *after* the one named in the
   last record.

Each run inserts a record (`name`, `stepId`, `count`, `terminated`, `logs`) and updates `stepId`
and `logs` after every action, so a failure says which action broke: the `logs` array of the
failed record holds the `DB request ERROR: ...` message of the failing action.

Matching is done **by name** (`getName()`). If the last recorded name is neither the
initialization nor any registered migration (a migration renamed or removed), the engine refuses
to go on and raises a `MigrationException` naming the unknown migration (`migrateWaitAdmin` then
blocks, so the server does not start). Applying nothing would run the application on a database of
unknown state, and applying everything would replay migrations already done. Never rename or
remove a migration once it has been deployed; if it happened, restore its former `getName()`.


MigrationEngine
---------------

```java
public void migrateDB() throws Exception {
	final MigrationEngine migrationEngine = new MigrationEngine();
	// Runs only on an empty database
	migrationEngine.setInit(new Initialization());
	// Later migrations, in order; append only
	migrationEngine.add(new Migration20261101());
	migrationEngine.add(new Migration20261215());
	// new DbConfig() reads the DB_* configuration from ConfigBaseVariable
	migrationEngine.migrateWaitAdmin(new DbConfig());
}
```

| Method | Description |
|--------|-------------|
| `setInit(migration)` | Initialization step, used only on an empty database. |
| `add(migration)` | Append a migration (order matters). Two migrations with the same name, or a migration with the name of the initialization, are rejected. |
| `migrateWaitAdmin(DbConfig)` | Run the pending migrations. On failure, log the error and **block forever** (the server does not start on a half-migrated database; an administrator must fix it and restart). |
| `migrateErrorThrow(DbConfig)` | Same, but throw a `MigrationException` on failure (tests, tools). |
| `getCurrentVersion(DBAccessMongo)` | Last record of `KAR_migration`, or `null` when there is none. |

### Database configuration

`DbConfig` describes the MongoDB connection:

```java
// From ConfigBaseVariable, i.e. the DB_HOST, DB_PORT, DB_USER, DB_PASSWORD, DB_DATABASE
// environment variables or the matching setters (setDbHost, setDbPassword, setBdDatabase...)
final DbConfig config = new DbConfig();

// Explicit values: hostname, port, login, password, database name, keep connected, classes
final DbConfig other = new DbConfig("localhost", (short) 27017, "root", "secret", "my_db", false, List.of());
```

`DB_PASSWORD` has no default: `new DbConfig()` throws an `IllegalStateException` when it is not set.

Run the migrations **before** `IndexEngine.synchronize()` (they may create or rename collections)
and before starting the HTTP server. See [Indexes](indexes.md).


MigrationStep
-------------

A migration extends `MigrationStep` and declares its actions in `generateStep()`:

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

- `getName()` is the identity stored in `KAR_migration`. The default is the canonical class name:
  override it with a stable string, and never change it once deployed.
- `addAction(AsyncCall)` takes a lambda `(DBAccessMongo da) -> { ... }`. Actions run in order,
  progress is recorded after each one; an exception stops the migration and leaves the record
  not terminated.
- Collections need no creation step: MongoDB creates them on the first insert.
- `MigrationSqlStep` is a deprecated alias of `MigrationStep`.


Examples
--------

### Adding data

```java
public class Migration20261101 extends MigrationStep {

	@Override
	public String getName() {
		return "Migration20261101-default-task";
	}

	@Override
	public void generateStep() throws Exception {
		addAction((final DBAccessMongo da) -> {
			da.insert(new Task("Check the backups", null));
		});
	}
}
```

### Filling a new field on existing documents

```java
addAction((final DBAccessMongo da) -> {
	final Task update = new Task();
	update.setDone(Boolean.FALSE);
	// Only the "done" field is written, on the documents that do not have it yet
	da.update(update, new Condition(Filters.exists("done", false)), new FilterValue("done"));
});
```

### Renaming a collection

```java
addAction((final DBAccessMongo da) -> {
	da.renameCollection("OldName", "NewName");
});
```


Best practices
--------------

1. **Prefix names with a date** (`Migration20261101`) and give them a stable `getName()`.
2. **Keep actions small**: one logical change per `addAction()`, so a failure points to it.
3. **Append only**: never modify, reorder, rename or remove a deployed migration.
4. **Keep the initialization up to date**: when a migration changes the model, the initialization
   must produce the same final state on an empty database.
5. **Test on a copy** of the production database before deploying.
