Converters, validation groups and bean introspection
====================================================

Four packages sit between the data model and the wire. Most of them work
without you touching them; this page tells what each one does for you, which
types they support, how to extend them, and how the validation groups are
chosen.

| Package | What it does | Do you call it? |
|---|---|---|
| `converter.jackson` | JSON serializers/deserializers for `ObjectId`, `Date`, `OffsetDateTime`, BSON `Document` | no: `ContextGenericTools` installs them |
| `converter.Jakarta` | JAX-RS parameter converters for `Date` and `OffsetDateTime` | register them once |
| `converter.mongo` | BSON codec for `OffsetDateTime`, installed in the MongoDB client | no |
| `interceptor` | `ValidGroupInterceptor`: bean validation by group on request bodies | registered by `ContextGenericTools.addJsr310` |
| `bean`, `bean.accessor`, `bean.annotation`, `bean.exception` | cached class introspection and fast property accessors | internal, see the last section |


Server setup
------------

```java
final ResourceConfig rc = new ResourceConfig();
rc.register(DateParamConverterProvider.class);            // @QueryParam / @PathParam of type Date
rc.register(OffsetDateTimeParamConverterProvider.class);  // ... and of type OffsetDateTime
ContextGenericTools.addJsr310(rc);                        // JSON mapper + ValidGroupInterceptor
rc.register(JacksonFeature.class);
rc.register(ValidationFeature.class);
```

`addJsr310` is the important call: it builds the `ObjectMapper` of
[tools.md](tools.md) (`ContextGenericTools.createObjectMapper()`), registers it as
the Jersey JSON provider and registers `ValidGroupInterceptor`. Without it, JSON
dates and `ObjectId` use Jackson's defaults, and `@ValidGroup` is ignored.
The parameter converters are annotated `@Provider`, so a launcher that scans
packages finds them; the explicit `rc.register` of the test launcher is the safe
form.


Supported types
---------------

### In JSON (request and response bodies)

| Java type | JSON | Notes |
|---|---|---|
| `ObjectId` | string of 24 hex digits | `ObjectIdSerializer` / `ObjectIdDeserializer`; an invalid hex string fails with an `IllegalArgumentException` |
| `java.util.Date` | ISO-8601 string in UTC with milliseconds (`1999-01-30T18:16:17.123Z`) | read through `DateTools.parseDate`: a missing zone means UTC, see [tools.md](tools.md) |
| `OffsetDateTime` | same, in UTC | **the original offset is not kept** on output; read with `DateTools.parseOffsetDateTime`, which requires an offset |
| `org.bson.Document` | JSON object | nested documents, lists, maps and primitives; ObjectId as hex string. Reading yields `String`, `Integer`, `Long`, `Double`, `Boolean`, `List`, `Document`; other numbers become text |
| `Instant`, `LocalDate`, `LocalTime`, `LocalDateTime`... | ISO-8601 | from Jackson's `JavaTimeModule` (added last in the mapper), with `WRITE_DATES_AS_TIMESTAMPS` disabled |

All other types follow Jackson's defaults.

### In query and path parameters

`Date` (`DateParamConverterProvider`, with `@Priority(1)`) and `OffsetDateTime`
(`OffsetDateTimeParamConverterProvider`): the ISO-8601 string is parsed with
`DateTools` (same rules as above, including the zone rule), an empty value gives
`null`, and an invalid one raises an `IllegalArgumentException`, which Jersey
wraps in a `QueryParamException` answered as 400 by the
[error catchers](error_handling.md). `ObjectId` parameters work out of the box
through its `String` constructor.

### In MongoDB documents

Entity fields are converted by `MongoCodecFactory` (package
`dataAccess.model.codec`, shared with the data layer): `long`, `int`, `short`,
`float`, `double`, `boolean`, `String`, `Character`, `ObjectId`, `UUID`, `Date`
natively; `Instant` and `OffsetDateTime` as a BSON date; `LocalDate` as the string `yyyy-MM-dd`;
`LocalTime` as nanoseconds of the day (`long`); enums by `toString()` (not
`name()`); `List<E>`; `Set<E>` stored as a list; `Map<K,V>` stored as a
sub-document with `K` among `String`, `Integer`, `Long`, `Short`, `ObjectId`;
`org.bson.Document` as is; and any other class or record as a nested sub-document
(its fields converted the same way). See [data_model.md](data_model.md) for the
annotations.

An `OffsetDateTime` entity field is stored like an `Instant`: a BSON date (UTC,
millisecond precision). It is read back **in UTC**: the original offset is not
kept (`2025-06-15T14:30+02:00` comes back as `2025-06-15T12:30Z`, the same
instant), and the sub-millisecond part is lost. `OffsetDateTimeCodec`
(`converter.mongo`) does the same for the values handled by the MongoDB driver
itself (it is installed in the client registry). Store the offset in a separate
field if you need it. `LocalDateTime` has no branch: it is not a supported
persisted type.


Adding a converter
------------------

There is one extension point per channel.

### A new JSON type

`addJsr310` builds its mapper internally and has no hook. To add your own module,
build the provider yourself instead of calling it:

```java
final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
final SimpleModule module = new SimpleModule();
module.addSerializer(Money.class, new MoneySerializer());
module.addDeserializer(Money.class, new MoneyDeserializer());
mapper.registerModule(module);

final JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
provider.setMapper(mapper);
rc.register(provider);
rc.register(ValidGroupInterceptor.class);   // keep the group validation
```

