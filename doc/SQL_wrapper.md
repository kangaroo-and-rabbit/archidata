Managing relation over the Mongo DB interface
====================


Generic Access:
---------------

Some generic function are available to permit you to simply request the DB

### Create a new Value:

```java
MyDataModel dataToInsert = ...;
MyDataModel dataCreated = DataAccess.insert(dataToInsert);
```


### Get a full table:

```java
List<MyDataModel> data = DataAccess.gets(MyDataModel.class);
```

### Get a single element in the DB:

```java
UUID id = ...;
MyDataModel data = DataAccess.get(MyDataModel.class, id);
```

> **_Note:_** The Id type fully managed are UUID and Long

### Removing the Data:

```java
UUID id = ...;
DataAccess.delete(MyDataModel.class, id);
```

> **_Note:_** Removing the data automatically detect if it is a software remove or definitive remove

### Updating the Data:

The update of the data can be managed by 2 way:
  - Direct update of the Object with direct injection (Good for `PUT`)
  - Update with input json (Good for `PATCH`)

The second way is preferable for some reasons
  - When jakarta transform the data in you object, we can not detect the element set at null or not set (We consider user of `Optional` il all data class will create a too bug amount of unneeded code in all dev parts)
  - Throw in the jakarta parsing are not well catch when we will generate generic error
  - The Check will permit to explain what is wrong better than a generic Json parser.

Updating with JSON:

```java
UUID id = ...;
String jsonRequest = "{...}";
DataAccess.updateWithJson(MyDataModel.class, id, jsonRequest);
```

Updating with direct data:

```java
UUID id = ...;
MyDataModel dataToUpdate = ...;
// This update all fields:
DataAccess.update(dataToUpdate, id);
// Select the field to update:
DataAccess.update(dataToUpdate, id, List.of("field1","field2"));
```

Generic option of the request:
------------------------------

Many API have a generic multiple option available like:

```java
public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... option) throws Exception
```

You just need to add your options in the list of `option`.

Filter the list of field read:
```java
public FilterValue(final String... filterValue)
public FilterValue(final List<String> filterValues)
// example:
new newFilterValue("field1", "field2");
```

Add a condition [more detail](#condition-models)
```java
public Condition(final QueryItem items)
```

Order the request:
```java
public OrderBy(final OrderItem... childs);
// example:
new OrderBy(new OrderItem("name", Order.DESC));
```

Limit the :
```java
public Limit(final long limit)
// example:
new Limit(50);
```

Read all column like update date and create date or delete field
```java
public ReadAllColumn()
```

Condition models:
-----------------

Creating a condition independent of the DB model use need to have a little abstraction of the query model:

For this we propose some condition that update with the internal "auto" condition that is added (like the soft delete...)

### default generic comparator

This is the base of the comparison tool. It compare a column with a value

```java
public QueryCondition(final String key, final String comparator, final Object value);
```

Simple DB comparison element. Note the injected object is injected in the statement and not in the query directly.

Example:
```java
String nameToCheck = "plop";
new QueryCondition("fieldName", "=", nameToCheck);
// OR:
UUID uuid = ...;
new QueryCondition("uuid", "=", uuid);
```

### List comparator

It permit to check a column is (NOT) in a list of value

```java
public QueryInList(final String key, final List<T> value)
public QueryInList(final String key, final T... value)
```
and his opposite:
```java
public QueryNotInList(final String key, final List<T> value)
public QueryNotInList(final String key, final T... value)
```

example
```java
new QueryInList("uuid", List.of(uuid1, uuid2));
```

### NULL and NOT NULL checker

This permit to check an element is `NULL` or `not NULL`

```java
public QueryNull(final String key);
public QueryNotNull(final String key);
```

### The group condition:

The generic `OR` group:
```java
public QueryOr(final List<QueryItem> child);
public QueryOr(final QueryItem... child);
```

Or the generic `AND group:
```java
public QueryAnd(final List<QueryItem> child);
public QueryAnd(final QueryItem... child);
```

### Full example:

```java
List<MyDataModel> result = DataAccess.getsWhere(MyDataModel.class, 
        new Limit(50),
        new OrderBy(new OrderItem("name", Order.DESC)),
        new Condition(
            new QueryAnd(
                QueryNull("Field3")
                new QueryOr(
                    new QueryInList("Field4", 5, 55, 65, 62, 27, 84),
                    new QueryCondition("cityID", ">", 78000);
                )
            )
        )
    );
```


