Error handling
==============

Archidata turns every exception that leaves a REST resource into the same JSON
body, whatever its origin (your code, bean validation, query-parameter parsing,
JSON parsing, Jersey itself). The mapping is done by a set of JAX-RS
`ExceptionMapper`s (`org.atriasoft.archidata.catcher`) driven by a small
exception hierarchy (`org.atriasoft.archidata.exception`).

This page covers the registration, the exception to HTTP mapping, the exact
response shape, how to fail cleanly from a resource, and how a Java client
gets the error back.


When to use it
--------------

Always: register the catchers in every server you build with archidata. Without
them Jersey answers with its own HTML or empty error pages, the TypeScript and
Python generated clients cannot parse the failure, and `RESTApi` (see
[tools.md](tools.md)) falls back to an `IOException` instead of a typed
`RESTErrorResponseException`.

Throw the archidata exceptions from a resource when you want to control the
status and the message. Let everything else (validation, parameter conversion)
flow through: the catchers already handle it.


Registration
------------

```java
final ResourceConfig rc = new ResourceConfig();
GenericCatcher.addAll(rc);          // all the catchers below, in one call
ContextGenericTools.addJsr310(rc);  // JSON provider + ValidGroupInterceptor
rc.register(JacksonFeature.class);
rc.register(ValidationFeature.class);
```

`GenericCatcher.addAll(ResourceConfig)` is the only entry point; the class has
a private constructor. It registers, in this order: `JacksonExceptionCatcher`,
`InputExceptionCatcher`, `SystemExceptionCatcher`, `FailExceptionCatcher`,
`ConstraintViolationExceptionCatcher`, `QueryParamExceptionCatcher`,
`ExceptionCatcher`, `WebApplicationExceptionCatcher`. The order does not matter:
Jersey picks the mapper whose exception type is the closest to the thrown
class.

Karideo, karso, karusic and karentory all call `GenericCatcher.addAll(rc)` in
their `WebLauncher`.


Exception to HTTP mapping
-------------------------

| Thrown | Mapper | HTTP status | `name` in the body |
|---|---|---|---|
| `FailException` | `FailExceptionCatcher` | `exception.status` (default 400) | `Request Fail` |
| `UnAuthorizedException` (extends `FailException`) | `FailExceptionCatcher` | 401 | `Request Fail` |
| `InputException` | `InputExceptionCatcher` | `exception.status` (default 400; the body `status` and `statusMessage` follow it) | `Constraint Violation` |
| `SystemException` | `SystemExceptionCatcher` | `exception.status` (default 500) | `System error` |
| `jakarta.validation.ConstraintViolationException` | `ConstraintViolationExceptionCatcher` | 400 | `Error on input='<path>'` for one violation, `Multiple error on input` for several, `Constraint Violation` for none |
| `org.glassfish.jersey.server.ParamException.QueryParamException` | `QueryParamExceptionCatcher` | 400 | `Error on query input='<param>'` |
| any other `jakarta.ws.rs.WebApplicationException` (`NotFoundException`, `ForbiddenException`, other `ParamException`...) | `WebApplicationExceptionCatcher` | the status carried by the exception | `Catch system exception` |
| `com.fasterxml.jackson.core.JacksonException` | `JacksonExceptionCatcher` | 400 for a body that cannot be parsed (`StreamReadException`) or does not match the expected type (`MismatchedInputException`, `ValueInstantiationException`); 500 for any other Jackson failure | `Catch JSON Exception` |
| any other `Exception` (including `DataAccessException`) | `ExceptionCatcher` | 500 | `Internal Server Error` |

Every mapper logs the error with its `oid` (see below) at `ERROR` level, so a
client report can be matched to a log line.

`DataAccessException`, `RESTErrorResponseException` and the introspection
exceptions are plain `Exception`s with no dedicated mapper: they end up in the
generic 500.


Response shape
--------------

The body is a serialized `RestErrorResponse` (annotated `@ApiGenerationMode`,
so the generated TypeScript and Python clients know it) with
`@JsonInclude(NON_NULL)`:

| Field | Type | Meaning |
|---|---|---|
| `oid` | `ObjectId` (string) | unique id of this error occurrence, also written in the server log |
| `name` | string | short error type, see the table above |
| `message` | string | human-readable message (`exception.getMessage()`) |
| `time` | string | ISO-8601 instant of the error |
| `status` | int | HTTP status code |
| `statusMessage` | string | HTTP reason phrase (`Bad Request`...) |
| `inputError` | list, optional | one `RestInputError` per invalid input |

`RestInputError` has three fields: `argument` (the parameter name), `path` (the
property path inside the argument) and `message`.

```json
{
  "oid": "6710a3c2f1e4b52a9c0d1e77",
  "name": "Error on input='valueCreate'",
  "message": "Validation error",
  "time": "2026-10-15T09:41:07.512Z",
  "status": 400,
  "statusMessage": "Bad Request",
  "inputError": [
    { "path": "valueCreate", "message": "size must be between 0 and 5" }
  ]
}
```

How `argument` and `path` are filled depends on the source:

