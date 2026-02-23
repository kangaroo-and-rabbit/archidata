Data Model
==========

Archidata uses annotated Java classes to define your data models. The framework introspects these classes at runtime to automatically generate MongoDB operations (insert, query, update, delete) without manual mapping.

There are two aspects to the data model:
- **DB model**: How data is stored in MongoDB (primary key, timestamps, soft delete)
- **API model**: How data is exposed via REST (read-only fields, validation, generated TypeScript types)


Base Model Classes
==================

Archidata provides base classes that add common fields (primary key, timestamps, soft delete). Choose the one that matches your needs.

### Hierarchy overview

```
GenericTiming                      (createdAt, updatedAt)
├── GenericData                    (+ Long id)
│   └── GenericDataSoftDelete      (+ Boolean deleted)
├── OIDGenericData                 (+ ObjectId oid)
│   └── OIDGenericDataSoftDelete   (+ Boolean deleted)
└── UUIDGenericData                (+ UUID uuid)
    └── UUIDGenericDataSoftDelete  (+ Boolean deleted)
```

### OIDGenericDataSoftDelete (recommended for MongoDB)

The most common choice. Provides an ObjectId primary key, creation/update timestamps, and soft delete:

```java
public class Article extends OIDGenericDataSoftDelete {
	@Column(nullable = false, length = 200)
	public String title;
	@Column(length = 0)
	public String content;
}
```

Inherited fields:
- `oid` (ObjectId) — auto-generated primary key, mapped to `_id` in MongoDB
- `createdAt` (Date) — set automatically on insert
- `updatedAt` (Date) — set automatically on insert and update
- `deleted` (Boolean) — soft delete flag (default: false)

> **Note:** `createdAt`, `updatedAt`, and `deleted` are excluded from default queries (marked `@DataNotRead`). Use the `ReadAllColumn` query option to include them.

### OIDGenericData

Same as above but without soft delete. Calling `deleteById()` performs a hard delete:

```java
public class LogEntry extends OIDGenericData {
	@Column(nullable = false)
	public String message;
	@Column(nullable = false)
	public String level;
}
```

### GenericData / GenericDataSoftDelete

Uses a Long primary key with auto-increment (via a MongoDB `counters` collection):

```java
public class Counter extends GenericDataSoftDelete {
	@Column(nullable = false, length = 100)
	public String name;
	public int value;
}
```

Inherited field: `id` (Long) — auto-generated sequential primary key.

### Inline Models (no inheritance)

For simple cases or tests, you can define models without inheriting from a base class:

```java
public class SimpleModel {
	@Id
	public ObjectId _id = null;
	public String data;
}
```

This gives you just a primary key with no timestamps or soft delete.


Field Annotations
=================

### @Id

Marks the primary key field. Required on exactly one field per model.

```java
@Id
public ObjectId _id = null;
```

### @Column

Controls how a field is stored in MongoDB.

| Parameter    | Default | Description |
|-------------|---------|-------------|
| `nullable`  | `true`  | Whether the field can be null |
| `unique`    | `false` | Whether the value must be unique across all documents |
| `length`    | `255`   | Max string length. Use `0` for unlimited |
| `name`      | field name | Override the column name in MongoDB |
| `insertable`| `true`  | Whether the field is written on insert |
| `updatable` | `true`  | Whether the field is written on update |

```java
@Column(nullable = false, length = 100, unique = true)
public String email;

@Column(length = 0)
public String description;  // unlimited length

@Column(name = "user_name")
public String name;  // stored as "user_name" in MongoDB
```

### @Table

Override the MongoDB collection name for a model (defaults to the class name):

```java
@Table(name = "articles")
public class Article extends OIDGenericDataSoftDelete {
	// stored in the "articles" collection
}
```

### @GeneratedValue

