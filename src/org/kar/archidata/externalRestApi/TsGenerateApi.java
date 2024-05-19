package org.kar.archidata.externalRestApi;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
	}

	private static List<TsClassElement> generateApiModel(final AnalyzeApi api) {
		// First step is to add all specific basic elements the wrap correctly the model
		final List<TsClassElement> tsModels = new ArrayList<>();
		List<ClassModel> models = getCompatibleModels(api.classModels, List.of(Void.class, void.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "void", "void", null, null, true));
		}
		// Map is binded to any ==> can not determine this complex model for now
		models = getCompatibleModels(api.classModels, List.of(Map.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "any", "any", null, null, true));
		}
		models = getCompatibleModels(api.classModels, List.of(String.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.string()", "string", null, "zod.string()", true));
		}
		models = getCompatibleModels(api.classModels, List.of(InputStream.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "z.instanceof(File)", "File", null, "z.instanceof(File)", true));
		}
		models = getCompatibleModels(api.classModels, List.of(Boolean.class, boolean.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "zod.boolean()", "boolean", null, "zod.boolean()", true));
		}
		models = getCompatibleModels(api.classModels, List.of(UUID.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodUUID", "UUID", "isUUID", "zod.string().uuid()", false));
		}
		models = getCompatibleModels(api.classModels, List.of(Long.class, long.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLong", "Long", "isLong", "zod.number()", false));
		}
		models = getCompatibleModels(api.classModels, List.of(Short.class, short.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodShort", "Short", "isShort", "zod.number().safe()", true));
		}
		models = getCompatibleModels(api.classModels, List.of(Integer.class, int.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInteger", "Integer", "isInteger", "zod.number().safe()", true));
		}
		models = getCompatibleModels(api.classModels, List.of(Double.class, double.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodDouble", "Double", "isDouble", "zod.number()", true));
		}
		models = getCompatibleModels(api.classModels, List.of(Float.class, float.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodFloat", "Float", "isFloat", "zod.number()", false));
		}
		models = getCompatibleModels(api.classModels, List.of(Instant.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodInstant", "Instant", "isInstant", "zod.string()", false));
		}
		models = getCompatibleModels(api.classModels, List.of(Date.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodDate", "Date", "isDate",
					"zod.string().datetime({ precision: 3 })", false));
		}
		models = getCompatibleModels(api.classModels, List.of(Timestamp.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodTimestamp", "Timestamp", "isTimestamp",
					"zod.string().datetime({ precision: 3 })", false));
		}
		models = getCompatibleModels(api.classModels, List.of(LocalDate.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalDate", "LocalDate", "isLocalDate", "zod.string().date()",
					false));
		}
		models = getCompatibleModels(api.classModels, List.of(LocalTime.class));
		if (models != null) {
			tsModels.add(new TsClassElement(models, "ZodLocalTime", "LocalTime", "isLocalTime", "zod.string().time()",
					false));
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
}
