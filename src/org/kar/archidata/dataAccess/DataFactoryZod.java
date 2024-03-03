package org.kar.archidata.dataAccess;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFactoryZod {
	static final Logger LOGGER = LoggerFactory.getLogger(DataFactoryZod.class);

	public static String convertTypeZodSimpleType(final Class<?> type, final Map<String, String> previousClassesGenerated, final List<String> order) throws Exception {
		if (type == UUID.class) {
			return "zod.string().uuid()";
		}
		if (type == Long.class) {
			return "zod.bigint()";
		}
		if (type == long.class) {
			return "zod.bigint()";
		}
		if (type == Integer.class || type == int.class) {
			return "zod.number().safe()";
		}
		if (type == Boolean.class || type == boolean.class) {
			return "zod.boolean()";
		}
		if (type == double.class || type == float.class || type == Double.class || type == Float.class) {
			return "zod.number()";
		}
		if (type == Instant.class) {
			return "zod.string().utc()";
		}
		if (type == Date.class || type == Timestamp.class) {
			return "zod.date()";
		}
		if (type == LocalDate.class) {
			return "zod.date()";
		}
		if (type == LocalTime.class) {
			return "zod.date()";
		}
		if (type == String.class) {
			return "zod.string()";
		}
		if (type.isEnum()) {
			final Object[] arr = type.getEnumConstants();
			final StringBuilder out = new StringBuilder();
			boolean first = true;
			out.append("zod.enum([");
			for (final Object elem : arr) {
				if (!first) {
					out.append(", ");
				}
				first = false;
				out.append("\"");
				out.append(elem.toString());
				out.append("\"");
			}
			out.append("])");
			return out.toString();
		}
		if (type == List.class) {
			return null;
		}
		createTable(type, previousClassesGenerated, order);
		return type.getSimpleName();
	}
	public static String convertTypeZod(final Field field, final Map<String, String> previousClassesGenerated, final List<String> order) throws Exception {
		final Class<?> type = field.getType();
		final String simpleType = convertTypeZodSimpleType(type, previousClassesGenerated, order);
		if (simpleType != null) {
			return simpleType;
		}
		if (type == List.class) {
			final ParameterizedType listType = (ParameterizedType) field.getGenericType();
			final Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
			final String simpleSubType = convertTypeZodSimpleType(listClass, previousClassesGenerated, order);
			return "zod.array(" + simpleSubType + ")";
		}
		throw new DataAccessException("Imcompatible type of element in object for: " + type.getCanonicalName());
	}

	public static String optionalTypeZod(final Class<?> type) throws Exception {
		if (type.isPrimitive()) {
			return "";
		}
		return ".optional()";
	}

	public static void createTablesSpecificType(final Field elem, final int fieldId, final StringBuilder builder, final Map<String, String> previousClassesGenerated, final List<String> order) throws Exception {
		final String name = elem.getName();
		final Class<?> classModel = elem.getType();
		final int limitSize = AnnotationTools.getLimitSize(elem);

		final String comment = AnnotationTools.getComment(elem);

		if (fieldId != 0) {
			builder.append(",");
		}
		if (comment != null) {
			builder.append("\n\t// ");
			builder.append(comment);
		}
		builder.append("\n\t");
		builder.append(name);
		builder.append(": ");
		builder.append(convertTypeZod(elem, previousClassesGenerated, order));
		if (limitSize > 0 && classModel == String.class) {
			builder.append(".max(");
			builder.append(limitSize);
			builder.append(")");
		}
		if (AnnotationTools.getSchemaReadOnly(elem)) {
			builder.append(".readonly()");
		}
		builder.append(optionalTypeZod(classModel));
	}

	private static boolean isFieldFromSuperClass(final Class<?> model, final String filedName) {
		final Class<?> superClass = model.getSuperclass();
		if (superClass == null) {
			return false;
		}
		for (final Field field : superClass.getFields()) {
			String name;
			try {
				name = AnnotationTools.getFieldName(field);
				if (filedName.equals(name)) {
					return true;
				}
			} catch (final Exception e) {
				// TODO Auto-generated catch block
				LOGGER.trace("Catch error field name in parent create data table: {}", e.getMessage());
			}
		}
		return false;
	}

	/** Request the generation of the TypeScript file for the "Zod" export model
	 * @param classs List of class used in the model
	 * @return A string representing the Server models
	 * @throws Exception */
	public static String createTables(final List<Class<?>> classs) throws Exception {
		final Map<String, String> previousClassesGenerated = new LinkedHashMap<>();
		final List<String> order = new ArrayList<>();
		for (final Class<?> clazz : classs) {
			createTable(clazz, previousClassesGenerated, order);
		}
		final StringBuilder generatedData = new StringBuilder();
		generatedData.append("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from \"zod\";

				""");
		for (final String elem : order) {
			final String data = previousClassesGenerated.get(elem);
			generatedData.append(data);
			generatedData.append("\n\n");
		}
		LOGGER.info("generated: {}", generatedData.toString());
		return generatedData.toString();
	}

	public static void createTable(final Class<?> clazz, final Map<String, String> previousClassesGenerated, final List<String> order) throws Exception {
		if (previousClassesGenerated.get(clazz.getCanonicalName()) != null) {
			return;
		}
		// add the current class to prevent multiple creation
		previousClassesGenerated.put(clazz.getCanonicalName(), "In Generation");
		// Local generation of class:
		final StringBuilder internalBuilder = new StringBuilder();
		final List<String> alreadyAdded = new ArrayList<>();
		LOGGER.trace("parse class: '{}'", clazz.getCanonicalName());
		int fieldId = 0;
		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final String dataName = elem.getName();
			if (isFieldFromSuperClass(clazz, dataName)) {
				LOGGER.trace("        SKIP:  '{}'", elem.getName());
				continue;
			}
			if (alreadyAdded.contains(dataName)) {
				LOGGER.trace("        SKIP2: '{}'", elem.getName());
				continue;
			}
			alreadyAdded.add(dataName);
			LOGGER.trace("        + '{}'", elem.getName());
			if (false && DataAccess.isAddOnField(elem)) {
				final DataAccessAddOn addOn = DataAccess.findAddOnforField(elem);
				LOGGER.error("Create type for: {} ==> {} (ADD-ON) ==> Not managed now ....", AnnotationTools.getFieldName(elem), elem.getType());
				/* LOGGER.trace("Create type for: {} ==> {} (ADD-ON)", AnnotationTools.getFieldName(elem), elem.getType()); if (addOn != null) { addOn.createTables(tableName, elem, tmpOut,
				 * preActionList, postActionList, createIfNotExist, createDrop, fieldId); } else { throw new DataAccessException( "Element matked as add-on but add-on does not loaded: table:" +
				 * tableName + " field name=" + AnnotationTools.getFieldName(elem) + " type=" + elem.getType()); } fieldId++; */
			} else {
				LOGGER.trace("Create type for: {} ==> {}", AnnotationTools.getFieldName(elem), elem.getType());
				DataFactoryZod.createTablesSpecificType(elem, fieldId, internalBuilder, previousClassesGenerated, order);
				fieldId++;
			}

		}
		final String description = AnnotationTools.getSchemaDescription(clazz);
		final String example = AnnotationTools.getSchemaExample(clazz);
		final StringBuilder generatedData = new StringBuilder();
		if (description != null || example != null) {
			generatedData.append("/**\n");
			if (description != null) {
				for (final String elem : description.split("\n")) {
					generatedData.append(" * ");
					generatedData.append(elem);
					generatedData.append("\n");
				}
			}
			if (example != null) {
				generatedData.append(" * Example:\n");
				generatedData.append(" * ```\n");
				for (final String elem : example.split("\n")) {
					generatedData.append(" * ");
					generatedData.append(elem);
					generatedData.append("\n");
				}
				generatedData.append(" * ```\n");
			}
			generatedData.append(" */\n");
		}
		generatedData.append("export const ");
		generatedData.append(clazz.getSimpleName());
		generatedData.append(" = ");
		final Class<?> parentClass = clazz.getSuperclass();
		if (parentClass != Object.class) {
			createTable(parentClass, previousClassesGenerated, order);
			generatedData.append(parentClass.getSimpleName());
			generatedData.append(".extend({");
		} else {
			generatedData.append("zod.object({");
		}
		generatedData.append(internalBuilder.toString());
		generatedData.append("\n});");
		// Remove the previous to reorder the map ==> parent must be inserted before us.
		previousClassesGenerated.put(clazz.getCanonicalName(), generatedData.toString());
		order.add(clazz.getCanonicalName());
	}

}