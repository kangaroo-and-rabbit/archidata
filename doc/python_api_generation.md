Python API Generation
=====================

Archidata can generate a complete Python client package (Pydantic models, one
client class per REST resource, an aggregated `ApiClient`) from your annotated
REST resources and data models — the Python counterpart of the
[TypeScript generator](typescript_api_generation.md).

The Python generator uses the same introspection system (`AnalyzeApi`) as the
[OpenAPI](openapi_generation.md) and TypeScript generators and reads the same
`@ApiDoc` annotations for descriptions.

Usage
-----

```java
AnalyzeApi api = new AnalyzeApi();
api.addAllApi(List.of(
	UserResource.class,
	ArticleResource.class
));

// The directory IS the Python package: its name is the import name.
PythonGenerateApi.generateApi(api, Paths.get("public_api/python/src/my_server_api"), true);
```

`generateApi(AnalyzeApi)` returns the in-memory generation map instead, so a
caller can add its own files before writing them with
`GenerationWriter.writeGeneratedFiles(map, dir, ".py", deleteObsolete)`.

Generated layout
----------------

```
my_server_api/
├── __init__.py        re-exports model, api, rest_tools and ApiClient
├── py.typed           PEP 561 marker (the package is fully typed)
├── rest_tools.py      REST runtime: urllib3 + Pydantic (copied from archidata)
├── client.py          ApiClient: one bound client per resource
├── api/
│   ├── __init__.py
│   └── user_resource.py       class UserResourceApi, one method per endpoint
└── model/
    ├── __init__.py            re-exports + model_rebuild() of cyclic models
    ├── user.py                class User / UserCreate / UserUpdate (Pydantic)
    ├── user_type.py           class UserType(StrEnum)
    └── object_id.py           ObjectId: TypeAlias = Annotated[str, Field(pattern=...)]
```

Runtime requirements: **Python 3.12**, `pydantic>=2`, `urllib3>=2`. 3.12 is not a
floor picked at random: the generated code uses PEP 695 type aliases
(`type ObjectId = ...`) and PEP 695 type parameters (`class Pagination[T]`),
which 3.11 cannot parse.

Mapping rules
-------------

| Java                                        | Python                                              |
|---------------------------------------------|-----------------------------------------------------|
| `String`, `Boolean`, `Object`               | `str`, `bool`, `Any`                                |
| `Long/Integer/Short`, `Double/Float`        | `int`, `float`                                      |
| `Date`, `Instant` / `LocalDate` / `LocalTime` | `datetime` / `date` / `time`                      |
| `UUID`                                      | `uuid.UUID`                                         |
| `ObjectId`                                  | `ObjectId` (a `str` constrained to 24 hex chars)    |
| `org.bson.Document`, `Map<String, ?>`       | `Document` / `dict[str, Any]`                       |
| `List<T>` / `List<@NotNull T>`              | `list[T \| None]` / `list[T]`                       |
| `Map<K, V>`                                 | `dict[K, V \| None]`                                |
| `Pagination<T>`                             | `Pagination[T]` (from `rest_tools`)                 |
| enum                                        | `StrEnum` (`IntEnum` when `getValue()` is an int)   |
| `ObjectId`, `Document`                      | a PEP 695 alias: `type ObjectId = Annotated[str, ...]` |
| class                                       | `BaseModel` subclass, inheritance preserved         |

Models:

- field names are `snake_case`; a `Field(alias="camelCase")` keeps the wire
  name, and `model_config = ConfigDict(populate_by_name=True)` accepts both;
- the variants are the ones the API requests, named like the TypeScript
  ones: `User` (read), `UserCreate` (`@Valid @ValidGroup(GroupCreate.class)`),
  `UserUpdate`, `UserNV` (parameter without `@Valid`). A child variant extends
  the same variant of its parent (`UserCreate(OIDGenericDataCreate)`);
