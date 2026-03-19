"""REST tools for Python API client (auto-generated code).

This module provides a generic REST client interface using urllib3.
It supports JSON serialization, authentication, and error handling.

@author Edouard DUPIN
@copyright 2024, Edouard DUPIN, all right reserved
@license MPL-2
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from enum import Enum
from typing import (
    TYPE_CHECKING,
    Any,
    TypeVar,
)

from pydantic import BaseModel
from urllib3 import PoolManager, Retry, Timeout
from urllib3.exceptions import MaxRetryError

if TYPE_CHECKING:
    from collections.abc import Callable

    from urllib3.response import BaseHTTPResponse


__all__ = [
    "HTTPMimeType",
    "HTTPRequestModel",
    "ModelResponseHttp",
    "RESTCallbacks",
    "RESTConfig",
    "RESTError",
    "RESTModel",
    "RESTRequest",
    "RESTRequestType",
    "RESTRequestJson",
    "RESTRequestVoid",
    "RESTUrl",
    "RestErrorResponse",
]

logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)


class HTTPRequestModel(str, Enum):
    """HTTP request method types."""

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


class HTTPMimeType(str, Enum):
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
    """REST client configuration."""

    # Base server URL: http(s)://my.server.org/plop/api/
    server: str
    # Bearer token for authentication
    token: str | None = None
    # API key for authentication
    token_api: str | None = None
    # Connection timeout in seconds
    connect_timeout: float = 5.0
    # Read timeout in seconds
    read_timeout: float = 30.0
    # Number of retries
    retries: int = 3


@dataclass
class RESTModel:
    """REST endpoint model."""

    # Base of the local API request: "sheep/{id}"
    end_point: str
    # Type of the request
    request_type: HTTPRequestModel = HTTPRequestModel.GET
    # Expected response MIME type
    accept: HTTPMimeType | None = None
    # Request body MIME type
    content_type: HTTPMimeType | None = None
    # Whether to pass token in URL instead of header
    token_in_url: bool = False


@dataclass
class ModelResponseHttp:
    """HTTP response model."""

    status: int
    data: Any


@dataclass
class RestErrorResponse(Exception):
    """REST error response."""

    name: str
    time: str
    status: int
    message: str
    status_message: str
    error: str


class RESTError(Exception):
    """REST API Exception."""


# Type alias for progress callback
ProgressCallback = Callable[[int, int], None]


@dataclass
class RESTCallbacks:
    """REST request callbacks for progress tracking."""

    progress_upload: ProgressCallback | None = None
    progress_download: ProgressCallback | None = None


@dataclass
class RESTRequestType:
    """REST request parameters."""

    rest_model: RESTModel
    rest_config: RESTConfig
    data: Any = None
    params: dict[str, Any] | None = None
    queries: dict[str, Any] | None = None
    headers: dict[str, str] | None = None
    callbacks: RESTCallbacks | None = None


# Global HTTP connection pool
_http_pool: PoolManager | None = None


def _get_http_pool() -> PoolManager:
    """Get or create the HTTP connection pool."""
    global _http_pool
    if _http_pool is None:
        _http_pool = PoolManager()
    return _http_pool


def _remove_trailing_slashes(value: str) -> str:
    """Remove trailing slashes from a string."""
    return value.rstrip("/")


def _remove_leading_slashes(value: str) -> str:
    """Remove leading slashes from a string."""
    return value.lstrip("/")


def RESTUrl(request: RESTRequestType) -> str:
    """Generate the full URL for a REST request.

    Args:
        request: The REST request parameters.

    Returns:
        The complete URL string.
    """
    rest_model = request.rest_model
    rest_config = request.rest_config
    params = request.params
    queries = request.queries

    # Build base URL
    generate_url = (
        f"{_remove_trailing_slashes(rest_config.server)}/"
        f"{_remove_leading_slashes(rest_model.end_point)}"
    )

    # Replace path parameters
    if params is not None:
        for key, value in params.items():
            generate_url = generate_url.replace(f"{{{key}}}", str(value))

    # Check if we need query parameters
    if (
        queries is None
        and rest_config.token is None
        and rest_config.token_api is None
    ):
        return generate_url

    if not rest_model.token_in_url and queries is None:
        return generate_url

    # Build query string
    query_parts: list[str] = []

    if queries is not None:
        for key, value in queries.items():
            if isinstance(value, list):
                for item in value:
                    query_parts.append(f"{key}={item}")
            else:
                query_parts.append(f"{key}={value}")

    if rest_model.token_in_url:
        if rest_config.token is not None:
            query_parts.append(f"Authorization=Bearer {rest_config.token}")
        if rest_config.token_api is not None:
            query_parts.append(f"Authorization=ApiKey {rest_config.token_api}")

    if query_parts:
        return f"{generate_url}?{'&'.join(query_parts)}"

    return generate_url


def RESTRequest(request: RESTRequestType) -> ModelResponseHttp:
    """Execute a REST request.

    Args:
        request: The REST request parameters.

    Returns:
        The HTTP response.

    Raises:
        RestErrorResponse: If the request fails.
    """
    from datetime import datetime

    rest_model = request.rest_model
    rest_config = request.rest_config
    headers = dict(request.headers) if request.headers else {}
    data = request.data

    # Generate URL
    generate_url = RESTUrl(request)

    # Set authentication headers
    if not rest_model.token_in_url:
        if rest_config.token is not None:
            headers["Authorization"] = f"Bearer {rest_config.token}"
        elif rest_config.token_api is not None:
            headers["Authorization"] = f"ApiKey {rest_config.token_api}"

    # Set Accept header
    if rest_model.accept is not None:
        headers["Accept"] = rest_model.accept.value

    # Set Content-Type header (except for GET and multipart)
    if rest_model.request_type not in (
        HTTPRequestModel.GET,
        HTTPRequestModel.ARCHIVE,
        HTTPRequestModel.RESTORE,
    ):
        if (
            rest_model.content_type is not None
            and rest_model.content_type != HTTPMimeType.MULTIPART
        ):
            headers["Content-Type"] = rest_model.content_type.value

    # Prepare body
    body: bytes | None = None
    if rest_model.content_type == HTTPMimeType.JSON and data is not None:
        if isinstance(data, BaseModel):
            body = data.model_dump_json(by_alias=True).encode("utf-8")
        else:
            body = json.dumps(data).encode("utf-8")
    elif data is not None:
        if isinstance(data, bytes):
            body = data
        elif isinstance(data, str):
            body = data.encode("utf-8")
        else:
            body = json.dumps(data).encode("utf-8")

    # Execute request
    http = _get_http_pool()
    try:
        response: BaseHTTPResponse = http.request(
            rest_model.request_type.value,
            generate_url,
            body=body,
            headers=headers,
            retries=Retry(rest_config.retries),
            timeout=Timeout(
                connect=rest_config.connect_timeout,
                read=rest_config.read_timeout,
            ),
        )
    except MaxRetryError as ex:
        logger.error("Request failed after %d retries: %s", rest_config.retries, ex)
        raise RestErrorResponse(
            name="Request fail",
            time=datetime.now().isoformat(),
            status=999,
            message=str(ex),
            status_message="Max retries exceeded",
            error="rest_tools.py: Connection failed",
        ) from ex

    # Parse response
    response_data = response.data
    content_type = response.headers.get("Content-Type", "")

    if 200 <= response.status <= 299:
        if HTTPMimeType.JSON.value in content_type:
            try:
                parsed_data = json.loads(response_data)
                return ModelResponseHttp(status=response.status, data=parsed_data)
            except json.JSONDecodeError as ex:
                raise RestErrorResponse(
                    name="API serialization error",
                    time=datetime.now().isoformat(),
                    status=902,
                    message=f"Failed to parse JSON: {ex}",
                    status_message="JSON parse error",
                    error="rest_tools.py: Invalid JSON response",
                ) from ex
        return ModelResponseHttp(status=response.status, data=response_data)

    # Handle error response
    error_message = response_data.decode("utf-8") if response_data else "No response body"
    try:
        error_json = json.loads(response_data)
        if isinstance(error_json, dict) and "message" in error_json:
            error_message = error_json.get("message", error_message)
    except (json.JSONDecodeError, TypeError):
        pass

    raise RestErrorResponse(
        name="API error",
        time=datetime.now().isoformat(),
        status=response.status,
        message=error_message,
        status_message=f"HTTP {response.status}",
        error="rest_tools.py: Server returned error",
    )


def RESTRequestJson(
    request: RESTRequestType,
    model_type: type[T],
    *,
    is_list: bool = False,
) -> T | list[T]:
    """Execute a REST request and parse the response as a Pydantic model.

    Args:
        request: The REST request parameters.
        model_type: The Pydantic model class to parse the response into.
        is_list: Whether to expect a list of models.

    Returns:
        The parsed model instance or list of instances.

    Raises:
        RestErrorResponse: If the request fails or parsing fails.
    """
    from datetime import datetime

    response = RESTRequest(request)

    try:
        if is_list:
            if not isinstance(response.data, list):
                raise RestErrorResponse(
                    name="Model validation error",
                    time=datetime.now().isoformat(),
                    status=950,
                    message="Expected list response",
                    status_message="Type mismatch",
                    error="rest_tools.py: Response is not a list",
                )
            return [model_type.model_validate(item) for item in response.data]
        return model_type.model_validate(response.data)
    except Exception as ex:
        raise RestErrorResponse(
            name="Model validation error",
            time=datetime.now().isoformat(),
            status=950,
            message=str(ex),
            status_message="Validation failed",
            error="rest_tools.py: Failed to validate response model",
        ) from ex


def RESTRequestVoid(request: RESTRequestType) -> None:
    """Execute a REST request that expects no response body.

    Args:
        request: The REST request parameters.

    Raises:
        RestErrorResponse: If the request fails.
    """
    RESTRequest(request)
