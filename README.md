Generic backend for archidata in java
===================================




mvn install

// create a single package jar
mvn clean compile assembly:single



generic interface for all KAR web application



Somes tools:
============

Auto-update dependency:
-----------------------

auto-update to the last version dependency:

```bash
mvn versions:use-latest-versions
```

Format the code
---------------

Simply run the cmd-line:

```bash
mvn formatter:format
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

```
export PATH=/usr/lib/jvm/java-19-openjdk/bin:$PATH
mvn install
mvn deploy
```


