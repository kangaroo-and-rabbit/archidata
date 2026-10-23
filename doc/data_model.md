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

Field Types
===========

A persisted field (a public field, or a property with a getter) is stored according to its
**declared** type, never to the class of the value it holds.

| Declared type | Stored as |
| --- | --- |
| primitives and boxed types, `String`, `Character` | BSON value |
| `ObjectId`, `UUID` | BSON value |
| `Date`, `Instant`, `OffsetDateTime` | BSON date (UTC, the offset is lost) |
| `LocalDate` (`yyyy-MM-dd` string), `LocalTime` (nanoseconds of the day) | BSON value |
| an enum | its name |
| `org.bson.Document` | free-form sub-document of values, `Document`s, `List`s and `Map`s, stored and read back as it is (see below) |
| `List<X>`, `Set<X>` | BSON array (a `Set` is read back as a `HashSet`) |
| `Map<K, V>` | sub-document whose keys are the keys as strings (`String`, `Integer`, `Long`, `Short`, `ObjectId`, enum) |
| a concrete class or a record of the application | embedded sub-document, with the fields of that class |

**Embedded objects are written by their declared type.** The fields stored for an embedded object
are those of the declared class (type of the field, element type of a `List` / `Set`, value type
of a `Map`), at any depth, and it is read back as that class. A value of a subclass is stored with
the fields of the declared class only: the fields the subclass adds are dropped, and a warning is
logged once per field and per class of value
(`Field com.app.Holder.base is declared com.app.Base but holds a com.app.Derived: only the fields of Base are stored, the fields Derived adds are dropped ([extra, secret])`).
A value that is not an instance of the declared class (reached through a raw type) is refused with
a `DataAccessException`.

**Refused declared types.** A declared type that does not tell which fields are stored is refused
when the model of the entity is analysed (`DbClassModel.of`, on the first use of the entity: insert,
read, `IndexEngine`, change journal, `Filters.checkFieldName`...), for every persisted field
reachable from the entity, at any depth of its embedded classes:

- `Object`, an interface or an abstract class, JDK ones included (`Serializable`, `Number`,
  `CharSequence`, `Comparable`, Jackson `JsonNode`...), and a type variable (`T`, erased to `Object`);
- a raw `List`, `Set` or `Map`, or one whose element (value) type is a wildcard (`List<?>`,
  `List<? extends X>`) or a type variable (`List<T>`);
- a collection that is neither a `List` nor a `Set` (`Collection<X>`, `Iterable<X>`, `Queue<X>`...);
- an array of a class of the application or of one of the above (`Point[]`, `Object[]`,
  `Serializable[][]`): an array is not stored element by element;
- a `List`, `Set` or `Map` whose element (value) type is one of the above.

An abstract class (or an interface) of the application used as the root of a model (an abstract
entity checked by `Filters.checkFieldName`, for example) is checked like a concrete one.

The refusal follows the declaration, whatever the package of the class: a class of a library used as
the type of a field (`com.mongodb.client.model.geojson.Point`, a `jakarta.*`, JTS or Joda class...) is
walked as an embedded object like a class of the application, and a single property of it declared
`Object`, an interface or an abstract class refuses the entity at startup, even when it always holds
`null` (`field Point.coordinateReferenceSystem (path location.coordinateReferenceSystem) is declared
com.mongodb.client.model.geojson.CoordinateReferenceSystem, which can hold any class ...`). Store such a
value in an `org.bson.Document` (a GeoJSON `Point` put in a `Document` is encoded by the driver), or
declare a concrete DTO of the application with the fields to keep.

**One exemption: a primary key declared `Object`** (`@Id public Object _id`), whose class is given on
each call by an `OptionSpecifyType("_id", ObjectId.class)` (or chosen by the value passed to
`DirectPrimaryKey`). Its value is written as it is once checked to be a key value: an `ObjectId`, a
`UUID`, a `String`, a `Long`, an `Integer` or a `Short`; any other class is refused
(`Field com.app.Keyed._id is a primary key declared java.lang.Object but holds a java.util.Date: such a key only takes an ObjectId, a UUID, a String, a Long, an Integer or a Short`).
Any other field declared `Object` (or an interface, an abstract class, an unbound type variable),
alone or as the element of a `List`, `Set` or `Map`, stays refused **with or without**
`OptionSpecifyType`: the option no longer makes such a field usable.

The exception is an `IntrospectionException` (`DB model creation failed for Account`) whose cause, a
`DataAccessException`, names the entity, the field, its path, the declared type and the replacement:

```
Unsupported field type on com.app.Account: field Wrapper.inner (path wrapper.inner) is declared java.lang.Object, which can hold any class (Object, an interface or an abstract class), and archidata stores and reads an embedded value by its declared type only; declare a concrete class (or a List, Set or Map of one), or org.bson.Document for free-form content (see MIGRATION.md)
```

