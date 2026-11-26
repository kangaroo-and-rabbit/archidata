package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAsyncType;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * An endpoint that answers one of several classes ({@code @ApiAsyncType({A.class, B.class})}).
 */
public class TestTypeScriptUnionAnswer {

	public static class Cat {
		public String name;
	}

	public static class Dog {
		public Long age;
	}

	@Path("animals")
	public class AnimalResource {
		@GET
		@ApiAsyncType({ Cat.class, Dog.class })
		public Response get() {
			return null;
		}
	}

	@Test
	public void testTheAnswerIsCheckedAgainstEveryClass() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(AnimalResource.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api);

		final String data = generation.get(Paths.get("api").resolve("animal-resource.ts"));
		Assertions.assertTrue(data.contains("""
					export const ZodGetTypeReturn = zod.union([
						ZodCat,
						ZodDog,
					]);
					export type GetTypeReturn = zod.infer<typeof ZodGetTypeReturn>;
				"""), data);
		Assertions.assertTrue(data.contains("}): Promise<GetTypeReturn> {"), data);
		// the answer is checked against the union, not against the first class only
		Assertions.assertTrue(data.contains("\t\t}, isGetTypeReturn);"), data);
		Assertions.assertFalse(data.contains(", isCat);"), data);
	}
}
