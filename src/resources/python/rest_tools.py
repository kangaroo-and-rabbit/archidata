"""REST runtime of the generated Python API client (auto-generated code).

This module is copied verbatim by the archidata Python generator next to the
generated ``model/`` and ``api/`` packages. It wraps urllib3 with the same
contract as the TypeScript ``rest-tools.ts``: URL building, token headers,
JSON / multipart bodies, typed responses validated with Pydantic, error
mapping to :class:`RESTError` and the ``Pagination<T>`` protocol
(``X-Pagination-Offset`` / ``X-Pagination-Limit`` request headers,
``X-Total-Count`` / ``Link`` response headers).

@author Edouard DUPIN
@copyright 2024, Edouard DUPIN, all right reserved
@license MPL-2
"""

from __future__ import annotations

from collections.abc import Callable, Mapping
from dataclasses import dataclass, field
from datetime import date, datetime, time
from enum import StrEnum
import json
import logging
from typing import Any, cast, overload, TYPE_CHECKING
from urllib.parse import quote, urlencode
from uuid import UUID

from pydantic import BaseModel, TypeAdapter, ValidationError
from pydantic_core import to_json
from urllib3 import PoolManager, Retry, Timeout
from urllib3.exceptions import HTTPError as Urllib3HTTPError
from urllib3.filepost import encode_multipart_formdata

from .model import RestErrorResponse


if TYPE_CHECKING:
    from urllib3.response import BaseHTTPResponse

__all__ = [
    "HTTPMimeType",
    "HTTPRequestModel",
    "ModelResponseHttp",
    "MultipartFile",
    "Pagination",
    "RESTConfig",
    "RESTConfigProvider",
    "RESTConfigSource",
    "RESTError",
    "RESTModel",
    "RESTRequest",
    "RESTRequestBytes",
    "RESTRequestJson",
    "RESTRequestPaginatedJson",
    "RESTRequestType",
    "RESTRequestVoid",
    "RESTUrl",
    "resolve_rest_config",
]

logger = logging.getLogger(__name__)

_HTTP_SUCCESS_MIN = 200
_HTTP_SUCCESS_MAX = 299
#: Pseudo statuses shared with ``rest-tools.ts``.
STATUS_INVALID_JSON = 902
STATUS_MODEL_CHECK_FAILED = 950
STATUS_PAGINATION_SHAPE = 951
STATUS_CONNECTION_FAILED = 999


class HTTPRequestModel(StrEnum):
    """HTTP request methods (including the archidata extensions)."""

    ARCHIVE = "ARCHIVE"
    CALL = "CALL"
    DELETE = "DELETE"
    HEAD = "HEAD"
    GET = "GET"
    OPTION = "OPTION"
    PATCH = "PATCH"
    POST = "POST"
    PUT = "PUT"
    RESTORE = "RESTORE"


class HTTPMimeType(StrEnum):
    """HTTP MIME types."""

    ALL = "*/*"
    CSV = "text/csv"
    IMAGE = "image/*"
    IMAGE_JPEG = "image/jpeg"
    IMAGE_PNG = "image/png"
    JSON = "application/json"
    MULTIPART = "multipart/form-data"
    OCTET_STREAM = "application/octet-stream"
    TEXT_PLAIN = "text/plain"


@dataclass
class RESTConfig:
    """Where and how to reach the server."""

    #: Base of the server API: ``http(s)://my.server.org/plop/api/``.
    server: str
    #: Bearer token (``Authorization: Bearer <token>``).
    token: str | None = None
    #: API key (``Authorization: ApiKey <token_api>``), used when ``token`` is unset.
    token_api: str | None = None
    #: Connection timeout, in seconds.
    connect_timeout: float = 5.0
    #: Read timeout, in seconds.
    read_timeout: float = 30.0
    #: Number of retries on connection errors.
    retries: int = 3
    #: Extra headers added to every request.
    headers: dict[str, str] = field(default_factory=dict)


#: A callable returning the configuration to use for the next call: pass one
#: instead of a :class:`RESTConfig` when the token changes at runtime.
RESTConfigProvider = Callable[[], RESTConfig]
RESTConfigSource = RESTConfig | RESTConfigProvider


def resolve_rest_config(source: RESTConfigSource) -> RESTConfig:
    """Return the configuration to use now, calling the provider if needed."""
    if isinstance(source, RESTConfig):
        return source
    return source()


@dataclass
class RESTModel:
    """Description of one endpoint."""

    #: Local path of the request, with ``{param}`` placeholders: ``"sheep/{id}"``.
    end_point: str
    #: HTTP method.
    request_type: HTTPRequestModel = HTTPRequestModel.GET
    #: Expected response MIME type (``Accept`` header).
    accept: HTTPMimeType | None = None
    #: Request body MIME type (``Content-Type`` header).
    content_type: HTTPMimeType | None = None
    #: Pass the token as a query parameter instead of a header.
    token_in_url: bool = False


