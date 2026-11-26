package org.atriasoft.archidata.externalRestApi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.typescript.TsApiGeneration;
import org.atriasoft.archidata.externalRestApi.typescript.TsClassElement;
import org.atriasoft.archidata.externalRestApi.typescript.TsClassElement.DefinedPosition;
import org.atriasoft.archidata.externalRestApi.typescript.TsClassElementGroup;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

/**
 * Generates TypeScript API client code from {@link AnalyzeApi} introspection data.
 *
 * <p>Produces a folder structure with Zod-based model definitions and API client
 * files that can be compiled with strict TypeScript settings.
 */
public class TsGenerateApi {
	/** Environment variable that switches the enums to {@code zod.enum([...])} when it is set. */
	private static final String ENV_GENERATE_ZOD_ENUM = "ARCHIDATA_GENERATE_ZOD_ENUM";

	/** Private constructor to prevent instantiation of this utility class. */
	private TsGenerateApi() {
		// Utility class
	}

	/**
	 * Generate a full API tree for Typescript in a specific folder. This
	 * generate a folder containing a full API with "model" folder and "api"
	 * folder. The generation depend of Zod and can be strict compile.
	 * Unchanged files are not rewritten. Obsolete .ts files trigger a warning.
	 *
	 * @param api the analyzed API model to generate from
	 * @param pathPackage the directory path to write the generated files into
	 * @throws Exception if generation or file writing fails
	 */
	public static void generateApi(final AnalyzeApi api, final Path pathPackage) throws Exception {
		generateApi(api, pathPackage, false);
	}

	/**
	 * Generate a full API tree for Typescript in a specific folder.
	 * Unchanged files are not rewritten.
	 *
	 * @param api the analyzed API model to generate from
	 * @param pathPackage the directory path to write the generated files into
	 * @param deleteObsoleteFiles if true, delete obsolete .ts files; if false, log a warning
	 * @throws Exception if generation or file writing fails
	 */
	public static void generateApi(final AnalyzeApi api, final Path pathPackage, final boolean deleteObsoleteFiles)
			throws Exception {
		final Map<Path, String> generation = generateApi(api);
		GenerationWriter.writeGeneratedFiles(generation, pathPackage, ".ts", deleteObsoleteFiles);
	}

	/**
	 * Generates TypeScript API client code as an in-memory map of file paths to content.
	 * @param api the analyzed API model to generate from
	 * @return a map of relative file paths to their generated TypeScript content
	 * @throws Exception if generation fails
	 */
	public static final Map<Path, String> generateApi(final AnalyzeApi api) throws Exception {
		return generateApi(api, System.getenv(ENV_GENERATE_ZOD_ENUM) != null);
	}

	/**
	 * Generates TypeScript API client code as an in-memory map of file paths to content.
	 * @param api the analyzed API model to generate from
	 * @param zodEnum true to generate the enums as {@code zod.enum([...])}, false for TypeScript enums
	 * (the default of {@link #generateApi(AnalyzeApi)} follows the {@code ARCHIDATA_GENERATE_ZOD_ENUM} variable)
	 * @return a map of relative file paths to their generated TypeScript content
	 * @throws Exception if generation fails
	 */
	public static final Map<Path, String> generateApi(final AnalyzeApi api, final boolean zodEnum) throws Exception {
		final Map<Path, String> generation = new HashMap<>();
		final List<TsClassElement> localModel = generateApiModel(api);
		final TsClassElementGroup tsGroup = new TsClassElementGroup(localModel, zodEnum);

		// -----------------------------------------------------------
		// -- Generate The API
		// -----------------------------------------------------------
		for (final ApiGroupModel element : api.apiModels) {
			TsApiGeneration.generateApiFile(element, tsGroup, generation);
		}
		// Generate index of model files
		createResourceIndex(api.apiModels, generation);
		createIndex(generation);

		// -----------------------------------------------------------
		// -- Generates all MODEL files & model requested by API:
		// -----------------------------------------------------------
		for (final TsClassElement element : localModel) {
			element.generateFile(tsGroup, generation);
		}
		// Generate index of model files
		createModelIndex(tsGroup, generation);

		// -----------------------------------------------------------
		// -- copy back-end engine
		// -----------------------------------------------------------
		copyResourceFile("rest-tools.ts", Paths.get("rest-tools.ts"), generation);
		return generation;
	}

	private static void createIndex(final Map<Path, String> generation) {
		final String out = """
				/**
				 * Interface of the server (auto-generated code)
				 */
				export * from \"./model\";
				export * from \"./api\";
				export * from \"./rest-tools\";

				""";
		generation.put(Paths.get("index.ts"), out.toString());
	}

	private static void createResourceIndex(final List<ApiGroupModel> apiModels, final Map<Path, String> generation) {
		final StringBuilder out = new StringBuilder("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				""");
		final List<String> files = new ArrayList<>();
		for (final ApiGroupModel elem : apiModels) {
			files.add(TsClassElement.determineFileName(elem.name));
		}
		Collections.sort(files);
		for (final String elem : files) {
			out.append("export * from \"./");
			out.append(elem);
			out.append("\"\n");
		}
		generation.put(Paths.get("api").resolve("index.ts"), out.toString());
	}

