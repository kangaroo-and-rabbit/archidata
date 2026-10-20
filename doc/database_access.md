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

Without `FilterValue`, `updateById` writes every editable field (`FilterValue.getEditableFieldsNames`),
and a `null` value removes the field (`$unset`, unless it has an `@InitValue`). The editable fields
exclude the system-managed ones (primary key, timestamps, deleted marker), the `@ApiReadOnly` ones
(see [ForceReadOnlyField](#forcereadonlyfield)) and the **protected** ones: `@DataNotRead` and
`@DataEncrypt`. A standard read leaves a protected field `null` (not projected, or not decrypted), so a
read-then-update would erase it; a full update therefore never touches it. To write a protected field,
name it in a `FilterValue`:

```java
da.updateById(user, userId, new FilterValue("passwordHash"));
```

The same rule protects the hidden fields of **embedded objects** (a standard read leaves their
`@DataNotRead` / `@DataEncrypt` fields `null` too). In a full update (no `FilterValue`, or
`FilterValue.getEditableFieldsNames`), a field whose type holds such fields at any depth is written as
follows:

| Field type | Full update |
|---|---|
| embedded object (POJO, record) | each visible sub-field in dot notation (`credentials.user`, `null` = `$unset`), the protected ones left as stored; recursively for a nested embedded object |
| embedded object set to `null` | `$unset` of the whole object (an explicit `null` deletes it, hidden fields included) |
| `List`, `Set` or `Map` of such objects | **not written** (kept as stored) |

To rewrite such a field as a whole (new list, hidden values included), name it in a `FilterValue`:
`da.updateById(account, id, new FilterValue("history"))`. An embedded object that is not stored yet is
created by its first non-null visible field; an object stored as a BSON `null` (written outside
archidata) makes the dot-notation update fail.

Update documents matching a condition:

```java
final Article updateData = new Article();
updateData.status = "archived";
da.update(updateData,
	new Condition(Filters.eq(Article::getStatus, "draft")),
	new FilterValue(Article::getStatus)
);
```

#### Scoped update and relation side effects

A `Condition` passed to `updateById` is ANDed with the id, which scopes the write (an owner filter, for
example):

```java
da.updateById(body, parentId, new Condition(Filters.eq("ownerId", currentUserId)));
```

Writing a relation field also changes other documents: `@OneToManyDoc` children removed from the list are
deleted or unlinked (`cascadeUpdate`), the ids added are re-parented, and `@ManyToManyDoc` /
`@ManyToOneDoc` links are added to or removed from the remote documents. These **side effects follow the
filter of the update**: the stored document they are computed from is read with the caller's `Condition`,
the id and the soft-delete exclusion, and they run only when the update matched a document. When the
condition matches nothing, or the document is soft-deleted (without `AccessDeletedItems`), the update returns
`0` and no remote document changes.

The side effects are separate writes made after the main update. Inside a `TransactionContext` (see
[Connection Management](connectionManagement.md)) they belong to its transaction; without one, a concurrent
write between the read of the stored document and the update can leave the remote links out of date (a WARN
is logged when that is detected). archidata does not check that the caller may modify the documents a
relation field lists: validate the ids of a client body before the update.

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

Whitelist of top-level fields. On a read it becomes the **MongoDB projection**; on an update it selects the fields to write. By default, all non-`@DataNotRead` fields are read. Supports both string-based and type-safe method reference constructors.

```java
// String-based
da.gets(Article.class, new FilterValue("title", "status"));
da.updateById(data, id, new FilterValue("title", "content"));

// Type-safe with method references (recommended — resolves @Field annotations)
da.gets(Article.class, new FilterValue(Article::getTitle, Article::getStatus));
da.updateById(data, id, new FilterValue(Article::getTitle, Article::getContent));
```

### FilterOmit

The opposite of `FilterValue` — blacklist of top-level fields, on reads as well as on updates. Supports method references.

```java
// String-based
da.gets(Article.class, new FilterOmit("content"));

// Type-safe with method references (recommended)
da.gets(Article.class, new FilterOmit(Article::getContent));
```

### Restricting what a read transfers

`FilterValue` / `FilterOmit` are pushed down to MongoDB as a projection: an unselected field is
**never transferred** and never mapped. Use it to leave a heavy field (a long text, a blob, a large
list) in the database when the query does not need it.

```java
// Only title and status travel over the wire:
da.gets(Article.class, new FilterValue(Article::getTitle, Article::getStatus));

// Everything but the heavy body:
da.gets(Article.class, new FilterOmit(Article::getContent));
```

Rules:

- **The primary key is always read**, even under a `FilterOmit` — an entity without its identifier
  could not be updated, deleted, nor linked afterwards.
- **An unknown field name raises a `DataAccessException`.** A typo in a whitelist would otherwise
  read nothing at all, which is much harder to diagnose than an exception.
- **Excluding a relation field also drops its queries.** A `@ManyToOneDoc` / `@OneToManyDoc` field
  that is expanded into its target entity costs an extra query per read; omitting it skips the
  resolution entirely.
- **Field names are the structural ones** — the name used by `@Field("...")` when present,
  which is exactly what the method references resolve to, and the same names updates already use.
- `@DataNotRead` fields (typically `createdAt` / `updatedAt`) stay excluded; `ReadAllColumn` lifts
  that exclusion but does **not** defeat an explicit `FilterValue` whitelist.
- Only one `FilterValue` and one `FilterOmit` per query — a second one raises a `DataAccessException`
  rather than silently picking a winner. When both are given, the omit applies on top of the include.
- The restriction applies to the **top level only**: the entities loaded behind a relation are read
  in full, with their own options.

```java
// Left at their default value (null / 0), not fetched:
final Article light = da.getById(Article.class, id, new FilterValue(Article::getTitle));
light.getTitle();    // "Hello"
light.getContent();  // null — never transferred
light.getOid();      // present anyway
```

#### Reaching inside an embedded sub-document

A `FilterValue` entry may be a dotted path. It is pushed to MongoDB as-is, so only that part of the
sub-document is transferred and the top-level field is rebuilt partially:

```java
final Article light = da.getById(Article.class, id,
		new FilterValue("author.name", "author.avatar.small"));

light.getAuthor().getName();          // "Alice"
light.getAuthor().getEmail();         // null — never transferred
light.getAuthor().getAvatar().getSmall();  // present
```

- Naming both a field and one of its sub-paths reads the **whole** field — the widest wins
  (`new FilterValue("author", "author.name")` reads all of `author`).
- Paths can go as deep as the document does (`a.b.c`).
- The prefix must be a real field of the entity: `new FilterValue("authr.name")` raises a
  `DataAccessException`.
- **Dotted paths are rejected in a `FilterOmit`**: the projection archidata builds is an inclusion,
  and MongoDB does not allow mixing inclusions and exclusions in a single projection. List what to
  keep with a `FilterValue` instead.
- This works on **embedded** sub-documents (an object, a list or a map of objects stored inside the
  document). A `@ManyToOneDoc` relation is *not* embedded — its target lives in another collection,
  so `"parent.name"` does not restrict the resolution query; the useful lever there is to omit the
  relation entirely, which drops that query.

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

The value must be in `1..Limit.getMaxValue()` (10 000 by default, `Limit.setMaxValue(long)` to change
it, up to `Integer.MAX_VALUE`), otherwise the constructor throws `IllegalArgumentException`: MongoDB
reads `0` as "no limit" and a negative value as "one batch". `Skip` accepts `0..Integer.MAX_VALUE`
(`Skip.MAX_VALUE`); neither value is truncated to an `int` any more. Clamp a value that comes from a
client before building the option (`Math.clamp(value, 1, max)`).

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

### AllowMassOperation

A write (`update`, `delete`, `deleteHard`, `deleteSoft`, `restore`, `updateBsonDocuments`) whose condition
is missing or empty (`new Condition()`, `new Condition(Filters.empty())`, a filter built conditionally that
stayed empty) is refused with a `DataAccessException`. Pass `AllowMassOperation` to target the whole
collection on purpose:

```java
da.updateBsonDocuments("article", new Document("$unset", new Document("legacy", "")), new AllowMassOperation());
```

An empty `Condition` combined with a real one is ignored (the real one applies). Reads are not concerned.

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

Archidata provides type-safe utilities that resolve method references to database field names, respecting `@Field("...")` annotations. This avoids string typos and provides compile-time safety.

### Fields — Field Name Resolution

`Fields` resolves getter or setter method references to their database field names:

```java
// Single field name
final String fieldName = Fields.of(User::getName);       // -> "name"
final String fieldName = Fields.of(User::getFullName);   // -> "full_name" (if @Field("full_name"))

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

Never pass a client value as a regex pattern (ReDoS, unexpected matches), nor build a
`Filters.where` expression from one (it is JavaScript run by MongoDB). To search a client text, use
`Filters.regexLiteral`, which quotes it with `Pattern.quote`:

```java
Filters.regexLiteral("title", userText)        // "a.b" only matches "a.b"
Filters.regexLiteral("title", userText, "i")   // case insensitive
```

### Values and field names from a client

- `Filters.eq` always produces an explicit `{field: {$eq: value}}`: a `Map` or a `Document` parsed from
  an untyped client JSON is compared as a literal, never read as operators (`{"$ne": null}`...).
- Every string field name given to `Filters` or to `new OrderItem(String, Order)` is checked: no leading
  `$` (`$where`, `$expr`), no NUL character (`IllegalArgumentException`).
- A field name chosen by a client (dynamic filter, sort column) must be checked against the model:
  `Filters.checkFieldName(Article.class, name)` or `OrderItem.ofClientField(Article.class, name, order)`.
  Every segment of a dotted name is resolved: each one must be a field of the entity, or of the embedded
  class reached so far, and none may be `@DataNotRead` / `@DataEncrypt` (filtering or sorting on a hidden
  field reveals its value by dichotomy). That covers the fields of embedded objects
  (`credentials.passwordHash`), of list and set elements (`history.passwordHash`, `history.0.passwordHash`:
  after a list an optional numeric segment is an array index) and of map values (`map.someKey.secret`:
  after a map the next segment is a key, any string). A name that ends on an embedded object, a list, a
  set or a map of objects whose class holds protected fields (`credentials`, `history`) is refused too:
  comparing or sorting the whole sub-document would reach them. The rest of the path is accepted without
  check below an explicit free-form position (`org.bson.Document`, `JsonNode`, a plain value,
  a concrete JDK class) and below a relation field. Refused: a path too deep (more than 64 segments), ambiguous (a
  numeric segment after a list of maps whose values hold protected fields), and a position declared as
  `Object`, as an interface or as an abstract class (JDK ones such as `Serializable` or `Number` included),
  as a whole or below it, and a raw (non-generic) `List`, `Set` or `Map`, whose elements are written by
  their own class: such a
  value is written by the class of the value (protected fields included) and read back by the declared
  type, so the guard cannot know what it holds (use a modeled class, or `org.bson.Document`, for free-form
  data). Protection follows the declared type: with a concrete base class holding a subclass instance, a
  field added by the subclass is refused as unknown, but comparing the whole base-class sub-document is
  not detected as reaching it. A sub-document that holds such a position at any depth
  (`Wrapper { Object inner; }`) is refused as a whole too.

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

## Logs of the data layer

At TRACE level, `org.atriasoft.archidata.dataAccess.DBAccessMongo` logs the filter of each read and
every document received. The values of the protected fields (`@DataNotRead`, `@DataEncrypt`) are
replaced by `***` where they stand, by position and not by name: the document is walked with the model, so
the protected fields of embedded objects, of list and set elements and of map values are masked, and so is
a dotted filter key designating one (`credentials.passwordHash`, `history.0.passwordHash`,
`map.someKey.secret`), inside `$and`, `$or`, `$elemMatch` and operator documents. A column renamed by
`OptionRenameColumn` is masked under its new name. Below the root, a key the model does not know (a
field added by a subclass of the declared type, for example) is masked too. The JSON rendering only runs when TRACE is enabled. The other values (e-mails, tokens compared by a filter...) are logged as they are:
never enable TRACE on this logger in production.
