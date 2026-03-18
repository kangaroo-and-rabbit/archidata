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