@dataclass
class ModelResponseHttp:
    """Raw HTTP response, before model validation."""

    status: int
    #: Parsed JSON when the response is JSON, the raw body (``bytes``) otherwise.
    data: Any
    headers: Mapping[str, str]
    #: The URL that was called.
    url: str = ""


@dataclass(frozen=True)
class Pagination[T]:
    """A page of a ``Pagination<T>`` endpoint.

    The server sends the items as a plain list body plus the ``X-Total-Count``
    and ``Link`` (RFC 5988) headers; :func:`RESTRequestPaginatedJson`
    assembles them here.
    """

    items: list[T]
    total: int
    offset: int
    limit: int
    has_next: bool
    has_prev: bool
    #: Raw ``Link`` header, for clients that want to follow ``rel="next"``.
    link_header: str | None = None


class RESTError(Exception):
    """Any failure of a REST call: network, HTTP status, decoding or validation.

    ``status`` is the HTTP status when the server answered, or one of the
    pseudo statuses shared with ``rest-tools.ts``: 902 (invalid JSON body),
    950 (response does not match the model), 951 (paginated body is not a
    list), 999 (connection failure).
    """

    def __init__(
        self,
        message: str,
        *,
        status: int,
        name: str = "REST error",
        url: str | None = None,
        response: RestErrorResponse | None = None,
        body: bytes | None = None,
    ) -> None:
        """Build the error; ``response`` is the parsed server error when available."""
        super().__init__(message)
        self.message = message
        self.status = status
        self.name = name
        self.url = url
        #: The server error body, when it follows the archidata ``RestErrorResponse`` shape.
        self.response = response
        #: The raw server body, for the other cases.
        self.body = body

    def __str__(self) -> str:
        """Return a one-line summary."""
        where = f" on {self.url}" if self.url else ""
        return f"{self.name} ({self.status}){where}: {self.message}"


#: A multipart file value: raw bytes, ``(filename, bytes)`` or ``(filename, bytes, mime_type)``.
MultipartFile = bytes | tuple[str, bytes] | tuple[str, bytes, str]


@dataclass
class RESTRequestType:
    """Everything needed to perform one call."""

    rest_model: RESTModel
    rest_config: RESTConfigSource
    #: Body: a Pydantic model, any JSON-serializable value, or a multipart fields dict.
    data: Any = None
    #: Path parameters, substituted in ``rest_model.end_point``.
    params: Mapping[str, Any] | None = None
    #: Query parameters; ``None`` values are skipped, lists are repeated.
    queries: Mapping[str, Any] | None = None
    headers: Mapping[str, str] | None = None


_HTTP_POOL: PoolManager | None = None
_ADAPTERS: dict[Any, TypeAdapter[Any]] = {}


def _get_http_pool() -> PoolManager:
    """Return the shared connection pool."""
    # one pool per process, like the module-level fetch of the TypeScript client
    global _HTTP_POOL
    if _HTTP_POOL is None:
        _HTTP_POOL = PoolManager()
    return _HTTP_POOL


def _adapter(type_: Any) -> TypeAdapter[Any]:
    """Return the (cached) validator of a type."""
    adapter = _ADAPTERS.get(type_)
    if adapter is None:
        adapter = TypeAdapter(type_)
        _ADAPTERS[type_] = adapter
    return adapter


def _to_url_value(value: Any) -> str:
    """Render a path or query value the way the Java server parses it."""
    if isinstance(value, StrEnum):
        return _to_url_value(value.value)
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, datetime | date | time):
        return value.isoformat()
    if isinstance(value, UUID):
        return str(value)
    if isinstance(value, BaseModel):
        return value.model_dump_json(by_alias=True)
    return str(value)


def RESTUrl(request: RESTRequestType, rest_config: RESTConfig | None = None) -> str:
    """Build the full URL of a request (path parameters, queries, token in URL).

    ``rest_config`` is the already resolved configuration; when omitted the
    request's provider is called (once per request in :func:`RESTRequest`).
    """
    rest_model = request.rest_model
    if rest_config is None:
        rest_config = resolve_rest_config(request.rest_config)
    url = f"{rest_config.server.rstrip('/')}/{rest_model.end_point.lstrip('/')}"
    if request.params:
        for key, value in request.params.items():
            url = url.replace(f"{{{key}}}", quote(_to_url_value(value), safe=""))
    query: list[tuple[str, str]] = []
    if request.queries:
        for key, value in request.queries.items():
            if value is None:
                continue
            if isinstance(value, list | tuple | set | frozenset):
                query.extend(
                    (key, _to_url_value(item)) for item in value if item is not None
                )
            else:
                query.append((key, _to_url_value(value)))
    if rest_model.token_in_url:
        if rest_config.token is not None:
            query.append(("Authorization", f"Bearer {rest_config.token}"))
        elif rest_config.token_api is not None:
            query.append(("Authorization", f"ApiKey {rest_config.token_api}"))
    if not query:
        return url
    return f"{url}?{urlencode(query)}"


