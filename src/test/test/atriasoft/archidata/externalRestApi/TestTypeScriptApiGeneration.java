package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.annotation.method.CALL;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

public class TestTypeScriptApiGeneration {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestTypeScriptApiGeneration.class);

	public enum TestEnum {
		PLOP, PLIP
	}

	public class TestObject extends OIDGenericDataSoftDelete {
		public Long value;
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
				""", generation.get(Paths.get("api/sample-resource-get")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";

				import {ZodLong} from "./long";
				import {ZodOIDGenericDataSoftDelete} from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					value: ZodLong.optional(),

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
		//Assertions.assertEquals(15, generation.size());
		//		Assertions.assertEquals("""
		//				/**
		//				 * Interface of the server (auto-generated code)
		//				 */
		//				import {
		//					HTTPRequestModel,
		//					RESTConfig,
		//					RESTRequestJson,
		//				} from "../rest-tools";
		//
		//				import {
		//					TestObject,
		//					TestObjectCreate,
		//					isTestObject,
		//				} from "../model";
		//
		//				export namespace SampleResourcePost {
		//
		//					export function create({
		//							restConfig,
		//							data,
		//						}: {
		//						restConfig: RESTConfig,
		//						data: TestObjectCreate,
		//					}): Promise<TestObject> {
		//						return RESTRequestJson({
		//							restModel: {
		//								endPoint: "resourcePath/",
		//								requestType: HTTPRequestModel.POST,
		//							},
		//							restConfig,
		//							data,
		//						}, isTestObject);
		//					};
		//				}
		//				""", generation.get(Paths.get("api/sample-resource-post")));
		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";

				import {ZodLong} from "./long";
				import {ZodOIDGenericDataSoftDelete} from "./oid-generic-data-soft-delete";

				export const ZodTestObject = ZodOIDGenericDataSoftDelete.extend({
					value: ZodLong.optional(),

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
}
