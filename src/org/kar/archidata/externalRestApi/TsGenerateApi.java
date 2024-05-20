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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.kar.archidata.dataAccess.DataFactoryTsApi;
import org.kar.archidata.externalRestApi.TsClassElement.DefinedPosition;
import org.kar.archidata.externalRestApi.model.ClassModel;

public class TsGenerateApi {
	
	public static List<ClassModel> getCompatibleModels(
			final List<ClassModel> requestedModel,
			final List<Class<?>> search) {
		final List<ClassModel> out = new ArrayList<>();
		for (final ClassModel model : requestedModel) {
			if (search.contains(model.getOriginClasses())) {
				out.add(model);
			}
		}
		if (out.isEmpty()) {
			return null;
		}
		return out;
	}
	
	public static void generateApi(final AnalyzeApi api, final String pathPackage) throws IOException {
		final List<TsClassElement> localModel = generateApiModel(api);
		final TsClassElementGroup tsGroup = new TsClassElementGroup(localModel);
		// Generates all files
		for (final TsClassElement element : localModel) {
			element.generateFile(pathPackage, tsGroup);
		}
		createIndex(pathPackage, tsGroup);

		copyResourceFile("rest-tools.ts", pathPackage + File.separator + "rest-tools.ts");
		//copyResourceFile("zod-tools.ts", pathPackage + File.separator + "zod-tools.ts");
	}
	
	private static void createIndex(final String pathPackage, final TsClassElementGroup tsGroup) throws IOException {
		final StringBuilder out = new StringBuilder("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				""");
		for (final TsClassElement elem : tsGroup.getTsElements()) {
			if (elem.nativeType == DefinedPosition.NATIVE) {
				continue;
			}
			out.append("export * from \"./");
			out.append(elem.fileName);
			out.append("\"\n");
		}
		final FileWriter myWriter = new FileWriter(
				pathPackage + File.separator + "model" + File.separator + "index.ts");
		myWriter.write(out.toString());
		myWriter.close();
		
	}

	private static List<TsClassElement> generateApiModel(final AnalyzeApi api) {
		// First step is to add all specific basic elements the wrap correctly the model
		final List<TsClassElement> tsModels = new ArrayList<>();
		List<ClassModel> models = getCompatibleModels(api.classModels, List.of(Void.class, void.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "void", "void", null, null, DefinedPosition.NATIVE));
		}
		models = getCompatibleModels(api.classModels, List.of(Object.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "zod.object()", "object", null, "zod.object()", DefinedPosition.NATIVE));
		}
		// Map is binded to any ==> can not determine this complex model for now
		models = getCompatibleModels(api.classModels, List.of(Map.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.any()", "any", null, null, DefinedPosition.NATIVE));
		}
		models = getCompatibleModels(api.classModels, List.of(String.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "zod.string()", "string", null, "zod.string()", DefinedPosition.NATIVE));
		}
		models = getCompatibleModels(api.classModels, List.of(InputStream.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "z.instanceof(File)", "File", null, "z.instanceof(File)",
					DefinedPosition.NATIVE));
		}
		models = getCompatibleModels(api.classModels, List.of(Boolean.class, boolean.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.boolean()", "boolean", null, "zod.boolean()",
					DefinedPosition.NATIVE));
		}
		models = getCompatibleModels(api.classModels, List.of(UUID.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodUUID", "UUID", "isUUID", "zod.string().uuid()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Long.class, long.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "ZodLong", "Long", "isLong", "zod.number()", DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Short.class, short.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodShort", "Short", "isShort", "zod.number().safe()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Integer.class, int.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInteger", "Integer", "isInteger", "zod.number().safe()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Double.class, double.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodDouble", "Double", "isDouble", "zod.number()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Float.class, float.class));
		if (models != null) {
			tsModels.add(
					new TsClassElement(models, "ZodFloat", "Float", "isFloat", "zod.number()", DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Instant.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInstant", "Instant", "isInstant", "zod.string()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Date.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodIsoDate", "IsoDate", "isIsoDate",
					"zod.string().datetime({ precision: 3 })", DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(Timestamp.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodTimestamp", "Timestamp", "isTimestamp",
					"zod.string().datetime({ precision: 3 })", DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(LocalDate.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalDate", "LocalDate", "isLocalDate", "zod.string().date()",
					DefinedPosition.BASIC));
		}
		models = getCompatibleModels(api.classModels, List.of(LocalTime.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalTime", "LocalTime", "isLocalTime", "zod.string().time()",
					DefinedPosition.BASIC));
		}
		for (final ClassModel model : api.classModels) {
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
		final InputStream ioStream = DataFactoryTsApi.class.getClassLoader().getResourceAsStream(name);
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
