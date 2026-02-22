package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

public class TestTypeScriptApiGenerationAPIInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestTypeScriptApiGenerationAPIInterface.class);

	public enum TestEnum {
		PLOP, PLIP
	}

	public interface SampleResourceGetInterface {
		@GET
		Long gets(@QueryParam("values") final List<TestEnum> values);
	}

	@Path("resourcePath")
	public class SampleResourceGet implements SampleResourceGetInterface {
		@Override
		public Long gets(final List<TestEnum> values) {
			return 0L;
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
		// Assertions.assertEquals(15, generation.size());
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
					Long,
					TestEnum,
					isLong,
				} from "../model";

				export namespace SampleResourceGet {
					export function gets({
							restConfig,
							queries,
						}: {
						restConfig: RESTConfig,
						queries: {
							values?: TestEnum[],
						},
					}): Promise<Long> {
						return RESTRequestJson({
							restModel: {
								endPoint: "resourcePath/",
								requestType: HTTPRequestModel.GET,
							},
							restConfig,
							queries,
						}, isLong);
					};
				}
				""", generation.get(Paths.get("api/sample-resource-get.ts")));
	}

}