`JacksonJaxbJsonProvider` is the Jersey-internal class
(`org.glassfish.jersey.jackson.internal.jackson.jaxrs.json`) that
`ContextGenericTools` itself uses. Serializers extend Jackson's `JsonSerializer<T>`
and `JsonDeserializer<T>`. The built-in set is exposed as
`JacksonModules.getAllModules()` (a `SimpleModule`), which you can register on
any other `ObjectMapper` (a Kafka serializer, for example).

### A new parameter type

Implement `jakarta.ws.rs.ext.ParamConverterProvider` (return a `ParamConverter<T>`
for your type, `null` for the others), mark it `@Provider`, and register it with
`rc.register(...)`. `DateParamConverterProvider` is a short model.

### A new persisted type

There is no registration point: `MongoCodecFactory` is a closed list. The
options are to store the value in a supported type (a `String`, a `long`, a
`Date`) and expose a typed getter, or to model the value as a small record/class,
which is stored as a sub-document.


Validation groups and the interceptor
-------------------------------------

Bean Validation constraints can be assigned to groups. Archidata ships the group
markers `GroupCreate`, `GroupRead`, `GroupUpdate`, `GroupPersistant` and
`GroupWithContext` (package `annotation.checker`); the generic base models use
them, for example an `oid` that must be null on creation and update:

```java
@Null(groups = { GroupCreate.class, GroupUpdate.class })
@NotNull(groups = { GroupRead.class, GroupPersistant.class })
public ObjectId oid;
```

Jersey's `@Valid` only validates the default group. `@ValidGroup` (a parameter
annotation) selects the groups to check:

```java
@POST
public Article create(@Valid @ValidGroup(GroupCreate.class) final Article data) { ... }

@PUT
@Path("{oid}")
public Article update(@PathParam("oid") final ObjectId oid,
        @Valid @ValidGroup(GroupUpdate.class) final Article data) { ... }
```

How it works: `ValidGroupInterceptor` is a Jersey `ReaderInterceptor`. After
the body is deserialized, it looks for a `@ValidGroup` among the annotations of
the parameter, and if found validates the object with exactly those groups. A
violation raises a `ConstraintViolationException`, answered as 400 by the
[catchers](error_handling.md). It is verified by `TestValidGroup`:

| Parameter annotations | Checked |
|---|---|
| `@Valid` | the default group only (constraints without `groups`) |
| `@ValidGroup(GroupCreate.class)` | the `GroupCreate` constraints **only**; the default-group constraints are not checked |
| `@ValidGroup({GroupUpdate.class, GroupCreate.class})` | the union of both groups |
| `@Valid @ValidGroup(...)` | both: Jersey checks the default group, the interceptor checks the named groups |

So combine `@Valid` and `@ValidGroup` to get the default constraints (`@Size`,
`@NotNull` without group) plus the group ones. A group marker does not extend
`Default`.

Other effects of `@ValidGroup`:

- The TypeScript and Python generators use it to pick the type of the request:
  `@Valid @ValidGroup(GroupCreate.class) User` becomes `UserCreate`. Put it on a
  shared data model with create/update variants, not on a DTO made for a single
  endpoint (the suffix would be added twice: `UserCreateCreate`). See
  [typescript_api_generation.md](typescript_api_generation.md).
- `ValidationProxy` (see [tools.md](tools.md)) applies the same logic to calls
  that do not go through Jersey.
- The interceptor needs the `Validator` injected by Jersey; outside a Jersey
  container it does nothing useful.

The error shape of a group violation does not have the usual `arg0` prefix, see
[error_handling.md](error_handling.md).


Bean introspection (internal)
-----------------------------

`org.atriasoft.archidata.bean` is the reflection layer used by the data access
(`DbClassModel`, the Mongo codecs, the add-ons) and by the API generators. An
application rarely calls it. Summary, in case you extend the framework:

- `ClassModel.of(Class<?>)` returns the cached, thread-safe introspection of a
  class: public constructors, instance fields (including the inherited ones), and
  getters/setters merged by property name into `PropertyDescriptor`s.
  Supported shapes: plain classes, records, enums, static inner classes, and
  fluent setters. `ClassModel.clearCache()` empties the cache (tests).
- `PropertyDescriptor` exposes the name, `TypeInfo` (generic-aware type with
  `isList()`, `isSet()`, `isMap()`, `isEnum()`, `isRecord()`, `isOptional()`...),
  the annotations merged from field, getter and setter, and `getValue` /
  `setValue` on an instance.
- `ClassModel.newInstance(Map<String,Object>)` picks the constructor matching
  the most values, then fills the rest through setters.
- `bean.accessor`: `LambdaAccessorFactory` builds getters and setters with
  `LambdaMetafactory` (`PropertyGetter`, `PropertySetter`, and typed variants),
  to avoid reflection cost at each access.
- `bean.annotation.UnifiedAnnotationLookup` merges the annotations of the field,
  the getter and the setter (and their interface declarations); the field wins.
- `bean.exception.IntrospectionException` is the checked exception of the layer.

Because the introspection reads the parameter names of the constructors, a
non-record class that you want built through a constructor must be compiled with
`-parameters`; otherwise the names are `arg0`, `arg1` and no value matches.


Pitfalls
--------

- An `OffsetDateTime` field is persisted and serialized to JSON in UTC: the
  offset given by the client is lost (the instant is kept).
- Enums are stored with `toString()`. An enum that overrides `toString()` is
  stored under that text, and renaming it breaks the existing documents.
- `@ValidGroup` without `@Valid` skips the default group, so a plain `@NotNull`
  on a field is not enforced. Use both.
- The groups are chosen on the endpoint parameter, not on the model: the same
  `Article` class is validated differently by `create` and `update`.
- Forgetting `ContextGenericTools.addJsr310(rc)` silently disables
  `@ValidGroup` and changes the date format of the responses.
