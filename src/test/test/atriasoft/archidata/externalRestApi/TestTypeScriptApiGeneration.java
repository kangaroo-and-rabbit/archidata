package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.annotation.method.ARCHIVE;
import org.atriasoft.archidata.annotation.method.CALL;
import org.atriasoft.archidata.annotation.method.RESTORE;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

public class TestTypeScriptApiGeneration {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTypeScriptApiGeneration.class);

	public enum TestEnum {
		PLOP, PLIP
	}

	public class TestObject extends OIDGenericDataSoftDelete {
		public Long valueEmpty;
		@Null()
		public Long valueNullValid;
		@Null(groups = GroupRead.class)
		public Long valueNullGroupRead;
		@Null(groups = GroupUpdate.class)
		public Long valueNullGroupUpdate;
		@Null(groups = GroupCreate.class)
		public Long valueNullGroupCreate;
		@Null(groups = GroupPersistant.class)
		public Long valueNullGroupOther;
		@NotNull()
		public Long valueNotNullValid;
		@NotNull(groups = GroupRead.class)
		public Long valueNotNullGroupRead;
		@NotNull(groups = GroupUpdate.class)
		public Long valueNotNullGroupUpdate;
		@NotNull(groups = GroupCreate.class)
		public Long valueNotNullGroupCreate;
		@NotNull(groups = GroupPersistant.class)
		public Long valueNotNullGroupOther;
	}

	@Path("resourcePath")
	public class SampleResourceGet {
		@GET
		@Path("{oid}")
		public TestObject get(@PathParam("oid") final ObjectId oid) {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourceGetList {
		@GET
		public List<TestObject> gets() {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourcePost {
		@POST
		public TestObject create(@Valid @ValidGroup(GroupCreate.class) final TestObject data) {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourcePut {
		@PUT
		public TestObject update(@Valid @ValidGroup(GroupUpdate.class) final TestObject data) {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourcePatch {
		@PATCH
		public TestObject patch(@Valid @ValidGroup(GroupUpdate.class) final TestObject data) {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourceDelete {
		@DELETE
		public void remove(@PathParam("oid") final ObjectId oid) {}

	}

	@Path("resourcePath")
	public class SampleResourceCall {
		@CALL
		@Path("executionCall")
		public Boolean execute(@QueryParam("helloParam") final String value, final TestEnum parameter) {
			return null;
		}
	}

	@Path("resourcePath")
	public class SampleResourceArchive {
		@ARCHIVE
		public void archive(@PathParam("oid") final ObjectId oid) {}

	}

	@Path("resourcePath")
	public class SampleResourceRestore {
		@RESTORE
		@Path("executionCall")
		public Boolean restore(@PathParam("oid") final ObjectId oid) {
			return null;
		}
	}

	@Test
	public void testGenerateABasicRestApiGet() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourceGet.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		//Assertions.assertEquals(15, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					ObjectId,
					TestObject,
					isTestObject,
				} from "../model";

				export namespace SampleResourceGet {
					export function get({
							restConfig,
							params,
						}: {
						restConfig: RESTConfig,
						params: {
							oid: ObjectId,
						},
					}): Promise<TestObject> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/{oid}",
								requestType: HTTPRequestModel.GET,
							},
							restConfig,
							params,
						}, isTestObject);
					};
				}
				""", generation.get(Paths.get("api/sample-resource-get.ts")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";
				import { ZodLong } from "./long";
				import { ZodOIDGenericDataSoftDelete } from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupUpdate: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong,
					valueNotNullGroupUpdate: ZodLong.optional(),
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObject = zod.infer<typeof ZodTestObject>;

				export function isTestObject(data: any): data is TestObject {
					try {
						ZodTestObject.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObject' error=${e}`);
						return false;
					}
				}
				""", generation.get(Paths.get("model/test-object.ts")));
	}

	@Test
	public void testGenerateABasicRestApiPost() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourcePost.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(15, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					TestObject,
					TestObjectCreate,
					isTestObject,
				} from "../model";

				export namespace SampleResourcePost {
					export function create({
							restConfig,
							data,
						}: {
						restConfig: RESTConfig,
						data: TestObjectCreate,
					}): Promise<TestObject> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.POST,
							},
							restConfig,
							data,
						}, isTestObject);
					};
				}
				""", generation.get(Paths.get("api/sample-resource-post.ts")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";
				import { ZodLong } from "./long";
				import {
					ZodOIDGenericDataSoftDelete,
					ZodOIDGenericDataSoftDeleteCreate,
				} from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupUpdate: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong,
					valueNotNullGroupUpdate: ZodLong.optional(),
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObject = zod.infer<typeof ZodTestObject>;

				export function isTestObject(data: any): data is TestObject {
					try {
						ZodTestObject.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObject' error=${e}`);
						return false;
					}
				}

				export const ZodTestObjectCreate = ZodOIDGenericDataSoftDeleteCreate.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupRead: ZodLong.optional(),
					valueNullGroupUpdate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong.optional(),
					valueNotNullGroupUpdate: ZodLong.optional(),
					valueNotNullGroupCreate: ZodLong,
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObjectCreate = zod.infer<typeof ZodTestObjectCreate>;

				export function isTestObjectCreate(data: any): data is TestObjectCreate {
					try {
						ZodTestObjectCreate.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObjectCreate' error=${e}`);
						return false;
					}
				}
				""", generation.get(Paths.get("model/test-object.ts")));
	}

	@Test
	public void testGenerateABasicRestApiPut() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourcePut.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(15, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					TestObject,
					TestObjectUpdate,
					isTestObject,
				} from "../model";

				export namespace SampleResourcePut {
					export function update({
							restConfig,
							data,
						}: {
						restConfig: RESTConfig,
						data: TestObjectUpdate,
					}): Promise<TestObject> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.PUT,
							},
							restConfig,
							data,
						}, isTestObject);
					};
				}
				""", generation.get(Paths.get("api/sample-resource-put.ts")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";
				import { ZodLong } from "./long";
				import {
					ZodOIDGenericDataSoftDelete,
					ZodOIDGenericDataSoftDeleteUpdate,
				} from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupUpdate: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong,
					valueNotNullGroupUpdate: ZodLong.optional(),
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObject = zod.infer<typeof ZodTestObject>;

				export function isTestObject(data: any): data is TestObject {
					try {
						ZodTestObject.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObject' error=${e}`);
						return false;
					}
				}

				export const ZodTestObjectUpdate = ZodOIDGenericDataSoftDeleteUpdate.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupRead: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong.optional(),
					valueNotNullGroupUpdate: ZodLong,
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObjectUpdate = zod.infer<typeof ZodTestObjectUpdate>;

				export function isTestObjectUpdate(data: any): data is TestObjectUpdate {
					try {
						ZodTestObjectUpdate.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObjectUpdate' error=${e}`);
						return false;
					}
				}
				""", generation.get(Paths.get("model/test-object.ts")));
	}

	@Test
	public void testGenerateABasicRestApiPatch() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourcePatch.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(15, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					TestObject,
					TestObjectUpdate,
					isTestObject,
				} from "../model";

				export namespace SampleResourcePatch {
					export function patch({
							restConfig,
							data,
						}: {
						restConfig: RESTConfig,
						data: Partial<TestObjectUpdate>,
					}): Promise<TestObject> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.PATCH,
							},
							restConfig,
							data,
						}, isTestObject);
					};
				}
				""", generation.get(Paths.get("api/sample-resource-patch.ts")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";
				import { ZodLong } from "./long";
				import {
					ZodOIDGenericDataSoftDelete,
					ZodOIDGenericDataSoftDeleteUpdate,
				} from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupUpdate: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong,
					valueNotNullGroupUpdate: ZodLong.optional(),
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObject = zod.infer<typeof ZodTestObject>;

				export function isTestObject(data: any): data is TestObject {
					try {
						ZodTestObject.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObject' error=${e}`);
						return false;
					}
				}

				export const ZodTestObjectUpdate = ZodOIDGenericDataSoftDeleteUpdate.extend({
					valueEmpty: ZodLong.optional(),
					valueNullGroupRead: ZodLong.optional(),
					valueNullGroupCreate: ZodLong.optional(),
					valueNullGroupOther: ZodLong.optional(),
					valueNotNullValid: ZodLong,
					valueNotNullGroupRead: ZodLong.optional(),
					valueNotNullGroupUpdate: ZodLong,
					valueNotNullGroupCreate: ZodLong.optional(),
					valueNotNullGroupOther: ZodLong.optional(),
				});

				export type TestObjectUpdate = zod.infer<typeof ZodTestObjectUpdate>;

				export function isTestObjectUpdate(data: any): data is TestObjectUpdate {
					try {
						ZodTestObjectUpdate.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodTestObjectUpdate' error=${e}`);
						return false;
					}
				}
				""", generation.get(Paths.get("model/test-object.ts")));
	}

	@Test
	public void testGenerateABasicRestApiDelete() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourceDelete.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(9, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPMimeType,
					HTTPRequestModel,
					RESTConfig,
					RESTRequestVoid,
				} from "../rest-tools";
				import {
					ObjectId,
				} from "../model";

				export namespace SampleResourceDelete {
					export function remove({
							restConfig,
							params,
						}: {
						restConfig: RESTConfig,
						params: {
							oid: ObjectId,
						},
					}): Promise<void> {
						return RESTRequestVoid({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.DELETE,
								contentType: HTTPMimeType.TEXT_PLAIN,
							},
							restConfig,
							params,
						});
					};
				}
				""", generation.get(Paths.get("api/sample-resource-delete.ts")));
	}

	@Test
	public void testGenerateABasicRestApiCall() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourceCall.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(10, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					TestEnum,
				} from "../model";

				export namespace SampleResourceCall {
					export function execute({
							restConfig,
							queries,
							data,
						}: {
						restConfig: RESTConfig,
						queries: {
							helloParam?: string,
						},
						data: TestEnum,
					}): Promise<boolean> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/executionCall",
								requestType: HTTPRequestModel.CALL,
							},
							restConfig,
							queries,
							data,
						});
					};
				}
				""", generation.get(Paths.get("api/sample-resource-call.ts")));
	}

	@Test
	public void testGenerateABasicRestApiArchive() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourceArchive.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(9, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestVoid,
				} from "../rest-tools";
				import {
					ObjectId,
				} from "../model";

				export namespace SampleResourceArchive {
					export function archive({
							restConfig,
							params,
						}: {
						restConfig: RESTConfig,
						params: {
							oid: ObjectId,
						},
					}): Promise<void> {
						return RESTRequestVoid({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.ARCHIVE,
							},
							restConfig,
							params,
						});
					};
				}
				""", generation.get(Paths.get("api/sample-resource-archive.ts")));
	}

	@Test
	public void testGenerateABasicRestApiRestore() throws Exception {

		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(SampleResourceRestore.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		for (final java.nio.file.Path elem : generation.keySet()) {
			LOGGER.info("path= {}", elem);
		}
		Assertions.assertEquals(9, generation.size());
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import {
					HTTPRequestModel,
					RESTConfig,
					RESTRequestJson,
				} from "../rest-tools";
				import {
					ObjectId,
				} from "../model";

				export namespace SampleResourceRestore {
					export function restore({
							restConfig,
							params,
						}: {
						restConfig: RESTConfig,
						params: {
							oid: ObjectId,
						},
					}): Promise<boolean> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/executionCall",
								requestType: HTTPRequestModel.RESTORE,
							},
							restConfig,
							params,
						});
					};
				}
				""", generation.get(Paths.get("api/sample-resource-restore.ts")));
	}
}