	private static void createModelIndex(final TsClassElementGroup tsGroup, final Map<Path, String> generation) {
		final StringBuilder out = new StringBuilder("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				""");
		final List<String> files = new ArrayList<>();
		for (final TsClassElement elem : tsGroup.getTsElements()) {
			if (elem.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			files.add(elem.fileName);
		}
		Collections.sort(files);
		for (final String elem : files) {
			out.append("export * from \"./");
			out.append(elem);
			out.append("\"\n");
		}
		generation.put(Paths.get("model").resolve("index.ts"), out.toString());
	}

	private static List<TsClassElement> generateApiModel(final AnalyzeApi api) throws Exception {
		// needed for Rest interface
		api.addModel(RestErrorResponse.class);
		// First step is to add all specific basic elements the wrap correctly the model
		final List<TsClassElement> tsModels = new ArrayList<>();
		GeneratedModels.addNative(api, tsModels, List.of(Void.class, void.class),
				models -> new TsClassElement(models, "void", "void", null, null, DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, tsModels, List.of(Object.class), models -> new TsClassElement(models,
				"zod.any()", "object", null, "zod.object()", DefinedPosition.NATIVE));
		// Map is binded to any ==> can not determine this complex model for now
		GeneratedModels.addNative(api, tsModels, List.of(Map.class),
				models -> new TsClassElement(models, "zod.any()", "any", null, null, DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, tsModels, List.of(String.class), models -> new TsClassElement(models,
				"zod.string()", "string", null, "zod.string()", DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, tsModels,
				List.of(InputStream.class, FormDataContentDisposition.class, ContentDisposition.class),
				models -> new TsClassElement(models, "z.instanceof(File)", "File", null, "z.instanceof(File)",
						DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, tsModels, List.of(Boolean.class, boolean.class),
				models -> new TsClassElement(models, "zod.boolean()", "boolean", null, "zod.boolean()",
						DefinedPosition.NATIVE));
		GeneratedModels.addNative(api, tsModels, List.of(UUID.class), models -> new TsClassElement(models, "ZodUUID",
				"UUID", "isUUID", "zod.string().uuid()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Document.class),
				models -> new TsClassElement(models, "ZodDocument", "Document", "isDocument",
						"zod.record(zod.string(), zod.any()).nullable()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(ObjectId.class), models -> new TsClassElement(models,
				"ZodObjectId", "ObjectId", "isObjectId",
				"zod.string().length(24, \"Invalid ObjectId length\").regex(/^[a-fA-F0-9]{24}$/, \"Invalid ObjectId format\")",
				DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Long.class, long.class), models -> new TsClassElement(models,
				"ZodLong", "Long", "isLong", "zod.number()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Short.class, short.class), models -> new TsClassElement(models,
				"ZodShort", "Short", "isShort", "zod.number().safe()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Integer.class, int.class), models -> new TsClassElement(models,
				"ZodInteger", "Integer", "isInteger", "zod.number().safe()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Double.class, double.class),
				models -> new TsClassElement(models, "ZodDouble", "Double", "isDouble", "zod.number()",
						DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Float.class, float.class), models -> new TsClassElement(models,
				"ZodFloat", "Float", "isFloat", "zod.number()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Instant.class), models -> new TsClassElement(models,
				"ZodInstant", "Instant", "isInstant", "zod.string()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(Date.class), models -> new TsClassElement(models, "ZodIsoDate",
				"IsoDate", "isIsoDate", "zod.string().datetime({ precision: 3 })", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(LocalDate.class), models -> new TsClassElement(models,
				"ZodLocalDate", "LocalDate", "isLocalDate", "zod.string().date()", DefinedPosition.BASIC));
		GeneratedModels.addNative(api, tsModels, List.of(LocalTime.class), models -> new TsClassElement(models,
				"ZodLocalTime", "LocalTime", "isLocalTime", "zod.string().time()", DefinedPosition.BASIC));
		GeneratedModels.addRemaining(api, tsModels, TsClassElement::isCompatible,
				model -> new TsClassElement(model, false));
		return tsModels;

	}

	/**
	 * Copies a classpath resource file into the generation map.
	 * @param name the resource name on the classpath
	 * @param destinationPath the relative path to use as the key in the generation map
	 * @param generation the map to store the resource content into
	 * @throws IOException if the resource cannot be read
	 * @throws IllegalArgumentException if the resource is not on the classpath
	 */
	public static void copyResourceFile(
			final String name,
			final Path destinationPath,
			final Map<Path, String> generation) throws IOException {
		final InputStream ioStream = TsGenerateApi.class.getClassLoader().getResourceAsStream(name);
		if (ioStream == null) {
			throw new IllegalArgumentException("Resource not found on the classpath: " + name);
		}
		final StringBuilder data = new StringBuilder();
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(ioStream, StandardCharsets.UTF_8))) {
			String line;
			while ((line = buffer.readLine()) != null) {
				data.append(line);
				data.append("\n");
			}
		}
		generation.put(destinationPath, data.toString());
	}
}
