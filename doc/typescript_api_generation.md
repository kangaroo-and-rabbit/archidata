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