- Validation through `@Valid` on a resource parameter: the Jakarta property path
  looks like `postValid.arg0.valueNoGroup`; the first segment (method name) is
  dropped, so `argument = "arg0"` and `path = "valueNoGroup"`. Compile with
  `-parameters` to get the real parameter names instead of `arg0`.
- Validation through `@ValidGroup` only (done by `ValidGroupInterceptor`, see
  [converters_and_beans.md](converters_and_beans.md)): the path has no method
  prefix, so `argument` is absent and `path` holds the property path.
- `InputException(variable, message)`: a single entry, `path = variable`.
- `QueryParamException`: a single entry with `argument = "query"` and
  `path = <parameter name>`.

`inputError` is sorted by `argument.path`, so the order is stable.


Failing cleanly from a resource
-------------------------------

```java
@GET
@Path("{oid}")
@RolesAllowed("USER")
public Article get(@PathParam("oid") final ObjectId oid) throws Exception {
    final Article article = DataAccess.get(Article.class, oid);
    if (article == null) {
        throw new InputException(Response.Status.NOT_FOUND, "oid", "Article not found: " + oid);
    }
    if (article.locked) {
        throw new FailException(Response.Status.CONFLICT, "Article is locked");
    }
    return article;
}
```

Pick the exception by intent:

| Intent | Exception |
|---|---|
| a request input is wrong or refers to something that does not exist | `InputException(variable, message)` (400) or `InputException(status, variable, message)` |
| the request is refused for a business reason | `FailException(message)` (400) or `FailException(status, message)` |
| the caller is not authorized | `UnAuthorizedException()` (message "Not enough Right") or `UnAuthorizedException(message)` |
| an internal failure, not the caller's fault | `SystemException(message)` (500) or `SystemException(status, message)` |

`FailException` also has `FailException(status, message, Exception cause)`: the
cause is logged immediately in the constructor, and again by the catcher; it
is never sent to the client.

For field-level validation prefer the Jakarta annotations on the model
(`@NotNull`, `@Size`...) and `@Valid`: the violations are collected and
returned together in `inputError`, instead of failing on the first one.


Reading the error from a Java client
------------------------------------

`RESTApiRequest.fetch(...)` and `fetchList(...)` throw a
`RESTErrorResponseException` when the status is not 2xx and the body is a
`RestErrorResponse`. Its public fields mirror the JSON (`oid`, `time`, `name`,
`message`, `status`, `statusMessage`, `inputError`).

```java
try {
    api.request("articles", oid).get().fetch(Article.class);
} catch (final RESTErrorResponseException ex) {
    LOGGER.error("{} ({}): {}", ex.name, ex.status, ex.message);
}
```

If the error body is not JSON (a proxy or gateway page for a 502/503/504), an
`IOException` carrying the status and the body is thrown instead.


Class reference
---------------

`org.atriasoft.archidata.exception`:

| Class | Constructors | Public fields |
|---|---|---|
| `FailException` | `(String)`, `(Response.Status, String)`, `(Response.Status, String, Exception)` | `status`, `exception` (cause, may be null) |
| `UnAuthorizedException` | `()`, `(String)` | inherited |
| `InputException` | `(String variable, String message)`, `(Response.Status, String variable, String message)` | `missingVariable`, `status` |
| `SystemException` | `(String)`, `(Response.Status, String)` | `status` |
| `DataAccessException` | `(String)`, `(String, Throwable)` | none |
| `RESTErrorResponseException` | `()`, `(ObjectId, time, name, message, status, statusMessage, inputError)` | `oid`, `time`, `name`, `message`, `status`, `statusMessage`, `inputError` |

`org.atriasoft.archidata.catcher`:

| Class | Role |
|---|---|
| `GenericCatcher` | `static void addAll(ResourceConfig)` registers every mapper |
| `RestErrorResponse` | the JSON body; constructors `(status, time, error, message, inputError)`, `(status, time, error, message)`, `(status, error, message)` (time = now), `(status)` (name `generic`) |
| `RestInputError` | one invalid input: `argument`, `path`, `message` |
| `FailExceptionCatcher`, `InputExceptionCatcher`, `SystemExceptionCatcher`, `ConstraintViolationExceptionCatcher`, `QueryParamExceptionCatcher`, `WebApplicationExceptionCatcher`, `JacksonExceptionCatcher`, `ExceptionCatcher` | the mappers, public with a public no-arg constructor; register them individually only if you want a subset |

All mappers answer with `Content-Type: application/json`.


Pitfalls
--------

- `FailException` does not use the `inputError` list: only `InputException`
  and validation errors do.
- The generic `ExceptionCatcher` returns `exception.getMessage()` to the
  client. A message built from a database or file-system error can leak
  internals: wrap such failures in a `SystemException` with a neutral message
  and keep the detail in the log (the `oid` links both).
- A request rejected by a `ContainerRequestFilter` through
  `requestContext.abortWith(...)` never reaches the mappers. The archidata
  `AuthenticationFilter` builds its 401/403 answers itself with a
  `RestErrorResponse` (for example name `FORBIDDEN`, message
  `Not enough RIGHT !!!`), so the shape is the same but the `name` values are
  not the ones of the table above. Your own filters must do the same.
- `RestInputError.getFullPath()` is package-private: it is not part of the
  client API.
