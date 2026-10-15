TypeScript API Generation
=========================

Archidata can generate TypeScript client code (types, API functions, Zod validators) from your annotated REST resources and data models.

The TypeScript generator uses the same introspection system (`AnalyzeApi`) as the [OpenAPI generator](openapi_generation.md) and reads the same `@ApiDoc` annotations for descriptions and examples.

Usage
-----

```java
AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(
	UserResource.class,
	ArticleResource.class
));

TsGenerateApi.generateApi(api, outputPath);
```

Annotations
-----------

- Use `@ApiDoc(description=..., example=...)` on models and fields to provide descriptions and examples in the generated TypeScript code.
- Use `@ApiGenerationMode(create = true, update = true)` on model classes to generate `*Create` and `*Update` TypeScript types in addition to the base read type.

See [Data Model - @ApiDoc](data_model.md#apidoc) and [Data Model - @ApiGenerationMode](data_model.md#apigenerationmode) for details.

Pagination
----------

A resource returning `Pagination<T>` is generated with an extra optional
`page` argument and answers `Promise<Pagination<T>>`:

```ts
const firstPage = await RobotKpiResource.getActions({
	restConfig,
	params: { entity },
	queries: { dueAfter, dueBefore },
	page: { offset: 0, limit: 50 },
});
console.log(firstPage.total, firstPage.hasNext);
```

`page` travels as the `X-Pagination-Offset` / `X-Pagination-Limit` headers,
which is what an endpoint reading `@PaginationContext` expects. On the way
back the server sends the items as a plain list plus `X-Total-Count` and
`Link`; the client assembles them into `Pagination<T>` and validates each item
with the model's own type guard.

A resource that carries its page through its own `offset` / `limit` **query**
parameters instead keeps working: they stay in `queries`, and the generated
call feeds them to the helper as well, so `total`, `offset` and `limit` on the
answer describe the page that was actually asked for.

One thing the server cannot do for such a resource: its `Link` header always
names the `X-Pagination-*` parameters, so `rel="next"` does not navigate an
endpoint that reads `offset` / `limit`. Reading `total` and asking for the next
page explicitly works; following the links needs the resource to take
`@PaginationContext`.

Forms (react-hook-form): optional fields that accept `null`
-----------------------------------------------------------

react-hook-form sends `null` for a field the user cleared, and an update is a
`PUT` of the whole object, so `null` means "field emptied". The generated Zod
schema of the request body must therefore accept `null` **and** a missing key
on its optional fields: `.nullable().optional()`.

How an optional field (no `@NotNull`) is generated:

| Class annotations                                          | Zod suffix                |
|------------------------------------------------------------|---------------------------|
| none                                                       | `.nullable()`             |
| `@JsonInclude(NON_NULL)`                                   | `.optional()`             |
| `@JsonInclude(NON_NULL)` + `@ApiOptionalIsNullable`        | `.nullable().optional()` (every usage) |
| `@JsonInclude(NON_NULL)` + `@ApiOptionalIsNullable(groups = …)` | `.nullable().optional()` in the listed groups, `.optional()` elsewhere |

A field-level `@jakarta.annotation.Nullable` makes the field optional even with
`@NotNull`, but follows the same table: under `@JsonInclude(NON_NULL)` it gives
`.optional()`, which does **not** accept `null`.

`@ValidGroup(GroupX.class)` on a parameter appends the group name to the
generated type: `@Valid @ValidGroup(GroupCreate.class) User` is `UserCreate`
(plain `@Valid` is the read variant, without suffix; no `@Valid` at all adds
`NV`). Pick the annotations from what the class is:

**A data model read, created and edited** (one class, `X` / `XCreate` /
`XUpdate`): the groups select the write variants.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiGenerationMode(create = true, update = true)
@ApiOptionalIsNullable(groups = { GroupCreate.class, GroupUpdate.class })
public class Article extends OIDGenericDataSoftDelete {
	@NotNull
	public String title;   // required everywhere
	public String summary; // Article: .optional(); ArticleCreate/ArticleUpdate: .nullable().optional()
}

@POST
public Article create(@Valid @ValidGroup(GroupCreate.class) final Article data) { … }
@PUT
@Path("{oid}")
public Article update(@PathParam("oid") final ObjectId oid,
		@Valid @ValidGroup(GroupUpdate.class) final Article data) { … }
```

**A request DTO** (a record or class with one use, the endpoint takes a plain
`@Valid`): no group, no `@ValidGroup`.

```java
// Do: generates UserCreate, birthDate: .nullable().optional()
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiOptionalIsNullable
public record UserCreate(@NotNull String login, LocalDate birthDate) {}

@POST
public UserAuthGet create(@Valid final UserCreate data) { … }
```

```java
// Do not: the group suffix renames the type UserCreateCreate, and the DTO
// carries groups it does not need.
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiOptionalIsNullable(groups = { GroupCreate.class })
public record UserCreate(@NotNull String login, LocalDate birthDate) {}

@POST
public UserAuthGet create(@Valid @ValidGroup(GroupCreate.class) final UserCreate data) { … }
```

`@ApiOptionalIsNullable` without groups needs archidata 0.50.3 or later (before,
it had no effect).

**`@DefaultValue` fields on update:** `DataAccess` skips a `null` field that
carries `@DefaultValue` (the stored value is kept), while any other `null`
field is removed from the document. A form can not clear such a field by
sending `null`: give it an explicit value instead.
