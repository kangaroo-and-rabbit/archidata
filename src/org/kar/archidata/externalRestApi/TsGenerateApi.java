package org.kar.archidata.externalRestApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.kar.archidata.catcher.RestErrorResponse;
import org.kar.archidata.externalRestApi.model.ApiGroupModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.typescript.TsApiGeneration;
import org.kar.archidata.externalRestApi.typescript.TsClassElement;
import org.kar.archidata.externalRestApi.typescript.TsClassElement.DefinedPosition;
import org.kar.archidata.externalRestApi.typescript.TsClassElementGroup;

public class TsGenerateApi {
	/**
	 * Generate a full API tree for Typescript in a specific folder.
	 * This generate a folder containing a full API with "model" folder and "api" folder.
	 * The generation depend of Zod and can be strict compile.
	 * @param api Data model to generate the api
	 * @param pathPackage Path to store the api.
	 */
	public static void generateApi(final AnalyzeApi api, final String pathPackage) throws Exception {
		final List<TsClassElement> localModel = generateApiModel(api);
		final TsClassElementGroup tsGroup = new TsClassElementGroup(localModel);
		// Generates all MODEL files
		for (final TsClassElement element : localModel) {
			element.generateFile(pathPackage, tsGroup);
		}
		// Generate index of model files
		createModelIndex(pathPackage, tsGroup);

		for (final ApiGroupModel element : api.apiModels) {
			TsApiGeneration.generateApiFile(element, pathPackage, tsGroup);
		}
		// Generate index of model files
		createResourceIndex(pathPackage, api.apiModels);
		createIndex(pathPackage);
		copyResourceFile("rest-tools.ts", pathPackage + File.separator + "rest-tools.ts");
	}

	private static void createIndex(final String pathPackage) throws IOException {
		final String out = """
				/**
				 * Interface of the server (auto-generated code)
				 */
				export * from \"./model\";
				export * from \"./api\";
				export * from \"./rest-tools\";

				""";
		final FileWriter myWriter = new FileWriter(pathPackage + File.separator + "index.ts");
		myWriter.write(out);
		myWriter.close();
	}

	private static void createResourceIndex(final String pathPackage, final List<ApiGroupModel> apiModels)
			throws IOException {
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
		final FileWriter myWriter = new FileWriter(pathPackage + File.separator + "api" + File.separator + "index.ts");
		myWriter.write(out.toString());
		myWriter.close();
	}

	private static void createModelIndex(final String pathPackage, final TsClassElementGroup tsGroup)
			throws IOException {
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
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "model" + File.separator + "index.ts");
		myWriter.write(out.toString());
		myWriter.close();
	}

	private static List<TsClassElement> generateApiModel(final AnalyzeApi api) throws Exception {
		// First step is to add all specific basic elements the wrap correctly the model
		final List<TsClassElement> tsModels = new ArrayList<>();
		List<ClassModel> models = api.getCompatibleModels(List.of(Void.class, void.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "void", "void", null, null, DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(Object.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "zod.any()", "object", null, "zod.object()", DefinedPosition.NATIVE));
		}
		// Map is binded to any ==> can not determine this complex model for now
		models = api.getCompatibleModels(List.of(Map.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.any()", "any", null, null, DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(String.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "zod.string()", "string", null, "zod.string()", DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(
				List.of(InputStream.class, FormDataContentDisposition.class, ContentDisposition.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "z.instanceof(File)", "File", null, "z.instanceof(File)",
					DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(Boolean.class, boolean.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.boolean()", "boolean", null, "zod.boolean()",
					DefinedPosition.NATIVE));
		}
		models = api.getCompatibleModels(List.of(UUID.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodUUID", "UUID", "isUUID", "zod.string().uuid()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Long.class, long.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "ZodLong", "Long", "isLong", "zod.number()", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Short.class, short.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodShort", "Short", "isShort", "zod.number().safe()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Integer.class, int.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInteger", "Integer", "isInteger", "zod.number().safe()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Double.class, double.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodDouble", "Double", "isDouble", "zod.number()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Float.class, float.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "ZodFloat", "Float", "isFloat", "zod.number()", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Instant.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInstant", "Instant", "isInstant", "zod.string()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Date.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodIsoDate", "IsoDate", "isIsoDate",
					"zod.string().datetime({ precision: 3 })", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(Timestamp.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodTimestamp", "Timestamp", "isTimestamp",
					"zod.string().datetime({ precision: 3 })", DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(LocalDate.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalDate", "LocalDate", "isLocalDate", "zod.string().date()",
					DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(LocalTime.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalTime", "LocalTime", "isLocalTime", "zod.string().time()",
					DefinedPosition.BASIC));
		}
		// needed for Rest interface
		api.addModel(RestErrorResponse.class);
		for (final ClassModel model : api.getAllModel()) {
			boolean alreadyExist = false;
			for (final TsClassElement elem : tsModels) {
				if (elem.isCompatible(model)) {
					alreadyExist = true;
					break;
				}
			}
			if (alreadyExist) {
				continue;
			}
			tsModels.add(new TsClassElement(model));
		}
		return tsModels;

	}

	public static void copyResourceFile(final String name, final String destinationPath) throws IOException {
		final InputStream ioStream = TsGenerateApi.class.getClassLoader().getResourceAsStream(name);
		if (ioStream == null) {
			throw new IllegalArgumentException("rest-tools.ts is not found");
		}
		final BufferedReader buffer = new BufferedReader(new InputStreamReader(ioStream));
		final FileWriter myWriter = new FileWriter(destinationPath);
		String line;
		while ((line = buffer.readLine()) != null) {
			myWriter.write(line);
			myWriter.write("\n");
		}
		ioStream.close();
		myWriter.close();
	}
}