Used with Long primary keys to enable auto-increment:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(nullable = false, unique = true)
public Long id = null;
```

> **Note:** This is already configured in `GenericData`. You only need this for custom models without inheritance.

### @CreationTimestamp

Automatically sets the field to the current timestamp on insert:

```java
@CreationTimestamp
@Column(nullable = false, insertable = false, updatable = false)
public Date createdAt = null;
```

### @UpdateTimestamp

Automatically sets the field to the current timestamp on insert and every update:

```java
@UpdateTimestamp
@Column(nullable = false, insertable = false, updatable = false)
public Date updatedAt = null;
```

### @DefaultValue

Sets a default value for a field when it is null on insert:

```java
@DefaultValue("'active'")
public String status = null;

@DefaultValue("'0'")
public Boolean deleted = null;
```

> **Note:** The value string uses single-quote quoting: `'string_value'` for strings, `0` or `1` for booleans.

### @DataNotRead

Excludes a field from default read queries. The field is still stored in the database but not returned unless `ReadAllColumn` is used:

```java
@DataNotRead
public Date createdAt = null;
```

### @DataDeleted

Marks a field as the soft-delete flag. When this field is `true`, the document is excluded from queries by default:

```java
@DataDeleted
@DataNotRead
@Column(nullable = false)
@DefaultValue("'0'")
public Boolean deleted = null;
```

Use the `AccessDeletedItems` query option to include soft-deleted documents.

API Annotations
===============

These annotations control how fields appear in the generated REST API and TypeScript client.

### @ApiReadOnly

Marks a field as read-only in the API. It cannot be set via POST (create) or PUT (update):

```java
@ApiReadOnly
public ObjectId oid = null;
```

### @ApiNotNull

Explicitly marks a field as required or optional in the generated API:

```java
@ApiNotNull
public String name;           // always required

@ApiNotNull(value = false)
public String nickname;        // explicitly optional
```

### @ApiGenerationMode

Controls which TypeScript types are generated for a model:

```java
@ApiGenerationMode(create = true, update = true)
public class User extends OIDGenericDataSoftDelete {
	public String name;
	public String email;
}
```

This generates three TypeScript types: `User` (read), `UserCreate`, and `UserUpdate`.


Relationship Annotations
========================

Archidata uses document-based relationship annotations for MongoDB. These manage bidirectional links between collections.

### @ManyToOneDoc

The child stores a reference to the parent. When the child is created/deleted/updated, the parent's list is automatically maintained.

```java
public class Comment extends OIDGenericDataSoftDelete {
	@CheckForeignKey(Article.class)
	@ManyToOneDoc(targetEntity = Article.class, remoteField = "comments")
	public ObjectId articleId;
	public String text;
}

public class Article extends OIDGenericDataSoftDelete {
	public String title;
	@OneToManyDoc(targetEntity = Comment.class, remoteField = "articleId")
	public List<@CheckForeignKey(Comment.class) ObjectId> comments;
}
```

Parameters:
| Parameter             | Default | Description |
|----------------------|---------|-------------|
| `targetEntity`       | —       | The parent class |
| `remoteField`        | —       | The field in the parent that stores the reverse list |
| `addLinkWhenCreate`  | `true`  | Auto-add to parent list on insert |
| `removeLinkWhenDelete`| `true` | Auto-remove from parent list on delete |
| `updateLinkWhenUpdate`| `true` | Auto-update parent list on update |

### @OneToManyDoc

The parent stores a list of child references. Define cascade behavior for updates and deletes.

```java
public class Author extends OIDGenericDataSoftDelete {
	public String name;
	@OneToManyDoc(
		targetEntity = Book.class,
		remoteField = "authorId",
		cascadeDelete = OneToManyDoc.CascadeMode.SET_NULL
	)
	public List<@CheckForeignKey(Book.class) ObjectId> books;
}
```

Parameters:
| Parameter          | Default   | Description |
|-------------------|-----------|-------------|
| `targetEntity`    | —         | The child class |
| `remoteField`     | —         | The field in the child that references the parent |
| `addLinkWhenCreate`| `true`   | Auto-update children on insert |
| `cascadeUpdate`   | `IGNORE`  | What to do to children when parent is updated |
| `cascadeDelete`   | `IGNORE`  | What to do to children when parent is deleted |

`CascadeMode` values:
- `DELETE` — delete the child documents
- `SET_NULL` — set the child's reference field to null
- `IGNORE` — do nothing (default)

### @ManyToManyDoc

Both sides store lists of references to each other:

```java
public class Student extends OIDGenericDataSoftDelete {
	public String name;
	@ManyToManyDoc(targetEntity = Course.class, remoteField = "students")
	public List<@CheckForeignKey(Course.class) ObjectId> courses;
}