- required / optional follows the same rules as the TypeScript generator
  (`@NotNull(groups = ...)`, `@Null(groups = ...)`, `@Nullable`, primitives),
  optional fields are `T | None = None`. One deliberate difference: a
  non-validated variant (`NV`) keeps every field (all optional) where the
  TypeScript one is an empty object;
- `@Size`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`, `@Pattern` become
  `Field(min_length=, ge=, pattern=, ...)`, `@ApiReadOnly` becomes
  `Field(frozen=True)`, `@ApiDoc(description)` becomes `Field(description=)`;
- model modules import each other eagerly; when two models reference each
  other, one side of the cycle is imported under `if TYPE_CHECKING:` and
  resolved by `model_rebuild()` in `model/__init__.py`.

API:

- one class per resource (`UserResourceApi`), constructed with a `RESTConfig`
  or a callable returning one (evaluated before each request: use it when the
  token changes at runtime);
- one method per endpoint, named in `snake_case`. Path parameters are
  positional, in the order of the path; then everything is keyword-only:
  `data` (the body, typed with the matching variant; a `PATCH` also accepts a
  `dict`), the query parameters (optional, `None` = not sent), the headers,
  `offset` / `limit` for a paginated endpoint, `accept` when the endpoint
  produces several media types;
- a paginated endpoint takes `offset` / `limit`, sent as the
  `X-Pagination-Offset` / `X-Pagination-Limit` headers, and returns
  `Pagination[T]` assembled from the item list body plus the `X-Total-Count`
  and `Link` headers. A resource declaring its own `offset` / `limit` query
  parameters keeps them: they are reused as the page input rather than
  doubled, so the returned page echoes what was asked;
- the answer is validated with Pydantic and returned typed (`User`,
  `list[User]`, `Pagination[User]`, `int`...). `void` returns `None`, a
  non-JSON `@Produces` returns the raw `bytes`;
- any failure raises `RESTError` (`status`, `message`, `url`, and `response`
  holding the server's `RestErrorResponse` when the body has that shape).
  The pseudo statuses are the TypeScript ones: 902 invalid JSON, 950 model
  check failed, 951 paginated body is not a list, 999 connection failure.

```python
from my_server_api import ApiClient, RESTConfig, RESTError, UserCreate

client = ApiClient(RESTConfig(server="https://my.server/api", token_api=API_KEY))
users = client.user_resource.gets(status="ACTIVE")          # -> list[User]
user = client.user_resource.create(data=UserCreate(name="x"))
try:
    client.user_resource.get(user.oid)
except RESTError as error:
    print(error.status, error.message)
```

Typing
------

The package is fully annotated and ships `py.typed` (PEP 561), so a consumer's
own checker reads it instead of falling back to `Any`:

- every endpoint declares its parameters and its return type;
- models are Pydantic classes, which double as the runtime validation;
- the two helpers that take "a type Pydantic can validate" carry the same
  overload pair as `pydantic.TypeAdapter`, so a plain class keeps the answer
  typed and an annotated alias falls back to `Any` — which the generated
  method narrows again through its own return annotation.

The generated output is held to the static checkers the organisation uses,
`ruff` (the configuration `neofarm_ros` pins in its pre-commit) and `ty`, and
it must pass them **without a per-file exception**: a violation is a bug in
this generator, not something the consuming project silences.

Checking the output
-------------------

`TestPythonApiGeneration` writes a sample package under
`target/generated-python-sample/sample_api`; with a virtualenv holding
`pydantic`, `urllib3`, `ruff` and `ty`:

```bash
cd target/generated-python-sample
python -c "import sample_api"
ruff check sample_api
ruff format --check sample_api
ty check sample_api
```

The back's own CI does exactly that on the real package, then runs the test
suite committed next to it (see `doc/python-generation.md` there).

Annotations
-----------

See [Data Model - @ApiDoc](data_model.md#apidoc) and
[Data Model - @ApiGenerationMode](data_model.md#apigenerationmode) for the
shared annotations.
