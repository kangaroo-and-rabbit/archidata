Archi-data
==========

Archi-data is a framework that simplify:
  - Creating a REST server with:
    - Right control
    - Swagger display interface
    - Normalize error generate by the server
  - Access to the DB:
     - introspect Object and insert in the TD (SQLITE & MY-SQL)
     - Manage migration
  - JPA checker for many generic request
  - simplify the request of the Test-service
  
  
Develop in cmd-line:
--------------------

The first step is configuring your JAVA version (or select the JVM with the OS)

```bash
export PATH=$(ls -d --color=never /usr/lib/jvm/java-2*-openjdk)/bin:$PATH
```

Install the dependency:

```bash
mvn install
```

Run the test
```bash
mvn test
```

Install it for external use
```bash
mvn install
```

Develop With Eclipse:
--------------------

Import the project:
  - Open a (new) project on eclipse
  - `File` -> `Import`
    - `Maven` -> `Existing Maven project`
    - Select the `pom.xml` file and click on import

Run the Test:
  - Open the package `test.kar.archidata`
  - Click right on it
  - Select `Debug As` -> `JUnit Test`

Install in the local maven repository:
  - Click right on the `pom.xml` file
  - Select `Run As` -> `Maven install`


Somes tools:
============

Auto-update dependency:
-----------------------

Auto-update to the last version dependency:

```bash
mvn versions:use-latest-versions
```

Format the code
---------------

Simply run the cmd-line:

```bash
mvn formatter:format
```

Reformat XML file like the pom.xml

```bash
XMLLINT_INDENT="	" xmllint  --format "back/pom.xml" -o "back/pom.xml"
```

Enable the pre-commit checker
-----------------------------

```bash
./tools/configure_precommit.bash
```

> **_Note_**: You can change the code in `.git/hooks/pre-commit` by replacing `formatter:verify` with `formatter:format` to auto format the code @ every commit

Run Spot-bug:
------------

```bash
mvn spotbugs:check
```

Add Gitea in the dependency for the registry:
=============================================

Read instruction for tocken in ~/.m2/setting.xml

edit file: ```~/.m2/settings.xml``` 

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

release:
========

```bash
export PATH=$(ls -d --color=never /usr/lib/jvm/java-2*-openjdk)/bin:$PATH
mvn install
mvn deploy
```