Replace such a field with a concrete class (or a `List`, `Set` or `Map` of one), or with
`org.bson.Document` for free-form content. The relation fields (`@ManyToOneDoc`, `@OneToManyDoc`,
`@ManyToManyDoc`) are checked by their own rules (see below).

**A `Document` holds values only.** A field declared `org.bson.Document` (or a `List`, `Set` or `Map` of
them) is written as it is, but its content is checked first, at any depth, against an allowlist; anything
else fails the write with a `DataAccessException`, and nothing is stored. Accepted:

- `null` and the values: `String`, the boxed primitives (`Character` included), `BigDecimal`, `ObjectId`,
  `UUID`, `Date`, `Instant`, `OffsetDateTime`, `LocalDate`, `LocalTime`, `LocalDateTime`, `Pattern`, the
  enums (stored by their name) and the arrays of primitives (`byte[]`...); the exact class counts (a
  subclass of `Date` such as `java.sql.Timestamp` is refused);
- the BSON types: any `BsonValue` (`BsonDocument`, `BsonArray`...), `Binary`, `Decimal128`, `Code`, `Symbol`,
  `MinKey`, `MaxKey`, `BSONTimestamp`, `JsonObject`, and a GeoJSON `Geometry` of the driver (`Point`...);
- walked: a `Map` with `String` keys (`Document` included), a `Collection` (`List`, `Set`...), an array of
  objects, the scope of a `CodeWithScope` and the id of a `DBRef`, at most 100 levels deep (the limit of
  MongoDB).

A value of a class of the application (a POJO, a record, an implementation of `Bson`) is refused because
the driver would encode it field by field with its `@DataNotRead` and `@DataEncrypt` fields in clear:

```
Field com.app.Account.free is a free-form org.bson.Document holding, under 'list.1.deep.k', a com.app.Secret, a class of the application: the driver would store all its fields (@DataNotRead and @DataEncrypt ones included, in clear); put only values, Documents, Lists and Maps in a Document, or declare a field with that class
```

Any other class (a JDK wrapper such as `AtomicReference`, `Optional` or `Map.Entry`, a `StringBuilder`, a
Jackson node, a `Map` with non-`String` keys) is refused too, because what it holds would not be checked:

```
Field com.app.Account.free is a free-form org.bson.Document holding, under 'list.0.x', a java.util.concurrent.atomic.AtomicReference, which is not a value archidata accepts in a Document (a BSON or JDK value, an enum, a Document or a Map with String keys, a List, a Collection or an array of them): what it holds would not be checked; put only values, Documents, Lists and Maps in a Document, or declare a field with the class it holds
```

The raw writes (`insertBsonDocument`, `updateBsonDocuments`, the driver collection) do not go through this
check; the client still refuses to encode there an object of the application holding a `@DataNotRead` or
`@DataEncrypt` field (see [Raw access to the driver](database_access.md#raw-access-to-the-driver)).

**Not handled.** The other classes of the JDK, of BSON and of Jackson (`BigDecimal`,
`LocalDateTime`, `Optional`, `Decimal128`, `ObjectNode`...) and the arrays of values or of such
classes (`byte[]`, `String[]`, `BigDecimal[]`...) are neither refused nor modeled: the codec does not
convert them, so they do not read back. Do not use them on an entity (`LocalDateTime` is not
supported: use `OffsetDateTime` or `Instant`). Writing there a value of a class of the application (a
subclass of `BigDecimal`, an implementation of `org.bson.conversions.Bson`, an array of them) fails
with a `DataAccessException`
(`Field com.app.Account.price is declared java.math.BigDecimal, a type archidata does not model, but holds a com.app.Money, a class of the application: it cannot be stored there; declare the field with that class`),
and `Filters.checkFieldName` refuses any path below such a field (`price.scale`); the change journal
copies such a field without its sub-keys and the TRACE masking masks them.

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

The links are maintained by the writes of the data layer. On an update, these side effects follow the
`Condition` of the caller: an update that matches no document (an owner filter that excludes it, a
soft-deleted target) changes no remote document. See
[Scoped update and relation side effects](database_access.md#scoped-update-and-relation-side-effects).

Expanding a relation runs a read of the target entities, which expands their own relations in turn.
This is bounded: at most `RelationExpansion.getMaxDepth()` nested levels (5 by default,
`RelationExpansion.setMaxDepth(int)` to change it), and an entity already expanded higher on the same
chain (a cycle `A -> B -> A`, or a self-reference) is read again without expanding its relations.
Beyond these limits the relation field is left `null`.

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
