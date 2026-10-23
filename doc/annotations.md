Annotations reference
=====================

Single reference of every annotation archidata defines or interprets. Each entry gives the target,
the attributes with their defaults, the real effect (as implemented), a short example and the page
that details the topic. Archidata annotations live in `org.atriasoft.archidata.annotation` and its
sub-packages; the standard ones come from `jakarta.*` or Jackson. The JPA annotations
(`jakarta.persistence`: `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@ManyToOne`...) are not read:
they are rejected: analysing the model of an entity (or of a super class) that still carries one of them on the class, a field or a method throws a `DataAccessException` naming the class, the element, the old annotation and its replacement. See [MIGRATION.md](https://github.com/kangaroo-and-rabbit/archidata/blob/develop/MIGRATION.md)
for the correspondence.

Summary
-------

| Annotation | Theme | One line |
|------------|-------|----------|
| [`@Collection`](#collection) | Model | Collection name of an entity (default: class simple name). |
| [`@Id`](#id) | Model | Primary key field (`ObjectId`, `Long`, `UUID`...). |
| [`@Field`](#field) | Model | Name of the field in the MongoDB document; nothing else. |
| [`@InitValue`](#initvalue) | Model | Value written on insert when the field is `null`. |
| [`@Nullable`](#nullable) | Model | Marks a field as nullable for the API generators. |
| [`@DataNotRead`](#datanotread) | Model | Field written normally but not returned by default reads. |
| [`@DataEncrypt`](#dataencrypt) | Encryption | Transparent encryption of a `String` field. |
| [`@ManyToOneDoc`](#manytoonedoc) | Relations | Child side of a document-level 1-N link. |
| [`@OneToManyDoc`](#onetomanydoc) | Relations | Parent side of a document-level 1-N link, with cascade. |
| [`@ManyToManyDoc`](#manytomanydoc) | Relations | Document-level N-N link. |
| [`@Index`](#index) / [`@IndexList`](#indexlist) | Indexes | Class-level, repeatable (compound) MongoDB index. |
| [`@Indexed`](#indexed) | Indexes | Single-field MongoDB index. |
| [`@CreationTimestamp`](#creationtimestamp) | Timestamps | Set to now on insert. |
| [`@UpdateTimestamp`](#updatetimestamp) | Timestamps | Set to now on insert and on every update. |
| [`@DataDeleted`](#datadeleted) | Soft delete | Boolean soft-delete flag; deleted rows hidden by default. |
| [`@DataAsyncHardDeleted`](#dataasyncharddeleted) | Soft delete | Flag for a deferred physical removal. |
| [`@CheckForeignKey`](#checkforeignkey) | Validation | The referenced document must exist. |
| [`@UniqueInBaseId`](#uniqueinbaseid) | Validation | The value must be unique in the collection of `target`. |
| [`@ValueInList`](#valueinlist) | Validation | String restricted to a fixed list. |
| [`@CollectionNotEmpty`](#collectionnotempty) | Validation | Collection must not be empty. |
| [`@CollectionItemNotNull`](#collectionitemnotnull) | Validation | No `null` element in the collection. |
| [`@CollectionItemUnique`](#collectionitemunique) | Validation | No duplicated element in the collection. |
| [`@ValidGroup`](#validgroup) | Validation | Validate a request body against given groups. |
| [`GroupCreate` / `GroupUpdate` / `GroupRead` / `GroupPersistant` / `GroupWithContext`](#validation-groups) | Validation | Marker interfaces used as validation groups. |
| [Bean Validation (`@NotNull`, `@Null`, `@Size`, ...)](#standard-bean-validation) | Validation | Standard constraints, also read by the generators. |
| [`@ApiDoc`](#apidoc) | API generation | Description, example, group of a class, field or method. |
| [`@ApiGenerationMode`](#apigenerationmode) | API generation | Which variants (read/create/update) of a class are generated. |
| [`@ApiReadOnly`](#apireadonly) | API generation | Field excluded from create/update, and not written from the API. |
| [`@ApiNotNull`](#apinotnull) | API generation | Forces the nullability of a field in generated clients. |
| [`@ApiOptionalIsNullable`](#apioptionalisnullable) | API generation | Zod `.nullable().optional()` for the given groups. |
| [`@ApiInputOptional`](#apiinputoptional) | API generation | Optional form/body parameter in generated clients. |
| [`@ApiAsyncType`](#apiasynctype) | API generation | Extra types accepted by an update/async parameter or method. |
| [`@ApiTypeScriptProgress`](#apitypescriptprogress) | API generation | Progress callback in the generated TypeScript client. |
| [`@ApiAccessLimitation`](#apiaccesslimitation) | API generation | Deprecated, replaced by `@ApiReadOnly`. |
| [`@PaginationContext`](#paginationcontext) | HTTP | Injects offset/limit read from the pagination headers. |
| [`@ARCHIVE` / `@RESTORE` / `@CALL`](#custom-http-methods) | HTTP | Custom HTTP verbs for resource methods. |
| [`@DataAccessSingleConnection`](#dataaccesssingleconnection) | HTTP | One DB connection (optionally one transaction) per request. |
| [`@RolesAllowed`](#rolesallowed) | Security | Role check (any of the roles). |
| [`@RightAllowed`](#rightallowed) | Security | Fine-grained right check. |
| [`@PermitAll` / `@DenyAll`](#permitall-and-denyall) | Security | Public endpoint / always refused. |
| [`@PermitTokenInURI`](#permittokeninuri) | Security | Accept the token in the query string. |
| [`@JsonInclude`, `@JsonFormat`, `@Schema`, `@Operation`](#jackson-and-swagger-annotations) | Other | Interpreted by the JSON layer and the generators. |

Known pitfalls
--------------

- Unicity only exists through a unique index: declare `@Index` / `@Indexed(unique = true)` and call
  `IndexEngine.synchronize()` ([Indexes](indexes.md)).
- `createdAt` and `updatedAt` (from `GenericTiming`) carry `@DataNotRead`: they are not returned by
  default. Add the `ReadAllColumn` query option to read them.
- `@ValidGroup` only works when `ContextGenericTools.addJsr310(rc)` has been called (it registers
  `ValidGroupInterceptor`). It validates the given groups only, **not** `Default`: also put `@Valid`
  to check the unannotated constraints.
- A resource method with none of `@RolesAllowed`, `@RightAllowed`, `@PermitAll` makes
  `AuthenticationFilter` answer 403; `@DenyAll` always answers 403.
- `@Field` only renames the field in the database: use `@NotNull` and `@Size` for validation.
- `@InitValue` is the archidata annotation for a value written on insert. The JAX-RS
  `jakarta.ws.rs.DefaultValue` on an entity field is rejected with a `DataAccessException` when the
  model is analysed (it stays the right annotation on the
  parameters of a REST resource).


Model and persistence
---------------------

Detail: [Data model](data_model.md).

### @Collection

`org.atriasoft.archidata.annotation.Collection`, on a class. `value` (required) gives the MongoDB
collection name. Without the annotation the class simple name is used (`Article` for
`com.example.Article`); the annotation is read on the class itself, not inherited. Overridable per
query with the `OverrideTableName` option. An empty or blank name (`@Collection("")`) is rejected
with a `DataAccessException` when the model is analysed.

```java
@Collection("article")
public class Article extends OIDGenericDataSoftDelete { }
```

### @Id

`org.atriasoft.archidata.annotation.Id`, on a field (or a record component; not on a getter). No
attribute. Makes it the primary key (action `PRIMARY_KEY`). The key is generated on insert for an `ObjectId`, a `Long`
(sequence in the `counters` collection) or a `UUID`; another type (`String`, `Integer`...) is set
by the application and inserted with the `DirectPrimaryKey` option. Stored as `_id` when combined
with `@Field("_id")`, as in `OIDGenericData` and `UUIDGenericData` (`GenericData` keeps `id`).
`OIDGenericData` also keeps the driver's `@BsonId`: the MongoDB POJO codec registered by
`DbIoMongo` (`PojoCodecProvider.automatic(true)`) reads it to map the property to `_id`.

```java
@Id
@Field("_id")
public ObjectId oid;
```

### @Field

`org.atriasoft.archidata.annotation.Field`, on a field (or a record component; not on a getter).
`value` (required) is the name of the field in MongoDB, when it differs from the Java name. It is also the name to use
in `@Index`, `FilterValue` and the filters; the method references (`Fields.of(User::getFullName)`)
resolve it. It carries no constraint: use Bean Validation (`@NotNull`, `@Size`) and
`@Indexed(unique = true)`. The JSON name of the REST API does not change. An empty or blank name
(`@Field("")`) is rejected with a `DataAccessException` when the model is analysed: remove the
annotation to keep the Java name.

```java
@Field("user_name")
public String name;
```

### @InitValue

`org.atriasoft.archidata.annotation.InitValue`, on a field (or a record component, a getter or a
setter). When the field is `null` at insert time, `value` is converted to the field type (number, boolean, enum, string,
date; surrounding single quotes removed) and stored through the field codec, like any value (so it is
encrypted on a `@DataEncrypt` field). On an update, a `null` field carrying
`@InitValue` is left unchanged instead of being removed. For a `@DataDeleted` field it replaces the
implicit `false`.

```java
@InitValue("'0'")
public Integer retryCount;
```

Not to be confused with `jakarta.ws.rs.DefaultValue`, which archidata rejects on an entity field
(`DataAccessException` when the model is analysed) and which keeps its JAX-RS meaning on the parameters of a resource method.

### @Nullable

`jakarta.annotation.Nullable`, on a field. The field is considered nullable by the TypeScript/Python
generators (the generated type accepts `null`).

### @DataNotRead

`org.atriasoft.archidata.annotation.DataNotRead`, on a field. No attribute. The field is written
on insert like any other, but excluded from default reads; the `ReadAllColumn` query
option returns it. Used for `createdAt`, `updatedAt`, `deleted`. A full update (`updateById` without
`FilterValue`) never writes it: name it in a `FilterValue` to update it. `@DataNotRead` does not imply
`@ApiReadOnly`. It also applies to the fields of embedded sub-objects (POJO or record, alone or in a
collection): hidden by a standard read (and not transferred from the database, except inside a `Map`,
transferred whole without a log, or inside a part cut by the depth or path bound, which a WARN reports; the
Java read drops it in both cases, see
[database access](database_access.md#embedded-objects-are-projected-down-to-their-leaf-fields)), returned
with `ReadAllColumn`, and kept by a full update (see
[database access](database_access.md#update): sub-fields in dot notation, a collection of such objects
not written unless named in a `FilterValue`).

```java
@DataNotRead
public String internalNote;
```

Priority: when a field carries several archidata persistence annotations, the first matching one
decides its action in this order: `@Id`, `@CreationTimestamp`, `@UpdateTimestamp`, `@DataDeleted`,
`@DataAsyncHardDeleted`, `@DataNotRead`.


Relations
---------

Detail: [Data model](data_model.md#relationship-annotations). Targets: field (or method).
Archidata stores links as lists of ids on both sides; these annotations keep the two sides
consistent. They are only needed for bidirectional links; for a simple reference use
[`@CheckForeignKey`](#checkforeignkey).

The key of a relation field must be an `ObjectId`, a `Long` or a `UUID` (a `List` of them for
`@OneToManyDoc` and `@ManyToManyDoc`), or the target entity itself to expand it. Any other type
(for example a `String` key) is rejected when the model is analysed, with an error naming the
class and the field, instead of being silently ignored.

### @ManyToOneDoc

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `targetEntity` | required | Class of the parent. |
| `remoteField` | required | Field of the parent that holds the list of children. |
| `addLinkWhenCreate` | `true` | Add the link on the parent when the child is created. |
| `removeLinkWhenDelete` | `true` | Remove it when the child is deleted. |
| `updateLinkWhenUpdate` | `true` | Update it when the child's reference changes. |

```java
@CheckForeignKey(target = Category.class)
@ManyToOneDoc(targetEntity = Category.class, remoteField = "articleIds")
public ObjectId categoryId;
```

### @OneToManyDoc

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `targetEntity` | required | Class of the children. |
| `remoteField` | required | Field of the child that references this parent. |
| `addLinkWhenCreate` | `true` | Maintain the link when a child is created. |
| `cascadeUpdate` | `CascadeMode.IGNORE` | `DELETE`, `SET_NULL` or `IGNORE` when the list is modified. |
| `cascadeDelete` | `CascadeMode.IGNORE` | Same, when the parent is deleted. |

```java
@OneToManyDoc(targetEntity = Article.class, remoteField = "categoryId", cascadeDelete = OneToManyDoc.CascadeMode.SET_NULL)
public List<ObjectId> articleIds;
```

### @ManyToManyDoc

`targetEntity` (required) and `remoteField` (required): field of the other class that holds the
reverse list of ids. Both sides declare the annotation.

```java
@ManyToManyDoc(targetEntity = Tag.class, remoteField = "articleIds")
public List<@CheckForeignKey(target = Tag.class) ObjectId> tagIds;
```


Indexes
-------

Detail: [Indexes](indexes.md). Declarations are inert until `IndexEngine.synchronize()` is called
at startup.

### @Index

On a class, `@Repeatable`, `@Inherited`.

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `value` | required | Field names (structural names, dotted path for sub-documents). A leading `-` means descending. Several names make a compound index. |
| `name` | `""` | Index name (generated when empty). |
| `unique` | `false` | Reject duplicated values. |
| `sparse` | `false` | Skip documents without the field. |
| `expireAfterSeconds` | `-1` | TTL in seconds; `-1` means no TTL. |
| `partialFilter` | `""` | JSON partial filter expression. |

```java
@Index({ "companyId", "-createdAt" })
@Index(value = "email", unique = true)
public class User extends OIDGenericData { }
```

An unknown field name makes the synchronization fail.

### @IndexList

Container of the repeatable `@Index`. Never written by hand.

### @Indexed

On a field (or method): short form of a single-key `@Index`. Attributes: `ascending` (`true`),
`name` (`""`), `unique` (`false`), `sparse` (`false`), `expireAfterSeconds` (`-1`),
`partialFilter` (`""`).

```java
@Indexed(unique = true)
public String email;
```


Timestamps and soft delete
--------------------------

Detail: [Data model](data_model.md#base-model-classes). Provided out of the box by `GenericTiming`,
`OIDGenericDataSoftDelete`, `OIDGenericDataSoftAsyncHardDelete` and the `UUID` / `Generic*` variants.

### @CreationTimestamp

On a `Date` field. Set to the current time when the document is inserted. `GenericTiming.createdAt`
also carries `@DataNotRead` and `@ApiReadOnly`.

### @UpdateTimestamp

On a `Date` field. Set to the current time on insert and on every update. `updatedAt` also carries
`@DataNotRead`.

```java
@DataNotRead
@CreationTimestamp
private Date createdAt = null;
```

### @DataDeleted

On a `Boolean` field. Marks the soft-delete flag: `delete` sets it to `true`, inserts initialize it
to `false` (or to the `@InitValue`), and reads skip documents whose flag is true unless the
`AccessDeletedItems` option is given. Only one such field per class.

### @DataAsyncHardDeleted

On a `Boolean` field. `deleteHard` only sets this flag (and the `@DataDeleted` one) instead of
removing the document; a cleanup process (`CleanupTools`) removes it later. The `ForceHardDelete`
option removes it immediately.

```java
public class MyEntity extends OIDGenericDataSoftDelete {
	@DataNotRead
	@DataAsyncHardDeleted
	public Boolean hardDeleted = null;
}
```


Encryption
----------

Detail: [Field encryption](field_encryption.md).

### @DataEncrypt

On a `String` field.

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `value` | `"default"` (`EncryptionKeyStore.DEFAULT_KEY_NAME`) | Logical name of the base key. |
| `encryptKey` | `""` | Key used to encrypt (overrides `value`). |
| `decryptKey` | `""` | Key used to decrypt; a key without private part gives an encrypt-only field. |

Decryption on read is disabled unless `DATA_DECRYPT_ENABLE=true`; otherwise the field is left
`null`. As for `@DataNotRead`, a full update (`updateById` without `FilterValue`) never writes it:
name it in a `FilterValue` to update it.

```java
@DataEncrypt(encryptKey = "org42-pub", decryptKey = "org42-priv")
public String iban;
```


Validation
----------

Detail: [Data model](data_model.md#validation-annotations). All checkers are Bean Validation
constraints (`message`, `groups`, `payload` attributes as usual, `null` is valid unless stated).

### @CheckForeignKey

Targets: class, method, field, parameter, type use (so it works on list elements). `target`
(required): entity class that must contain the id. Default message `Foreign-key does not exist in
the DB`.

```java
@CheckForeignKey(target = User.class)
public ObjectId authorId;
public List<@CheckForeignKey(target = Tag.class) ObjectId> tagIds;
```

### @UniqueInBaseId

Targets: class, method, field, type use. `target` (required), `nameOfField` (default `"name"`).
Checks that no other document of `target` has that value in `nameOfField`; the current entity (id
taken from the validation context) is excluded, so use it with the `GroupWithContext` group.

```java
@UniqueInBaseId(target = User.class, nameOfField = "login", groups = GroupWithContext.class)
public String login;
```

### @ValueInList

Targets: class, method, field, type use. `value` (required `String[]`): allowed values.

```java
@ValueInList({ "draft", "published" })
public String state;
```

### @CollectionNotEmpty

Targets: class, method, field, parameter. The collection must contain at least one element.

### @CollectionItemNotNull

Targets: class, method, field. No `null` element.

### @CollectionItemUnique

Targets: class, method, field. No element appears twice.

### @ValidGroup

Targets: parameter, type use. `value`: the validation groups to apply to the request body. Works
through `ValidGroupInterceptor`, registered by `ContextGenericTools.addJsr310(rc)`. It validates
**only** these groups: add `@Valid` for the `Default` group. The TypeScript generator appends the
group name (without `Group`) to the generated type: `@Valid @ValidGroup(GroupCreate.class) User`
becomes `UserCreate`.

```java
@POST
public User create(@Valid @ValidGroup(GroupCreate.class) final User data) { ... }
```

### Validation groups

Marker interfaces in `org.atriasoft.archidata.annotation.checker`: `GroupCreate`, `GroupUpdate`,
`GroupRead`, `GroupPersistant` (database-level validation), `GroupWithContext` (validators that need
the entity id). Used in the `groups` attribute of any constraint, for example on `OIDGenericData`:
`@NotNull(groups = { GroupRead.class, GroupPersistant.class })` and
`@Null(groups = { GroupCreate.class, GroupUpdate.class })`.

### Standard Bean Validation

`jakarta.validation.constraints`: `@NotNull`, `@Null`, `@Size`, `@Min`, `@Max`, `@DecimalMin`,
`@DecimalMax`, `@Pattern`, `@Email`; and `jakarta.validation.Valid`. They are checked by the
validator and also read by the generators (required/optional, min/max, regex, examples). `@NotNull`
and `@Null` with `groups` drive which fields are required in the `Create` / `Update` / `Read`
variants of generated schemas.


API generation
--------------

Detail: [TypeScript](typescript_api_generation.md), [Python](python_api_generation.md),
[OpenAPI](openapi_generation.md). Package `org.atriasoft.archidata.annotation.apiGenerator`.

### @ApiDoc

Targets: class, field, method. Replaces `@Schema` / `@Operation` for documentation.

| Attribute | Default | Meaning |
|-----------|---------|---------|
| `description` | `""` | Description. |
| `example` | `""` | Example value (otherwise one is generated). |
| `group` | `""` | Group (tag) of an API method. |
| `hidden` | `false` | Leaves the element out of every generator (OpenAPI, TypeScript, Python, graphs): on a method (or on the method it overrides or implements in a superclass or an interface, when the method itself has no `@ApiDoc`), the endpoint; on a JAX-RS resource class (or a superclass or interface of it), all its endpoints; on a field, its getter or its setter, the property of the model. The REST endpoint still answers and the field is still serialized in JSON: it only hides them from the generated clients and documentation. No effect on a model class. |

```java
@ApiDoc(description = "Unique login", example = "john_doe")
public String login;
```

### @ApiGenerationMode

Target: class. Which variants of the model are generated: `read` (`true`), `create` (`false`),
`update` (`false`). Without it only the read type exists.

```java
@ApiGenerationMode(create = true, update = true)
public class User { }
```

### @ApiReadOnly

Targets: field, method. Excluded from the create and update variants; `update` / `updateById` also
skip it unless `ForceReadOnlyField` is used. Applied to ids and timestamps.

**Request bodies**: the JSON provider registered by `ContextGenericTools.addJsr310(rc)` drops every
`@ApiReadOnly` property (field, getter or setter) of a request body, as
`@JsonProperty(access = READ_ONLY)` would: the value sent by the client is ignored silently (no 400,
since a front often sends back the whole object it read, id and timestamps included) and the property
keeps the value its constructor gives it. This protects `insert` against mass assignment, since
`insert` writes the fields as they are in the Java object. Server code that sets an `@ApiReadOnly`
field itself before `insert` (a hash, a size, a counter) still persists it; no option is needed.
`ContextGenericTools.createObjectMapper()` (used by `RESTApiRequest`, backups...) does not drop
anything: a client still reads the read-only fields of a response. Properties set through a
constructor (records, `@JsonCreator`) are not filtered: do not put a field the client must not set in
an input record.

### @ApiNotNull

Target: field. `value` (default `true`): `true` forces the generated type to be non-nullable,
`false` forces it nullable, overriding other considerations.

### @ApiOptionalIsNullable

Target: class, with `@JsonInclude(NON_NULL)`. `groups` (default empty = all groups): in these
groups, optional fields are generated `.nullable().optional()` in Zod (needed by form libraries that
send `null` for empty fields).

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiOptionalIsNullable(groups = { GroupCreate.class, GroupUpdate.class })
public class MyModel { }
```

### @ApiInputOptional

Target: parameter. The form-data/body parameter is optional in the generated client (polymorphic
endpoints).

### @ApiAsyncType

Targets: parameter, method. `value` (required `Class<?>[]`): types accepted by the generated
function; `tsComplement` (default empty): extra TypeScript type text.

### @ApiTypeScriptProgress

Targets: parameter, method. The TypeScript client gets a progress callback (uploads).

### @ApiAccessLimitation

Deprecated since v0.30.0, for removal; use `@ApiReadOnly`. `readable`, `creatable`, `updatable`
(all `true`).


HTTP resources
--------------

### @PaginationContext

Target: parameter of type `org.atriasoft.archidata.dataAccess.model.PaginationContext` (same simple
name, different package). Filled from the `X-Pagination-Offset` and `X-Pagination-Limit` headers,
with server defaults when missing. Detail: [Pagination](pagination.md).

```java
@GET
public Pagination<Foo> list(@PaginationContext final PaginationContext page) { ... }
```

### Custom HTTP methods

`@ARCHIVE`, `@RESTORE`, `@CALL` (package `annotation.method`, meta-annotated with `@HttpMethod`),
on a method. The resource answers the verbs `ARCHIVE`, `RESTORE`, `CALL`; the generators emit the
matching client calls. No attribute.

### @DataAccessSingleConnection

Targets: class, method. `transactional` (default `false`). A JAX-RS name binding: one DB connection
(and a transaction if `transactional = true`) is opened for the request and closed at its end. It
has an effect only when `DataAccessRetentionConnectionFilter` is registered. Detail:
[Connection management](connectionManagement.md).


Security
--------

Detail: [Security](security.md). Read by `AuthenticationFilter`, which must be registered (usually
through a subclass).

### @RolesAllowed

`jakarta.annotation.security.RolesAllowed`, on a method (also found on the interface method).
Granted when the user has any of the listed roles, each prefixed with the application name.

### @RightAllowed

Target: method. `right` (required, `String`) and `access` (required, `PartRight`). Fine-grained
right of the user. If combined with `@RolesAllowed` both must pass.

```java
@RightAllowed(right = "articles", access = PartRight.READ)
public List<Article> list() { ... }
```

### @PermitAll and @DenyAll

`jakarta.annotation.security`. `@PermitAll` disables the check (public endpoint); `@DenyAll`
always answers 403. A method with none of the four annotations also answers 403.

### @PermitTokenInURI

Target: method (JAX-RS name binding). Accept the JWT from the `Authorization` / apikey query
parameters when no header is given (downloads, images in `<img>` tags).


Jackson and Swagger annotations
-------------------------------

- `@JsonInclude(NON_NULL)` on a class: serialization option; the generators read it to decide
  whether optional fields are absent or `null` (see `@ApiOptionalIsNullable`).
- `@JsonFormat` on dates: the format of `createdAt` / `updatedAt` in JSON.
- `@Schema` (description, example) and `@Operation`: still read as a fallback when `@ApiDoc` is
  absent, with a deprecation warning. Prefer `@ApiDoc`.
- `@Entity` appears in some examples but archidata does not read it.