def _encode_multipart_value(value: Any) -> Any:
    """Turn a multipart field value into what urllib3 accepts."""
    if isinstance(value, bytes | str | tuple):
        return value
    if isinstance(value, BaseModel):
        return value.model_dump_json(by_alias=True, exclude_unset=True)
    if isinstance(value, StrEnum):
        return _to_url_value(value)
    if isinstance(value, bool | int | float):
        return _to_url_value(value)
    return to_json(value, by_alias=True).decode("utf-8")


def _encode_body(rest_model: RESTModel, data: Any) -> tuple[bytes | None, str | None]:
    """Serialize the body; return it with the ``Content-Type`` to send (if any)."""
    if data is None:
        return None, None
    content_type = rest_model.content_type
    if content_type == HTTPMimeType.MULTIPART:
        fields = {
            key: _encode_multipart_value(value)
            for key, value in dict(data).items()
            if value is not None
        }
        body, multipart_type = encode_multipart_formdata(fields)
        return body, multipart_type
    if content_type is None or content_type == HTTPMimeType.JSON:
        if isinstance(data, BaseModel):
            return data.model_dump_json(by_alias=True, exclude_unset=True).encode(
                "utf-8",
            ), HTTPMimeType.JSON.value
        if isinstance(data, bytes):
            return data, HTTPMimeType.JSON.value
        return to_json(data, by_alias=True), HTTPMimeType.JSON.value
    if isinstance(data, bytes):
        return data, content_type.value
    if isinstance(data, str):
        return data.encode("utf-8"), content_type.value
    return to_json(data, by_alias=True), content_type.value


def _error_from_response(response: BaseHTTPResponse, url: str) -> RESTError:
    """Map a non-2xx answer to a :class:`RESTError`, keeping the server error when parsable."""
    body: bytes = response.data or b""
    try:
        parsed = RestErrorResponse.model_validate_json(body)
    except ValidationError:
        parsed = None
    if parsed is not None:
        return RESTError(
            parsed.message or f"HTTP {response.status}",
            status=response.status,
            name=parsed.name or "API error",
            url=url,
            response=parsed,
            body=body,
        )
    text = body.decode("utf-8", errors="replace") if body else "No response body"
    return RESTError(text, status=response.status, name="API error", url=url, body=body)


def RESTRequest(request: RESTRequestType) -> ModelResponseHttp:
    """Perform the call and return the raw response.

    Raises:
        RESTError: connection failure, non-2xx status, or invalid JSON body.

    """
    rest_model = request.rest_model
    rest_config = resolve_rest_config(request.rest_config)
    url = RESTUrl(request, rest_config)
    headers: dict[str, str] = dict(rest_config.headers)
    if request.headers:
        headers.update(request.headers)
    if not rest_model.token_in_url:
        if rest_config.token is not None:
            headers["Authorization"] = f"Bearer {rest_config.token}"
        elif rest_config.token_api is not None:
            headers["Authorization"] = f"ApiKey {rest_config.token_api}"
    if rest_model.accept is not None:
        headers["Accept"] = rest_model.accept.value
    body: bytes | None = None
    if rest_model.request_type not in {
        HTTPRequestModel.GET,
        HTTPRequestModel.HEAD,
        HTTPRequestModel.ARCHIVE,
        HTTPRequestModel.RESTORE,
    }:
        body, content_type = _encode_body(rest_model, request.data)
        if content_type is not None:
            headers["Content-Type"] = content_type
    logger.debug("%s %s", rest_model.request_type.value, url)
    try:
        response = _get_http_pool().request(
            rest_model.request_type.value,
            url,
            body=body,
            headers=headers,
            retries=Retry(total=rest_config.retries),
            timeout=Timeout(
                connect=rest_config.connect_timeout,
                read=rest_config.read_timeout,
            ),
        )
    except Urllib3HTTPError as ex:
        logger.exception(
            "Request failed after %d retries: %s %s",
            rest_config.retries,
            rest_model.request_type.value,
            url,
        )
        raise RESTError(
            str(ex),
            status=STATUS_CONNECTION_FAILED,
            name="Request fail",
            url=url,
        ) from ex
    if not _HTTP_SUCCESS_MIN <= response.status <= _HTTP_SUCCESS_MAX:
        raise _error_from_response(response, url)
    # urllib3 hands back a case-insensitive mapping: keeping it is what makes
    # `X-Total-Count` still readable when a hop lowercases the header names.
    response_headers: Mapping[str, str] = response.headers
    raw: bytes = response.data or b""
    content_type_header = response.headers.get("Content-Type", "")
    if HTTPMimeType.JSON.value in content_type_header:
        if not raw:
            return ModelResponseHttp(
                status=response.status,
                data=None,
                headers=response_headers,
                url=url,
            )
        try:
            return ModelResponseHttp(
                status=response.status,
                data=json.loads(raw),
                headers=response_headers,
                url=url,
            )
        except json.JSONDecodeError as ex:
            message = f"REST parse json fail: {ex}"
            raise RESTError(
                message,
                status=STATUS_INVALID_JSON,
                name="API serialization error",
                url=url,
                body=raw,
            ) from ex
    return ModelResponseHttp(
        status=response.status,
        data=raw,
        headers=response_headers,
        url=url,
    )


