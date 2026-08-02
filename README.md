Archi-data
==========

Archi-data is a Java framework for building REST servers backed by MongoDB. It provides:

- **REST server** with Jakarta JAX-RS (Grizzly/Jersey):
  - Role-based access control (JWT + API keys)
  - Built-in OpenAPI 3.0.3 specification generation
  - Normalized error responses
  - TypeScript / Python client generation
- **MongoDB data access** layer:
  - Object introspection and automatic CRUD operations
  - Support for ObjectId, Long, and UUID primary keys
  - Soft delete, timestamps, and relationship management
  - Database migration engine
  - Backup/restore engine and incremental change journal
- **JPA-style annotations** for model definition and validation
- **Test utilities** for integration testing

For complete documentation, see [doc/index.md](doc/index.md).


Prerequisites
-------------

- **Java 21+** (tested with Java 25)
- **Maven 3.8+**
- **MongoDB 6+** (replica set required for transactions and change streams)


Develop in cmd-line
-------------------

Configure your Java version (or select the JVM with the OS):

```bash
export PATH=$(ls -d --color=never /usr/lib/jvm/java-2*-openjdk)/bin:$PATH
```

Install the dependencies:

```bash
mvn install
```

Run the tests:

```bash
mvn test
```

Install for external use:

```bash
mvn install
```


Develop with Eclipse
--------------------

Import the project:
  - Open a (new) project on Eclipse
  - `File` -> `Import`
    - `Maven` -> `Existing Maven project`
    - Select the `pom.xml` file and click on import

Run the tests:
  - Open the test package
  - Click right on it
  - Select `Debug As` -> `JUnit Test`

Install in the local Maven repository:
  - Click right on the `pom.xml` file
  - Select `Run As` -> `Maven install`


Tools
=====

Auto-update dependencies
------------------------

```bash
mvn versions:use-latest-versions
```

Format the code
---------------

```bash
mvn formatter:format
```

Reformat XML files like `pom.xml`:

```bash
XMLLINT_INDENT="	" xmllint --format "back/pom.xml" -o "back/pom.xml"
```

Enable the pre-commit checker
-----------------------------

```bash
./tools/configure_precommit.bash
```

> **Note:** You can change the code in `.git/hooks/pre-commit` by replacing `formatter:verify` with `formatter:format` to auto-format the code at every commit.

Run SpotBugs
------------

```bash
mvn spotbugs:check
```


Release
=======

`./deliver` releases the repository from `develop` to `main`, driven by `version.txt`.

```bash
./deliver           # prepare the release (interactive)
./deliver push      # publish branches and tags
./deliver revert    # undo a release that was not pushed
./deliver status    # where the repository stands
```

A release lists every commit made since the last tag, then asks which part of the version it bumps:

```
== Commits since v0.48.0 (12)
    9b671d9  [FEAT] (journal) periodic incremental journal of the modified documents  (Edouard DUPIN)
    bd111eb  [FEAT] (dataAccess) push FilterValue/FilterOmit down to the projection   (Edouard DUPIN)
    ...
== Current version: 0.48.1-dev
    (1) major   -> 1.0.0     (change API)
    (2) medium  -> 0.49.0    (add feature)
    (3) minor   -> 0.48.2    (bug fix & doc)
    (q) quit, release nothing
```

It then fast-forwards `main` onto `develop` — never a merge commit when the history allows a
fast-forward — writes `version.txt`, runs `.island/release.bash` (which propagates the version to
`pom.xml` and the dependencies), commits `[RELEASE] Release vX.Y.Z`, tags it, and reopens the next
`-dev` version on `develop`.

Nothing is pushed until `./deliver push`. Until then `./deliver revert` puts both branches and the
tag back exactly where they were — it refuses once the release is published, since rewriting a
shared history is not something to do by accident.

Useful options: `-n` (dry-run, changes nothing), `--level major|medium|minor` (skip the question),
`--from` / `--to` (other branches than `develop` / `main`), `--remote` (default `origin`).


Gitea Registry
==============

Read instructions for token in `~/.m2/settings.xml`.

Edit file: `~/.m2/settings.xml`

```xml
<settings>
  <servers>
    <server>
      <id>gitea</id>
      <configuration>
        <httpHeaders>
          <property>
            <name>Authorization</name>
            <value>token xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx</value>
          </property>
        </httpHeaders>
      </configuration>
    </server>
  </servers>
</settings>
```


Release
=======

```bash
export PATH=$(ls -d --color=never /usr/lib/jvm/java-2*-openjdk)/bin:$PATH
mvn install
mvn deploy
```
