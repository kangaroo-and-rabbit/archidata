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
	@NotNull
	@Size(max = 200)
	public String title;
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
	public String message;
	public String level;
}
```

### GenericData / GenericDataSoftDelete

Uses a Long primary key with auto-increment (via a MongoDB `counters` collection):

```java
public class Counter extends GenericDataSoftDelete {
	@Size(max = 100)
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

All the annotations are summarized in the [annotations reference](annotations.md). The mapping
annotations are archidata's own (`org.atriasoft.archidata.annotation`): the JPA annotations
(`jakarta.persistence`) are not read: an entity that still carries one is rejected with a
`DataAccessException` when its model is analysed.

### @Id

Marks the primary key field. Required on exactly one field per model. An `ObjectId`, `Long` or
`UUID` key is generated on insert; a key of another type is set by the application and inserted with
the `DirectPrimaryKey` option.

```java
@Id
public ObjectId _id = null;
```

### @Field

Gives the name of the field in MongoDB when it differs from the Java name (also the name used in
`@Index`, `FilterValue` and the filters). It carries no constraint: validation goes through Bean
Validation (`@NotNull`, `@Size`) and unicity through a unique index (see [indexes](indexes.md)).

```java
@Field("user_name")
public String name;  // stored as "user_name" in MongoDB

@Indexed(unique = true) // unicity is only enforced by a unique index
@Size(max = 100)
public String email;
```

### @Collection

Override the MongoDB collection name for a model (defaults to the class simple name, the annotation
is not inherited):

```java
@Collection("articles")
public class Article extends OIDGenericDataSoftDelete {
	// stored in the "articles" collection
}
```

### @CreationTimestamp

Automatically sets the field to the current timestamp on insert:

```java
@CreationTimestamp
public Date createdAt = null;
```

### @UpdateTimestamp

Automatically sets the field to the current timestamp on insert and every update:

```java
@UpdateTimestamp
public Date updatedAt = null;
```

### @InitValue

Sets the value written in the database when the field is null on insert. On an update, a null
field carrying `@InitValue` is left unchanged instead of being removed:

```java
@InitValue("'active'")
public String status = null;

@InitValue("'0'")
public Boolean deleted = null;
```

> **Note:** The value is a string converted to the field type; the surrounding single quotes are
> optional (`'active'`, `'42'`; `'0'` reads as `false` for a boolean). Do not use the JAX-RS
> `@DefaultValue` on an entity field: archidata rejects it with a `DataAccessException` (it is meant
> for resource parameters).

### @DataNotRead

Excludes a field from default read queries. The field is still stored in the database but not returned unless `ReadAllColumn` is used. A full update (`updateById` without `FilterValue`) never writes it, nor a `@DataEncrypt` field: name it in a `FilterValue` to update it. Inside an embedded sub-object (POJO or record, alone or in a list), a `@DataNotRead` field is hidden the same way unless `ReadAllColumn` is used:

```java
@DataNotRead
public Date createdAt = null;
```

### @DataDeleted

Marks a field as the soft-delete flag. When this field is `true`, the document is excluded from queries by default:

```java
@DataDeleted
@DataNotRead
@InitValue("'0'")
public Boolean deleted = null;
```

Use the `AccessDeletedItems` query option to include soft-deleted documents.

API Annotations
===============

These annotations control how fields appear in the generated REST API and client code (TypeScript, Python, OpenAPI).

### @ApiDoc

Unified documentation annotation for all archidata API generators. Replaces the use of Swagger's `@Schema` (on classes and fields) and `@Operation` (on methods).

Can be applied to:
- **Classes**: describes the model
- **Fields**: describes a field with optional example
- **Methods**: describes an API endpoint with optional group

```java
@ApiDoc(description = "A user in the system", example = "{\"login\":\"john\"}")
public class User extends OIDGenericDataSoftDelete {

	@ApiDoc(description = "Unique login", example = "john_doe")
	@Size(min = 3, max = 128)
	public String login;

	@ApiDoc(description = "Email address")
	@Email
	public String email;

	@ApiDoc(hidden = true)
	public String internalCode;  // excluded from generated API docs
}
```

On REST endpoints:

```java
@GET
@ApiDoc(description = "List all users", group = "USERS")
public List<User> getAll() { ... }
```

| Parameter     | Default | Description |
|---------------|---------|-------------|
| `description` | `""`    | Description of the class, field, or API method |
| `example`     | `""`    | Example value (classes and fields) |
| `group`       | `""`    | Logical group / tag for API methods |
| `hidden`      | `false` | Leave the field, the endpoint or the whole resource class out of the generated clients and documentation (the field is still serialized in JSON, the endpoint still answers) |

> **Backward compatibility:** When `@ApiDoc` is absent, the generators fall back to Swagger's `@Schema` and `@Operation` with a deprecation warning. This fallback will be removed in a future version.

> **Auto-generated examples:** When no explicit `example` is provided, archidata generates one automatically based on validation constraints (`@Size`, `@Min`, `@Max`, `@Email`...), field name heuristics (e.g. `email` → `"user@example.com"`, `createdAt` → `"2000-01-23T01:23:45.678Z"`), and type defaults. See [OpenAPI Generation](openapi_generation.md) for details.

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

The write variants are the ones an endpoint asks for with `@Valid @ValidGroup(GroupCreate.class) User`
(the group name is appended to the type name). For forms that send `null` (react-hook-form), see
[TypeScript API Generation - Forms](typescript_api_generation.md#forms-react-hook-form-optional-fields-that-accept-null).


Relationship Annotations
========================

Archidata uses document-based relationship annotations for MongoDB. These manage bidirectional links between collections.

The key of a relation field must be an `ObjectId`, a `Long` or a `UUID` (a `List` of them for
`@OneToManyDoc` and `@ManyToManyDoc`), or the target entity itself to expand it. A relation on
another key type (for example `String`) is rejected when the model is analysed, with an error
naming the class and the field; it used to be silently ignored.

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
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiGenerationMode(create = true, update = true)
public class User extends OIDGenericDataSoftDelete {

	@Indexed(unique = true)
	@NotNull(groups = { GroupCreate.class })
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[a-zA-Z0-9-_]+$")
	@UniqueInBaseId(target = User.class, nameOfField = "login", groups = GroupWithContext.class)
	public String login;

	@NotNull(groups = { GroupCreate.class })
	@Email
	public String email;

	@Size(max = 512)
	public String bio;

	@InitValue("'active'")
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
