Database Access
===============

Archidata provides a high-level API for MongoDB operations through `DBAccessMongo`. All CRUD operations use object introspection — you work with Java objects, not raw MongoDB documents.


Getting a Database Connection
-----------------------------

```java
// Default connection (uses ConfigBaseVariable settings)
final DBAccessMongo da = DBAccessMongo.createInterface();

// With explicit configuration
final DbConfig config = new DbConfig();
final DBAccessMongo da = DBAccessMongo.createInterface(config);

// Don't forget to close when done
da.close();
```

For connection reuse within a request, see [Connection Management](connectionManagement.md).


CRUD Operations
---------------

### Insert

```java
final Article article = new Article();
article.title = "Hello World";
article.content = "My first article";
final Article created = da.insert(article);
// created.oid is now set to the auto-generated ObjectId
```

Insert with a user-provided primary key:

```java
article.oid = new ObjectId("507f1f77bcf86cd799439011");
final Article created = da.insert(article, new DirectPrimaryKey());
```

Insert multiple documents:

```java
final List<Article> articles = List.of(article1, article2, article3);
final List<Article> created = da.insertMultiple(articles);
```

### Read

Get a single document by its primary key:

```java
final Article article = da.getById(Article.class, someObjectId);
```

Get the first document matching conditions:

```java
final Article article = da.get(Article.class,
	new Condition(new QueryCondition("title", "=", "Hello World"))
);
```

Get all documents matching conditions:

```java
final List<Article> articles = da.gets(Article.class,
	new Condition(new QueryCondition("status", "=", "published")),
	new OrderBy(new OrderItem("createdAt", Order.DESC)),
	new Limit(10)
);
```

Get all documents (use with caution on large collections):

```java
final List<Article> all = da.getAll(Article.class);
```

### Update

Update specific fields by ID:

```java
final Article updateData = new Article();
updateData.title = "Updated Title";
da.updateById(updateData, articleId, new FilterValue("title"));
```

Update all editable fields by ID:

```java
da.updateById(updatedArticle, articleId);
```

Update documents matching a condition:

```java
final Article updateData = new Article();
updateData.status = "archived";
da.update(updateData,
	new Condition(new QueryCondition("status", "=", "draft")),
	new FilterValue("status")
);
```

### Delete

Delete by ID (auto-detects soft delete vs hard delete based on the model):

```java
da.deleteById(Article.class, articleId);
```

Delete by condition:

```java
da.delete(Article.class,
	new Condition(new QueryCondition("status", "=", "expired"))
);
```

Explicit soft or hard delete:

```java
da.deleteSoftById(Article.class, articleId);  // sets deleted = true
da.deleteHardById(Article.class, articleId);  // removes from MongoDB
```

### Count and Exists

```java
final long count = da.count(Article.class,
	new Condition(new QueryCondition("status", "=", "published"))
);

final boolean exists = da.existsById(Article.class, someId);
```


Query Options
=============

All read, update, and delete methods accept `QueryOption...` as the last parameter. Options can be combined freely.

### Condition

Adds a WHERE clause to the query. Takes a `QueryItem` (see [Condition Models](#condition-models) below).

```java
new Condition(new QueryCondition("status", "=", "active"))
```

### FilterValue

Specifies which fields to read or update. By default, all non-`@DataNotRead` fields are returned.

```java
// Read only specific fields
da.gets(Article.class, new FilterValue("title", "status"));

// Update only specific fields
da.updateById(data, id, new FilterValue("title", "content"));
```

### FilterOmit

The opposite of `FilterValue` — specifies which fields to exclude:

```java
da.gets(Article.class, new FilterOmit("content"));
```

### OrderBy

Sort results by one or more fields:

```java
new OrderBy(new OrderItem("createdAt", Order.DESC))
new OrderBy(new OrderItem("status", Order.ASC), new OrderItem("title", Order.ASC))
```

### Limit

Limit the number of results:

```java
new Limit(50)
```

### ReadAllColumn

Include fields marked as `@DataNotRead` (such as `createdAt`, `updatedAt`, `deleted`):

```java
da.gets(Article.class, new ReadAllColumn())
```

### AccessDeletedItems

Include soft-deleted documents in query results:

```java
da.gets(Article.class, new AccessDeletedItems())
```

### DirectPrimaryKey

Allow inserting a document with a user-provided primary key (by default, inserting with a non-null primary key throws an exception):

```java
da.insert(article, new DirectPrimaryKey())
```

### DirectData

Insert or update data without auto-managing `_id`, `createdAt`, or `updatedAt`:

```java
da.insert(rawData, new DirectData())
```

### ForceReadOnlyField

Allow updating fields marked as `@ApiReadOnly`:

```java
da.updateById(data, id, new ForceReadOnlyField(), new FilterValue("oid"))
```

### OverrideTableName

Access a different MongoDB collection than the one defined by the model class:

```java
da.gets(Article.class, new OverrideTableName("articles_archive"))
```

### OptionRenameColumn

Rename a field in the query:

```java
new OptionRenameColumn("fieldName", "newFieldName")
```

### OptionSpecifyType

Specify the concrete type of a field declared as `Object`:

```java
new OptionSpecifyType("data", MyConcreteClass.class)
new OptionSpecifyType("items", ItemClass.class, true)  // is a list
```


Condition Models
================

Conditions are built using `QueryItem` objects, which can be combined into complex queries.

### QueryCondition — basic comparison

```java
new QueryCondition("fieldName", "=", value)
new QueryCondition("age", ">", 18)
new QueryCondition("status", "!=", "deleted")
```

Supported comparators: `=`, `!=`, `>`, `>=`, `<`, `<=`

### QueryInList / QueryNotInList — value in a set

```java
new QueryInList("status", "draft", "review", "published")
new QueryNotInList("category", List.of("spam", "trash"))
```

### QueryNull / QueryNotNull — null checks

```java
new QueryNull("deletedAt")
new QueryNotNull("email")
```

### QueryAnd / QueryOr — logical grouping

```java
new QueryAnd(
	new QueryCondition("status", "=", "active"),
	new QueryCondition("age", ">=", 18)
)

new QueryOr(
	new QueryCondition("role", "=", "admin"),
	new QueryCondition("role", "=", "moderator")
)
```

### Full Example

```java
final List<User> result = da.gets(User.class,
	new Limit(50),
	new OrderBy(new OrderItem("name", Order.DESC)),
	new Condition(
		new QueryAnd(
			new QueryNotNull("email"),
			new QueryOr(
				new QueryInList("role", "admin", "moderator", "editor"),
				new QueryCondition("score", ">", 100)
			)
		)
	)
);
```


Collection Management
=====================

```java
// List all collections
final List<String> collections = da.listCollections("prefix");

// Rename a collection
da.renameCollection("old_name", "new_name");

// Drop a database (destructive!)
da.deleteDatabase("database_name");
```
