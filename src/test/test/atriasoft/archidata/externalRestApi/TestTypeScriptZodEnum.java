package test.atriasoft.archidata.externalRestApi;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.TsGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * The enums generated as {@code zod.enum([...])} (variable {@code ARCHIDATA_GENERATE_ZOD_ENUM}).
 */
public class TestTypeScriptZodEnum {

	public enum Color {
		RED, GREEN
	}

	@Path("colors")
	public class ColorResource {
		@GET
		public Color get() {
			return null;
		}
	}

	@Test
	public void testZodEnumIsDeclaredUnderItsZodName() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ColorResource.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api, true);

		Assertions.assertEquals("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from "zod";

				export const ZodColor = zod.enum([
					'GREEN',
					'RED',

					]);

				export type Color = zod.infer<typeof ZodColor>;

				export function isColor(data: any): data is Color {
					try {
						ZodColor.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data type='ZodColor' error=${e}`);
						return false;
					}
				}
				""", generation.get(Paths.get("model").resolve("color.ts")));
	}

	@Test
	public void testNativeEnumByDefault() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ColorResource.class));

		final Map<java.nio.file.Path, String> generation = TsGenerateApi.generateApi(api, false);

		final String data = generation.get(Paths.get("model").resolve("color.ts"));
		Assertions.assertTrue(data.contains("export enum Color  {"), data);
		Assertions.assertTrue(data.contains("export const ZodColor = zod.nativeEnum(Color);"), data);
	}
}
