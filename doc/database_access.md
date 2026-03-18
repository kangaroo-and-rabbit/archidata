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
	new Condition(Filters.eq(Article::getTitle, "Hello World"))
);
```

Get all documents matching conditions:

```java
final List<Article> articles = da.gets(Article.class,
	new Condition(Filters.eq(Article::getStatus, "published")),
	OrderBy.desc(Article::getCreatedAt),
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
da.updateById(updateData, articleId, new FilterValue(Article::getTitle));
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
	new Condition(Filters.eq(Article::getStatus, "draft")),
	new FilterValue(Article::getStatus)
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
	new Condition(Filters.eq(Article::getStatus, "expired"))
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
	new Condition(Filters.eq(Article::getStatus, "published"))
);

final boolean exists = da.existsById(Article.class, someId);
```


Query Options
=============

All read, update, and delete methods accept `QueryOption...` as the last parameter. Options can be combined freely.

### Condition

Adds a WHERE clause to the query. Takes a `Bson` filter built with `Filters` (see [Filters Reference](#filters-reference)).

```java
new Condition(Filters.eq(Article::getStatus, "active"))
new Condition(Filters.gt(User::getAge, 18))
new Condition(Filters.and(
	Filters.eq(User::isActive, true),
	Filters.gt(User::getAge, 18)
))
```

### FilterValue

Specifies which fields to read or update. By default, all non-`@DataNotRead` fields are returned. Supports both string-based and type-safe method reference constructors.

```java
// String-based
da.gets(Article.class, new FilterValue("title", "status"));
da.updateById(data, id, new FilterValue("title", "content"));

// Type-safe with method references (recommended — resolves @Column annotations)
da.gets(Article.class, new FilterValue(Article::getTitle, Article::getStatus));
da.updateById(data, id, new FilterValue(Article::getTitle, Article::getContent));
```

### FilterOmit

The opposite of `FilterValue` — specifies which fields to exclude. Supports method references.

```java
// String-based
da.gets(Article.class, new FilterOmit("content"));

// Type-safe with method references (recommended)
da.gets(Article.class, new FilterOmit(Article::getContent));
```

### OrderBy

Sort results by one or more fields. Supports fluent factory methods with method references.

```java
// String-based
new OrderBy(new OrderItem("createdAt", Order.DESC))
new OrderBy(new OrderItem("status", Order.ASC), new OrderItem("title", Order.ASC))

// Type-safe fluent factory methods (recommended)
OrderBy.desc(Article::getCreatedAt)
OrderBy.asc(Article::getStatus)

// Type-safe OrderItem constructor
new OrderBy(new OrderItem(Article::getStatus, Order.ASC), new OrderItem(Article::getTitle, Order.ASC))
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


Type-Safe Utilities
===================

Archidata provides type-safe utilities that resolve method references to database field names, respecting `@Column(name)` annotations. This avoids string typos and provides compile-time safety.

### Fields — Field Name Resolution

`Fields` resolves getter or setter method references to their database field names:

```java
// Single field name
final String fieldName = Fields.of(User::getName);       // -> "name"
final String fieldName = Fields.of(User::getFullName);   // -> "full_name" (if @Column(name="full_name"))

// List of field names
final List<String> names = Fields.list(User::getName, User::getAge);  // -> ["name", "age"]
```

`Fields` can be used anywhere a field name string is needed.

### Type-Safe Filters with Filters

`Filters` is a drop-in replacement for `com.mongodb.client.model.Filters` that adds method reference support:

```java
// String-based (same as MongoDB Filters)
Filters.eq("name", "John")
Filters.gt("age", 18)

// Type-safe with method references (recommended)
Filters.eq(User::getName, "John")
Filters.gt(User::getAge, 18)
Filters.eq(User::isActive, true)

// Combine with logical operators
Filters.and(
	Filters.gt(User::getAge, 18),
	Filters.eq(User::isActive, true)
)
```

### Full Type-Safe Example

```java
final List<User> result = da.gets(User.class,
	new Limit(50),
	OrderBy.desc(User::getName),
	new Condition(Filters.and(
		Filters.exists(User::getEmail),
		Filters.or(
			Filters.in(User::getRole, "admin", "moderator", "editor"),
			Filters.gt(User::getScore, 100)
		)
	))
);

// Type-safe partial update
final User updateData = new User();
updateData.status = "archived";
da.update(updateData,
	new Condition(Filters.eq(User::getStatus, "draft")),
	new FilterValue(User::getStatus)
);
```


Filters Reference
=================

Conditions are built using `Filters` methods. All methods support both string-based and type-safe method reference variants.

### Comparison operators

```java
Filters.eq(User::getName, "John")          // name == "John"
Filters.ne(User::getStatus, "deleted")     // status != "deleted"
Filters.gt(User::getAge, 18)              // age > 18
Filters.gte(User::getAge, 18)             // age >= 18
Filters.lt(User::getAge, 65)              // age < 65
Filters.lte(User::getAge, 65)             // age <= 65
```

### Set membership

```java
Filters.in(User::getStatus, "draft", "review", "published")
Filters.nin(User::getCategory, "spam", "trash")
```

### Null / existence checks

```java
Filters.exists(User::getEmail)             // field exists
Filters.exists(User::getDeletedAt, false)  // field does not exist
```

### Logical operators

```java
Filters.and(
	Filters.eq(User::getStatus, "active"),
	Filters.gte(User::getAge, 18)
)

Filters.or(
	Filters.eq(User::getRole, "admin"),
	Filters.eq(User::getRole, "moderator")
)

Filters.not(Filters.eq(User::isActive, false))
```

### Regex

```java
Filters.regex(User::getEmail, ".*@example\\.com")
Filters.regex(User::getEmail, ".*@example\\.com", "i")  // case insensitive
```

### Array operators

```java
Filters.all(User::getTags, "java", "mongodb")
Filters.size(User::getTags, 3)
Filters.elemMatch(User::getTags, Filters.gt("value", 10))
```

### Full Example

```java
final List<User> result = da.gets(User.class,
	new Limit(50),
	OrderBy.desc(User::getName),
	new Condition(Filters.and(
		Filters.exists(User::getEmail),
		Filters.or(
			Filters.in(User::getRole, "admin", "moderator", "editor"),
			Filters.gt(User::getScore, 100)
		)
	))
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


Transactions
============

Archidata supports MongoDB multi-document transactions. See [Connection Management — Transactions](connectionManagement.md#transactions).
