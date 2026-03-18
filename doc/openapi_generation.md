OpenAPI Generation
==================

Archidata can generate an [OpenAPI 3.0.3](https://spec.openapis.org/oas/v3.0.3) specification directly from your annotated REST resources and data models, without depending on the Swagger library. The generated spec is suitable for API documentation tools (Swagger UI, Redoc) and client code generators.


Quick Start
-----------

### 1. Expose the `/openapi/swagger.json` endpoint

Register the `openApiResource` REST resource in your application and configure it at startup:

```java
AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(
	UserResource.class,
	ArticleResource.class
));
openApiResource.configure(api, "My Application API", "1.0.0");
```

The `openApiResource` class provides a `GET /openapi/swagger.json` endpoint that serves the generated spec as JSON. The result is cached after the first request.

### 2. Annotate your models and endpoints

Use `@ApiDoc` on your models, fields, and REST methods:

```java
@ApiDoc(description = "An article in the system")
public class Article extends OIDGenericDataSoftDelete {

	@ApiDoc(description = "Article title", example = "Getting Started with Archidata")
	@Size(min = 1, max = 200)
	public String title;

	@ApiDoc(description = "Article content")
	@Column(length = 0)
	public String content;

	@CheckForeignKey(User.class)
	public ObjectId authorId;
}

@Path("/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArticleResource {

	@GET
	@ApiDoc(description = "List all articles", group = "ARTICLES")
	public List<Article> getAll() { ... }

	@GET
	@Path("/{id}")
	@ApiDoc(description = "Get an article by ID", group = "ARTICLES")
	public Article getById(@PathParam("id") ObjectId id) { ... }

	@POST
	@ApiDoc(description = "Create a new article", group = "ARTICLES")
	public Article create(ArticleInput data) { ... }
}
```


@ApiDoc Annotation
------------------

`@ApiDoc` is a unified documentation annotation that replaces Swagger's `@Schema` (on classes/fields) and `@Operation` (on methods).

| Target   | Parameters used                     | Replaces             |
|----------|-------------------------------------|----------------------|
| Class    | `description`, `example`            | `@Schema`            |
| Field    | `description`, `example`, `hidden`  | `@Schema`            |
| Method   | `description`, `group`              | `@Operation`         |

See [Data Model - @ApiDoc](data_model.md#apidoc) for the full reference.

### Backward compatibility

When `@ApiDoc` is absent, the generators fall back to Swagger's `@Schema` and `@Operation` annotations with a deprecation warning in the logs. This allows gradual migration.


Auto-Generated Examples
-----------------------

When a field has no explicit `example` in `@ApiDoc`, archidata generates one automatically. The resolution order is:

1. **Explicit example** from `@ApiDoc(example=...)` (highest priority)
2. **Constraint-based** generation from Jakarta Validation annotations
3. **Field-name heuristics** based on the field name
4. **Type-based defaults** (lowest priority)

### Constraint-based examples

| Annotation              | Generated example                        |
|-------------------------|------------------------------------------|
| `@Email`                | `user@example.com`                       |
| `@Size(min=3, max=128)` | String of `min` length (or field-name based) |
| `@Min(0)`               | `0`                                      |
| `@Max(100)`             | `0` (or `max` if `max < 0`)             |
| `@DecimalMin("0.5")`    | `0.5`                                    |

### Field-name heuristics

The generator recognizes common field name patterns:

| Pattern                                      | Type           | Example                                            |
|----------------------------------------------|----------------|----------------------------------------------------|
| `*email*`, `*mail*`                          | String         | `user@example.com`                                 |
| `*url*`, `*uri*`, `*link*`                   | String         | `https://example.com/resource`                     |
| `*token*`, `*password*`, `*secret*`          | String         | `abc123xyz789`                                     |
| `*name*`, `*login*`, `*username*`            | String         | `example-name`                                     |
| `*title*`, `*label*`, `*subject*`            | String         | `Example Title`                                    |
| `*description*`, `*comment*`, `*message*`    | String         | `Example description text`                         |
| `*path*`, `*file*`, `*filename*`             | String         | `/data/example-file.dat`                           |
| `*sha*`, `*hash*`, `*md5*`                   | String         | `e3b0c44298fc1c14...` (SHA-256 of empty)           |
| `codec`                                      | String         | `h264`                                             |
| `language`, `lang`, `locale`                 | String         | `en`                                               |
| `mime`, `type`, `contentType`                | String         | `application/json`                                 |
| `id`, `*Id`                                  | Long           | `123456`                                           |
| `id`, `*Id`                                  | UUID           | `e6b33c1c-d24d-11ee-b616-02420a030102`             |
| `id`, `*Id`                                  | ObjectId       | `65161616841351`                                   |
| `*At`, `created*`, `updated*`, `*Time`       | Date/Instant   | `2000-01-23T01:23:45.678Z`                         |
| `*At`, `*Date`                               | LocalDate      | `2000-01-23`                                       |
| `*deleted*`, `*blocked*`, `*archived*`       | Boolean        | `false`                                            |
| `*enabled*`, `*active*`, `*visible*`         | Boolean        | `true`                                             |
| `width`                                      | Integer        | `1920`                                             |
| `height`                                     | Integer        | `1080`                                             |
| `size`, `length`, `count`                    | Integer        | `1024`                                             |
| `*sampleRate*`                               | Integer        | `48000`                                            |
| `channels`                                   | Integer        | `2`                                                |
| `*duration*`                                 | Double         | `120.5`                                            |
| `*frameRate*`, `*fps*`                       | Double         | `29.97`                                            |

### Type-based defaults

| Type              | Example                                    |
|-------------------|--------------------------------------------|
| `String`          | `string`                                   |
| `Boolean`         | `false`                                    |
| `Long`, `Integer` | `0`                                        |
| `Double`, `Float` | `0.0`                                      |
| `UUID`            | `e6b33c1c-d24d-11ee-b616-02420a030102`     |
| `ObjectId`        | `65161616841351`                           |
| `Date`, `Instant` | `2000-01-23T01:23:45.678Z`                 |
| `LocalDate`       | `2000-01-23`                               |
| `LocalTime`       | `01:23:45`                                 |
| `Enum`            | First enum value                           |
| `List`, `Map`     | *(no auto-generation)*                     |


Generated Spec Structure
------------------------

The generator produces a standard OpenAPI 3.0.3 document with:

- **`info`**: title and version from `configure()` parameters
- **`paths`**: one entry per REST endpoint, grouped by path
  - HTTP methods mapped from JAX-RS annotations (`@GET`, `@POST`, `@PUT`, `@DELETE`)
  - Parameters from `@PathParam`, `@QueryParam`, `@HeaderParam`
  - Request body for unnamed parameters (POST/PUT body)
  - Multipart support for file uploads
- **`components/schemas`**: one entry per model class and enum
  - Field properties with type, description, example, and constraints
  - `allOf` for class inheritance
  - `$ref` for cross-references between models
  - Enum values listed as string arrays


Programmatic Usage
------------------

You can generate the spec programmatically without the REST resource:

```java
AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(UserResource.class, ArticleResource.class));
api.addModel(User.class);
api.addModel(Article.class);

// As a Map tree
Map<String, Object> spec = OpenApiGenerateApi.generate(api, "My API", "1.0.0");

// As a JSON string
String json = OpenApiGenerateApi.generateJson(api, "My API", "1.0.0");
```


Architecture
------------

The OpenAPI generator reuses the same introspection system (`AnalyzeApi`, `ApiGroupModel`, `ApiModel`, `ClassObjectModel`, `ClassEnumModel`) used by the TypeScript and Python client generators. This ensures consistency across all generated outputs.

```
@ApiDoc + JAX-RS annotations
         |
    AnalyzeApi (introspection)
         |
    +----+----+----+
    |    |    |    |
    TS  Python Dot OpenAPI
```
