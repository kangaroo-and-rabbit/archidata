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
