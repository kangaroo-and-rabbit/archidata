package test.atriasoft.archidata.externalRestApi;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Python generation of the transport side of an endpoint: a multipart upload, a header parameter,
 * several answer types and the files of the package that do not depend on the models.
 * The models, the JSON bodies and the pagination are pinned by {@link TestPythonApiGeneration}.
 */
public class TestPythonApiGenerationTransport {

	public static class Attachment {
		public String fileName;
		public Long size;
	}

	@Path("/files")
	public static class FileResource {
		@POST
		@Path("upload")
		@Consumes(MediaType.MULTIPART_FORM_DATA)
		@Produces(MediaType.APPLICATION_JSON)
		@ApiDoc(description = "Upload a file with its title")
		public ObjectId upload(
				@FormDataParam("file") final InputStream file,
				@FormDataParam("file") final FormDataContentDisposition fileMeta,
				@FormDataParam("title") final String title) {
			return null;
		}

		@GET
		@Path("{oid}")
		@Produces({ MediaType.APPLICATION_JSON, "text/csv" })
		public Attachment get(@PathParam("oid") final ObjectId oid, @HeaderParam("X-Trace") final String trace) {
			return null;
		}

		@GET
		@Path("{oid}/note")
		@Produces(MediaType.TEXT_PLAIN)
		public String note(@PathParam("oid") final ObjectId oid) {
			return null;
		}
	}