def _validate(url: str, data: Any, type_: Any) -> Any:
    """Validate ``data`` against ``type_`` or raise the 950 error."""
    try:
        return _adapter(type_).validate_python(data)
    except ValidationError as ex:
        message = f"REST fail to verify the data: {ex}"
        raise RESTError(
            message,
            status=STATUS_MODEL_CHECK_FAILED,
            name="Model check fail",
            url=url,
        ) from ex


@overload
def RESTRequestJson[T](request: RESTRequestType, type_: type[T]) -> T: ...


@overload
def RESTRequestJson(request: RESTRequestType, type_: Any) -> Any: ...


def RESTRequestJson(request: RESTRequestType, type_: Any) -> Any:
    """Perform the call and validate the JSON answer as ``type_``.

    ``type_`` is anything Pydantic can validate: a generated model, ``list[Model]``,
    ``dict[str, Model]``, an enum, ``int``, or an annotated alias such as ``ObjectId``.
    The overloads are the ones ``pydantic.TypeAdapter`` declares: a plain class keeps
    the answer typed, an alias falls back to ``Any`` — which the generated methods
    narrow again through their own return annotation.

    Raises:
        RESTError: see :func:`RESTRequest`, plus 950 when the body does not match ``type_``.

    """
    response = RESTRequest(request)
    return _validate(response.url, response.data, type_)


@overload
def RESTRequestPaginatedJson[T](
    request: RESTRequestType,
    item_type: type[T],
    *,
    offset: int | None = None,
    limit: int | None = None,
) -> Pagination[T]: ...


@overload
def RESTRequestPaginatedJson(
    request: RESTRequestType,
    item_type: Any,
    *,
    offset: int | None = None,
    limit: int | None = None,
) -> Pagination[Any]: ...


def RESTRequestPaginatedJson(
    request: RESTRequestType,
    item_type: Any,
    *,
    offset: int | None = None,
    limit: int | None = None,
) -> Pagination[Any]:
    """Perform a ``Pagination<T>`` call: page headers out, list body + count headers in.

    Raises:
        RESTError: see :func:`RESTRequestJson`, plus 951 when the body is not a list.

    """
    headers: dict[str, str] = dict(request.headers or {})
    if offset is not None:
        headers["X-Pagination-Offset"] = str(offset)
    if limit is not None:
        headers["X-Pagination-Limit"] = str(limit)
    paginated = RESTRequestType(
        rest_model=request.rest_model,
        rest_config=request.rest_config,
        data=request.data,
        params=request.params,
        queries=request.queries,
        headers=headers,
    )
    response = RESTRequest(paginated)
    url = response.url
    if not isinstance(response.data, list):
        message = "Expected an array body for paginated endpoint"
        raise RESTError(
            message,
            status=STATUS_PAGINATION_SHAPE,
            name="Pagination shape error",
            url=url,
        )
    # `list[item_type]` is a runtime value, not a type expression: keeping it in a
    # variable is what tells the type checkers to stop reading it as one.
    items_type: Any = list[item_type]
    items = cast("list[Any]", _validate(url, response.data, items_type))
    total_header = response.headers.get("X-Total-Count")
    link_header = response.headers.get("Link")
    return Pagination(
        items=items,
        total=int(total_header) if total_header is not None else len(items),
        offset=offset if offset is not None else 0,
        limit=limit if limit is not None else (len(items) or 1),
        has_next=link_header is not None and 'rel="next"' in link_header,
        has_prev=link_header is not None and 'rel="prev"' in link_header,
        link_header=link_header,
    )


def RESTRequestBytes(request: RESTRequestType) -> bytes:
    """Perform the call and return the raw body (non-JSON ``@Produces``)."""
    response = RESTRequest(request)
    if isinstance(response.data, bytes):
        return response.data
    return to_json(response.data)


def RESTRequestVoid(request: RESTRequestType) -> None:
    """Perform a call whose answer body is ignored."""
    RESTRequest(request)
