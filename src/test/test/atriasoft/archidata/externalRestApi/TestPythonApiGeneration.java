package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiDoc;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.annotation.method.PaginationContext;
import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.PythonGenerateApi;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Python generation: the produced package is also written under
 * {@code target/generated-python-sample/sample_api} so it can be checked with a real
 * Python interpreter (import, mypy, ruff).
 */
public class TestPythonApiGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestPythonApiGeneration.class);

	public enum SampleStatus {
		TODO, DOING, DONE
	}

	@ApiDoc(description = "A sample entity with every kind of field")
	public static class SampleEntity extends OIDGenericDataSoftDelete {
		@ApiDoc(description = "Display name")
		@NotNull(groups = GroupCreate.class)
		@Size(min = 1, max = 64)
		public String name;
		@Min(0)
		@Max(100)
		public Integer percent;
		public Long bigCounter;
		public Double ratio;
		public boolean enabled;
		public SampleStatus status;
		public List<@NotNull ObjectId> childIds;
		public List<String> tags;
		public Map<String, Double> measures;
		public Map<SampleStatus, Integer> countByStatus;
		public java.util.Date lastSeenAt;
		public java.time.LocalDate day;
		public java.util.UUID externalId;
		@Null(groups = GroupCreate.class)
		public Long serverOnly;
		public SampleNode tree;
	}

	public static class SampleNode {
		public String label;
		public List<SampleNode> children;
		public SampleLeaf leaf;
	}

	public static class SampleLeaf {
		public String value;
		// import cycle: SampleLeaf -> SampleNode -> SampleLeaf
		public SampleNode back;
	}

	@Path("/samples/{entity}")
	@Produces(MediaType.APPLICATION_JSON)
	public static class SampleResource {
		@GET
		@Path("{oid}")
		@ApiDoc(description = "Get one sample")
		public SampleEntity get(@PathParam("entity") final String entity, @PathParam("oid") final ObjectId oid) {
			return null;
		}

		@GET
		public List<@NotNull SampleEntity> gets(
				@PathParam("entity") final String entity,
				@QueryParam("status") final SampleStatus status,
				@QueryParam("tags") final List<String> tags) {
			return null;
		}

		@GET
		@Path("paged")
		public Pagination<SampleEntity> paged(
				@PathParam("entity") final String entity,
				@PaginationContext final org.atriasoft.archidata.dataAccess.model.PaginationContext page) {
			return null;
		}

		// pagination carried by the resource's own query params (no @PaginationContext)
		@GET
		@Path("paged-query")
		public Pagination<SampleEntity> pagedQuery(
				@PathParam("entity") final String entity,
				@QueryParam("offset") final Long offset,
				@QueryParam("limit") final Long limit) {
			return null;
		}

		@POST
		@Consumes(MediaType.APPLICATION_JSON)
		public SampleEntity create(
				@PathParam("entity") final String entity,
				@Valid @ValidGroup(GroupCreate.class) final SampleEntity data) {
			return null;
		}

		@PUT
		@Path("{oid}")
		@Consumes(MediaType.APPLICATION_JSON)
		public SampleEntity update(
				@PathParam("entity") final String entity,
				@PathParam("oid") final ObjectId oid,
				@Valid @ValidGroup(GroupUpdate.class) final SampleEntity data) {
			return null;
		}

		@PATCH
		@Path("{oid}")
		@Consumes(MediaType.APPLICATION_JSON)
		public SampleEntity patch(
				@PathParam("entity") final String entity,
				@PathParam("oid") final ObjectId oid,
				@Valid @ValidGroup(GroupUpdate.class) final SampleEntity data) {
			return null;
		}

		@POST
		@Path("{oid}/change-status")
		public SampleEntity changeStatus(
				@PathParam("entity") final String entity,
				@PathParam("oid") final ObjectId oid,
				@QueryParam("status") final SampleStatus status) {
			return null;
		}

		@DELETE
		@Path("{oid}")
		public void remove(@PathParam("entity") final String entity, @PathParam("oid") final ObjectId oid) {}

		@GET
		@Path("count")
		public Long count(@PathParam("entity") final String entity) {
			return 0L;
		}

		@GET
		@Path("export")
		@Produces("text/csv")
		public String export(@PathParam("entity") final String entity) {
			return "";
		}
	}

	@Path("health")
	public static class HealthResource {
		@GET
		public Boolean isAlive() {
			return true;
		}
	}

	private static Map<java.nio.file.Path, String> generate() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResource.class, HealthResource.class));
		final Map<java.nio.file.Path, String> generation = PythonGenerateApi.generateApi(api);
		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		return generation;
	}

	@Test
	public void testPackageLayout() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		for (final String expected : List.of("__init__.py", "py.typed", "rest_tools.py", "client.py", "api/__init__.py",
				"api/sample_resource.py", "api/health_resource.py", "model/__init__.py", "model/sample_entity.py",
				"model/sample_status.py", "model/sample_node.py", "model/sample_leaf.py", "model/object_id.py",
				"model/oid_generic_data_soft_delete.py", "model/rest_error_response.py")) {
			Assertions.assertTrue(generation.containsKey(Paths.get(expected)), "missing generated file " + expected);
		}
		// native types have no file
		Assertions.assertFalse(generation.containsKey(Paths.get("model/str.py")));
		Assertions.assertFalse(generation.containsKey(Paths.get("model/int.py")));
		// also write the package for the Python-side checks (see doc/python_api_generation.md)
		final java.nio.file.Path out = Paths.get("target", "generated-python-sample", "sample_api");
		Files.createDirectories(out);
		PythonGenerateApi.generateApi(api(), out, true);
		Assertions.assertTrue(Files.exists(out.resolve("__init__.py")));
	}

	private static AnalyzeApi api() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResource.class, HealthResource.class));
		return api;
	}

	@Test
	public void testModelVariants() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		final String entity = generation.get(Paths.get("model/sample_entity.py"));
		LOGGER.info("sample_entity.py:\n{}", entity);
		Assertions.assertTrue(entity.startsWith("\"\"\"SampleEntity models (auto-generated code).\"\"\""));
		// read, create and update variants, extending the same variant of the parent
		Assertions.assertTrue(entity.contains("class SampleEntity(OIDGenericDataSoftDelete):"));
		Assertions.assertTrue(entity.contains("class SampleEntityCreate(OIDGenericDataSoftDeleteCreate):"));
		Assertions.assertTrue(entity.contains("class SampleEntityUpdate(OIDGenericDataSoftDeleteUpdate):"));
		Assertions.assertTrue(entity.contains(
				"from .oid_generic_data_soft_delete import (\n    OIDGenericDataSoftDelete,\n    OIDGenericDataSoftDeleteCreate,\n    OIDGenericDataSoftDeleteUpdate,\n)\n"));
		// class description
		Assertions.assertTrue(entity.contains("\"\"\"A sample entity with every kind of field.\"\"\""));
		Assertions
				.assertTrue(entity.contains("\"\"\"A sample entity with every kind of field (Create variant).\"\"\""));
		// snake_case + alias, description, constraints
		Assertions
				.assertTrue(entity.contains("    big_counter: int | None = Field(default=None, alias=\"bigCounter\")"));
		Assertions.assertTrue(entity.contains("    percent: int | None = Field(default=None, ge=0, le=100)"));
		// @NotNull(groups = GroupCreate) => required in Create, optional elsewhere
		Assertions.assertTrue(
				entity.contains("    name: str = Field(description=\"Display name\", min_length=1, max_length=64)"));
		Assertions.assertTrue(entity.contains(
				"    name: str | None = Field(\n        default=None,\n        description=\"Display name\",\n        min_length=1,\n        max_length=64,\n    )"));
		// primitive => required
		Assertions.assertTrue(entity.contains("    enabled: bool\n"));
		// @Null(groups = GroupCreate) => absent from the Create variant only
		Assertions.assertEquals(2, entity.split("server_only").length - 1);
		// collections, maps, enums, dates, uuid
		Assertions.assertTrue(
				entity.contains("    child_ids: list[ObjectId] | None = Field(default=None, alias=\"childIds\")"));
		Assertions.assertTrue(entity.contains("    tags: list[str | None] | None = None"));
		Assertions.assertTrue(entity.contains("    measures: dict[str, float | None] | None = None"));
		Assertions.assertTrue(entity.contains("    count_by_status: dict[SampleStatus, int | None] | None = Field("));
		Assertions.assertTrue(
				entity.contains("    last_seen_at: datetime | None = Field(default=None, alias=\"lastSeenAt\")"));
		Assertions.assertTrue(entity.contains("    day: date | None = None"));
		Assertions.assertTrue(
				entity.contains("    external_id: uuid.UUID | None = Field(default=None, alias=\"externalId\")"));
		Assertions.assertTrue(entity.contains("from datetime import date, datetime\nimport uuid\n"));
		Assertions.assertTrue(entity.contains("from .sample_status import SampleStatus\n"));
	}

	@Test
	public void testEnumAndAlias() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		Assertions.assertEquals("""
				\"\"\"SampleStatus enumeration (auto-generated code).\"\"\"

				from enum import StrEnum


				class SampleStatus(StrEnum):
				    \"\"\"SampleStatus enumeration.\"\"\"

				    DOING = "DOING"
				    DONE = "DONE"
				    TODO = "TODO"
				""", generation.get(Paths.get("model/sample_status.py")));
		Assertions.assertEquals("""
				\"\"\"ObjectId type alias (auto-generated code).\"\"\"

				from typing import Annotated

				from pydantic import Field


				type ObjectId = Annotated[
				    str,
				    Field(pattern=r"^[a-fA-F0-9]{24}$", description="MongoDB ObjectId"),
				]
				""", generation.get(Paths.get("model/object_id.py")));
	}

	@Test
	public void testImportCycleIsBroken() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		final String node = generation.get(Paths.get("model/sample_node.py"));
		final String leaf = generation.get(Paths.get("model/sample_leaf.py"));
		final String index = generation.get(Paths.get("model/__init__.py"));
		LOGGER.info("sample_node.py:\n{}", node);
		LOGGER.info("sample_leaf.py:\n{}", leaf);
		// exactly one side of the cycle imports lazily, and the index rebuilds it
		final boolean nodeDeferred = node.contains("if TYPE_CHECKING:\n    from .sample_leaf import SampleLeaf");
		final boolean leafDeferred = leaf.contains("if TYPE_CHECKING:\n    from .sample_node import SampleNode");
		Assertions.assertTrue(nodeDeferred ^ leafDeferred, "one deferred import expected:\n" + node + "\n" + leaf);
		Assertions
				.assertTrue(index.contains(nodeDeferred ? "SampleNode.model_rebuild()" : "SampleLeaf.model_rebuild()"));
		// self reference works through postponed annotations
		Assertions.assertTrue(node.contains("from __future__ import annotations"));
		Assertions.assertTrue(node.contains("    children: list[SampleNode | None] | None = None"));
	}

	@Test
	public void testApiClass() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		final String api = generation.get(Paths.get("api/sample_resource.py"));
		LOGGER.info("sample_resource.py:\n{}", api);
		Assertions.assertTrue(api.contains("class SampleResourceApi:"));
		Assertions.assertTrue(api.contains("def __init__(self, rest_config: RESTConfigSource) -> None:"));
		// GET one: positional path parameters in path order, typed answer
		Assertions.assertTrue(api.contains("""
				    def get(
				        self,
				        entity: str,
				        oid: ObjectId,
				    ) -> SampleEntity:
				        \"\"\"Get one sample.
				"""));
		Assertions.assertTrue(api.contains("""
				        return RESTRequestJson(
				            RESTRequestType(
				                rest_model=RESTModel(
				                    end_point="/samples/{entity}/{oid}",
				                    request_type=HTTPRequestModel.GET,
				                    accept=HTTPMimeType.JSON,
				                ),
				                rest_config=self._rest_config,
				                params={
				                    "entity": entity,
				                    "oid": oid,
				                },
				            ),
				            SampleEntity,
				        )
				"""));
		// list with keyword-only optional queries
		Assertions.assertTrue(api.contains("""
				    def gets(
				        self,
				        entity: str,
				        *,
				        status: SampleStatus | None = None,
				        tags: list[str | None] | None = None,
				    ) -> list[SampleEntity]:
				"""));
		Assertions.assertTrue(api.contains("""
				                queries={
				                    "status": status,
				                    "tags": tags,
				                },
				"""));
		// create / update / patch use the matching variant; patch also accepts a dict
		Assertions.assertTrue(api.contains("        data: SampleEntityCreate,\n    ) -> SampleEntity:"));
		Assertions.assertTrue(api.contains("        data: SampleEntityUpdate,\n    ) -> SampleEntity:"));
		Assertions
				.assertTrue(api.contains("        data: SampleEntityUpdate | dict[str, Any],\n    ) -> SampleEntity:"));
		Assertions.assertTrue(api.contains("                    content_type=HTTPMimeType.JSON,\n"));
		Assertions.assertTrue(api.contains("                data=data,\n"));
		// pagination
		Assertions.assertTrue(api.contains("""
				    def paged(
				        self,
				        entity: str,
				        *,
				        offset: int | None = None,
				        limit: int | None = None,
				    ) -> Pagination[SampleEntity]:
				"""));
		Assertions.assertTrue(api.contains("        return RESTRequestPaginatedJson(\n"));
		Assertions.assertTrue(
				api.contains("            SampleEntity,\n            offset=offset,\n            limit=limit,\n"));
		// void, native and raw answers
		Assertions.assertTrue(api.contains("    ) -> None:\n        \"\"\"Remove.\n"));
		Assertions.assertTrue(api.contains("        RESTRequestVoid(\n"));
		Assertions.assertTrue(api.contains("    ) -> int:\n        \"\"\"Count.\n"));
		Assertions.assertTrue(api.contains("            int,\n        )\n"));
		Assertions.assertTrue(api.contains("    ) -> bytes:\n        \"\"\"Export.\n"));
		Assertions.assertTrue(api.contains("        return RESTRequestBytes(\n"));
		// method name in snake_case
		Assertions.assertTrue(api.contains("    def change_status(\n"));
		// imports
		Assertions.assertTrue(api.contains(
				"from ..model import (\n    ObjectId,\n    SampleEntity,\n    SampleEntityCreate,\n    SampleEntityUpdate,\n    SampleStatus,\n)\n"));
		Assertions.assertTrue(api
				.contains("from ..rest_tools import (\n    HTTPMimeType,\n    HTTPRequestModel,\n    Pagination,\n"));
	}

	@Test
	public void testPaginationQueryParametersAreNotDuplicated() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		final String api = generation.get(Paths.get("api/sample_resource.py"));
		// the resource declares offset / limit itself: they are reused, not declared twice
		Assertions.assertTrue(api.contains("""
				    def paged_query(
				        self,
				        entity: str,
				        *,
				        limit: int | None = None,
				        offset: int | None = None,
				    ) -> Pagination[SampleEntity]:
				"""), api);
		Assertions.assertTrue(api.contains("""
				                queries={
				                    "limit": limit,
				                    "offset": offset,
				                },
				            ),
				            SampleEntity,
				            offset=offset,
				            limit=limit,
				        )
				"""), api);
	}

	@Test
	public void testClientAndIndexes() throws Exception {
		final Map<java.nio.file.Path, String> generation = generate();
		Assertions.assertEquals("""
				\"\"\"Aggregated client of the server API (auto-generated code).\"\"\"

				from .api import (
				    HealthResourceApi,
				    SampleResourceApi,
				)
				from .rest_tools import RESTConfigSource


				class ApiClient:
				    \"\"\"One object holding every resource client, sharing one configuration.

				    Example::

				        client = ApiClient(RESTConfig(server="https://my.server/api", token_api=KEY))
				        client.health_resource.<method>(...)

				    Pass a callable instead of a ``RESTConfig`` when the token changes at runtime:
				    it is called before every request.
				    \"\"\"

				    def __init__(self, rest_config: RESTConfigSource) -> None:
				        \"\"\"Bind every resource client to ``rest_config``.\"\"\"
				        self.rest_config = rest_config
				        self.health_resource = HealthResourceApi(rest_config)
				        self.sample_resource = SampleResourceApi(rest_config)
				""", generation.get(Paths.get("client.py")));
		Assertions.assertEquals("""
				\"\"\"Clients of the server resources (auto-generated code).\"\"\"

				from .health_resource import HealthResourceApi
				from .sample_resource import SampleResourceApi


				__all__ = [
				    "HealthResourceApi",
				    "SampleResourceApi",
				]
				""", generation.get(Paths.get("api/__init__.py")));
		final String modelIndex = generation.get(Paths.get("model/__init__.py"));
		Assertions.assertTrue(modelIndex
				.contains("from .sample_entity import SampleEntity, SampleEntityCreate, SampleEntityUpdate\n"));
		Assertions.assertTrue(modelIndex.contains("    \"SampleStatus\",\n"));
		Assertions.assertTrue(modelIndex.contains("    \"ObjectId\",\n"));
	}
}