public class Course extends OIDGenericDataSoftDelete {
	public String title;
	@ManyToManyDoc(targetEntity = Student.class, remoteField = "courses")
	public List<@CheckForeignKey(Student.class) ObjectId> students;
}
```

### @CheckForeignKey

Validates that a referenced ObjectId actually exists in the target collection:

```java
@CheckForeignKey(Author.class)
public ObjectId authorId;
```

Can also be used as a type annotation on list elements:

```java
public List<@CheckForeignKey(Tag.class) ObjectId> tags;
```


Validation Annotations
======================

Archidata supports Jakarta Validation (Bean Validation) with custom extensions.

### Standard Jakarta Annotations

```java
@NotNull
public String name;

@Size(min = 3, max = 128)
public String username;

@Pattern(regexp = "^[a-zA-Z0-9-_]+$")
public String slug;

@Email
public String email;

@Min(0)
@Max(100)
public Integer score;
```

### Validation Groups

Validation groups allow different validation rules for different operations:

| Group             | When applied |
|-------------------|-------------|
| `GroupCreate`     | On POST (create) |
| `GroupRead`       | On GET (read) |
| `GroupUpdate`     | On PUT/PATCH (update) |
| `GroupPersistant` | After database persistence |
| `GroupWithContext` | For validators needing DB access |

Example: An `id` field must be null on create but not null on read:

```java
@NotNull(groups = { GroupRead.class, GroupPersistant.class })
@Null(groups = { GroupCreate.class, GroupUpdate.class })
public ObjectId oid = null;
```

### Custom Validators

#### @CollectionNotEmpty

Ensures a collection field is not empty:

```java
@CollectionNotEmpty
public List<String> tags;
```

#### @CollectionItemUnique

Ensures all items in a collection are unique:

```java
@CollectionItemUnique
public List<ObjectId> memberIds;
```

#### @CollectionItemNotNull

Ensures no item in a collection is null:

```java
@CollectionItemNotNull
public List<String> values;
```

#### @ValueInList

Restricts a field to a predefined set of values:

```java
@ValueInList({"draft", "published", "archived"})
public String status;
```

#### @UniqueInBaseId

Ensures a field value is unique across all documents of a given class:

```java
@UniqueInBaseId(target = User.class, nameOfField = "login", groups = GroupWithContext.class)
public String login;
```

> **Note:** Use `groups = GroupWithContext.class` because this validator queries the database.


Complete Model Example
======================

```java
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiGenerationMode(create = true, update = true)
public class User extends OIDGenericDataSoftDelete {

	@Column(nullable = false, length = 128, unique = true)
	@NotNull(groups = { GroupCreate.class })
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[a-zA-Z0-9-_]+$")
	@UniqueInBaseId(target = User.class, nameOfField = "login", groups = GroupWithContext.class)
	public String login;

	@Column(nullable = false, length = 256)
	@NotNull(groups = { GroupCreate.class })
	@Email
	public String email;

	@Column(length = 512)
	public String bio;

	@Column(nullable = false)
	@DefaultValue("'active'")
	public String status;

	@CheckForeignKey(Article.class)
	@OneToManyDoc(
		targetEntity = Article.class,
		remoteField = "authorId",
		cascadeDelete = OneToManyDoc.CascadeMode.SET_NULL
	)
	public List<@CheckForeignKey(Article.class) ObjectId> articles;
}
```