	private static Map<java.nio.file.Path, String> generate() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(FileResource.class));
		return PythonGenerateApi.generateApi(api);
	}

	@Test
	public void testTransportEndpoints() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		Assertions.assertEquals("""
				\"""FileResource API client (auto-generated code).\"""

				from ..model import Attachment, ObjectId
				from ..rest_tools import (
				    HTTPMimeType,
				    HTTPRequestModel,
				    MultipartFile,
				    RESTConfigSource,
				    RESTModel,
				    RESTRequestBytes,
				    RESTRequestJson,
				    RESTRequestType,
				)


				class FileResourceApi:
				    \"""Client of the ``FileResource`` endpoints (``/files``).\"""

				    def __init__(self, rest_config: RESTConfigSource) -> None:
				        \"""Bind the client to a server configuration.

				        Args:
				            rest_config: The configuration, or a callable returning it (called
				                before every request, for tokens that change at runtime).

				        \"""
				        self._rest_config = rest_config

				    def get(
				        self,
				        oid: ObjectId,
				        *,
				        x_trace: str,
				        accept: HTTPMimeType = HTTPMimeType.JSON,
				    ) -> Attachment:
				        \"""Get.

				        ``GET /files/{oid}``

				        Args:
				            oid: Path parameter ``{oid}``.
				            x_trace: Header ``X-Trace``.
				            accept: Expected answer type, one of HTTPMimeType.JSON, HTTPMimeType.CSV.

				        Returns:
				            The validated answer (``Attachment``).

				        Raises:
				            RESTError: When the call fails (network, HTTP status or invalid answer).

				        \"""
				        return RESTRequestJson(
				            RESTRequestType(
				                rest_model=RESTModel(
				                    end_point="/files/{oid}",
				                    request_type=HTTPRequestModel.GET,
				                    accept=accept,
				                ),
				                rest_config=self._rest_config,
				                params={
				                    "oid": oid,
				                },
				                headers={
				                    key: value
				                    for key, value in {
				                        "X-Trace": x_trace,
				                    }.items()
				                    if value is not None
				                },
				            ),
				            Attachment,
				        )

				    def note(
				        self,
				        oid: ObjectId,
				    ) -> bytes:
				        \"""Note.

				        ``GET /files/{oid}/note``

				        Args:
				            oid: Path parameter ``{oid}``.

				        Returns:
				            The validated answer (``bytes``).

				        Raises:
				            RESTError: When the call fails (network, HTTP status or invalid answer).

				        \"""
				        return RESTRequestBytes(
				            RESTRequestType(
				                rest_model=RESTModel(
				                    end_point="/files/{oid}/note",
				                    request_type=HTTPRequestModel.GET,
				                ),
				                rest_config=self._rest_config,
				                params={
				                    "oid": oid,
				                },
				            ),
				        )

				    def upload(
				        self,
				        *,
				        file: MultipartFile,
				        title: str,
				    ) -> ObjectId:
				        \"""Upload a file with its title.

				        ``POST /files/upload``

				        Args:
				            file: Multipart field ``file``.
				            title: Multipart field ``title``.

				        Returns:
				            The validated answer (``ObjectId``).

				        Raises:
				            RESTError: When the call fails (network, HTTP status or invalid answer).

				        \"""
				        return RESTRequestJson(
				            RESTRequestType(
				                rest_model=RESTModel(
				                    end_point="/files/upload",
				                    request_type=HTTPRequestModel.POST,
				                    accept=HTTPMimeType.JSON,
				                    content_type=HTTPMimeType.MULTIPART,
				                ),
				                rest_config=self._rest_config,
				                data={
				                    "file": file,
				                    "title": title,
				                },
				            ),
				            ObjectId,
				        )
				""", generation.get(Paths.get("api/file_resource.py")));
	}

	@Test
	public void testRootFiles() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		Assertions.assertEquals("""
				\"""Python client of the server API (auto-generated code).

				Models live in ``.model``, one client class per resource in ``.api``, and
				``ApiClient`` binds them all to one server configuration.
				\"""

				from . import api, model, rest_tools
				from .client import ApiClient
				from .rest_tools import (
				    HTTPMimeType,
				    HTTPRequestModel,
				    ModelResponseHttp,
				    MultipartFile,
				    Pagination,
				    resolve_rest_config,
				    RESTConfig,
				    RESTConfigProvider,
				    RESTConfigSource,
				    RESTError,
				    RESTModel,
				    RESTRequest,
				    RESTRequestBytes,
				    RESTRequestJson,
				    RESTRequestPaginatedJson,
				    RESTRequestType,
				    RESTRequestVoid,
				    RESTUrl,
				)


				__all__ = [
				    "ApiClient",
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
				    "api",
				    "model",
				    "resolve_rest_config",
				    "rest_tools",
				]
				""", generation.get(Paths.get("__init__.py")));
		Assertions.assertEquals("", generation.get(Paths.get("py.typed")));
		Assertions.assertEquals("""
				\"""Attachment models (auto-generated code).\"""

				from __future__ import annotations

				from pydantic import BaseModel, ConfigDict, Field


				class Attachment(BaseModel):
				    \"""Attachment model.\"""

				    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())

				    file_name: str | None = Field(default=None, alias="fileName")
				    size: int | None = None
				""", generation.get(Paths.get("model/attachment.py")));
	}

	@Test
	public void testPackageFileList() throws Exception {
		final java.util.Set<String> names = new java.util.TreeSet<>();
		for (final java.nio.file.Path elem : generate().keySet()) {
			names.add(elem.toString());
		}
		Assertions
				.assertTrue(
						names.containsAll(List.of("__init__.py", "py.typed", "rest_tools.py", "client.py",
								"api/__init__.py", "api/file_resource.py", "model/__init__.py", "model/attachment.py",
								"model/object_id.py", "model/rest_error_response.py", "model/rest_input_error.py")),
						names.toString());
	}

	@Test
	public void testPackageIsWrittenAndObsoleteFilesAreRemoved() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(FileResource.class));
		final java.nio.file.Path dir = Paths.get("target", "generated-python-transport", "transport_api");
		if (Files.exists(dir)) {
			try (final java.util.stream.Stream<java.nio.file.Path> walk = Files.walk(dir)) {
				for (final java.nio.file.Path elem : walk.sorted(java.util.Comparator.reverseOrder()).toList()) {
					Files.delete(elem);
				}
			}
		}
		final java.nio.file.Path stale = dir.resolve("api").resolve("stale_resource.py");
		Files.createDirectories(stale.getParent());
		Files.writeString(stale, "stale\n");
		final java.nio.file.Path foreign = dir.resolve("notes.txt");
		Files.writeString(foreign, "mine\n");
		// without the flag the obsolete file only triggers a warning
		PythonGenerateApi.generateApi(api, dir);
		Assertions.assertTrue(Files.exists(stale));
		// a file already up to date is not rewritten: its modification time stays
		final FileTime past = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
		final java.nio.file.Path client = dir.resolve("client.py");
		Files.setLastModifiedTime(client, past);
		PythonGenerateApi.generateApi(api, dir, true);
		Assertions.assertEquals(past, Files.getLastModifiedTime(client));
		Assertions.assertFalse(Files.exists(stale));
		Assertions.assertTrue(Files.exists(foreign), "only the .py files are candidates");
		final Map<java.nio.file.Path, String> generation = generate();
		for (final Map.Entry<java.nio.file.Path, String> entry : generation.entrySet()) {
			Assertions.assertEquals(entry.getValue(), Files.readString(dir.resolve(entry.getKey())),
					entry.getKey().toString());
		}
	}

	@Test
	public void testSystemDirectoryIsRefused() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(FileResource.class));
		Assertions.assertThrows(java.io.IOException.class, () -> PythonGenerateApi.generateApi(api, Paths.get("/usr")));
		Assertions.assertThrows(java.io.IOException.class, () -> PythonGenerateApi.generateApi(api, Paths.get("/tmp")));
	}
}
