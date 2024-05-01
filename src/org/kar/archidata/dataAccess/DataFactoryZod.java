package org.kar.archidata.dataAccess;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

public class DataFactoryZod {
	static final Logger LOGGER = LoggerFactory.getLogger(DataFactoryZod.class);

	static public class ClassElement {
		public Class<?>[] model;
		public String zodName;
		public String tsTypeName;
		public String tsCheckType;
		public String declaration;
		public String comment = null;
		public boolean isEnum = false;
		public boolean nativeType;

		public ClassElement(final Class<?> model[], final String zodName, final String tsTypeName,
				final String tsCheckType, final String declaration, final boolean nativeType) {
			this.model = model;
			this.zodName = zodName;
			this.tsTypeName = tsTypeName;
			this.tsCheckType = tsCheckType;
			this.declaration = declaration;
			this.nativeType = nativeType;
		}

		public ClassElement(final Class<?> model) {
			this(new Class<?>[] { model });
		}

		public ClassElement(final Class<?> model[]) {
			this.model = model;
			this.zodName = "Zod" + model[0].getSimpleName();
			this.tsTypeName = model[0].getSimpleName();
			this.tsCheckType = "is" + model[0].getSimpleName();
			this.declaration = null;
			this.nativeType = false;
		}
	}

	public static class GeneratedTypes {
		final List<ClassElement> previousGeneration = new ArrayList<>();
		final List<Class<?>> order = new ArrayList<>();

		public ClassElement find(final Class<?> clazz) {
			for (final ClassElement elem : this.previousGeneration) {
				for (final Class<?> elemClass : elem.model) {
					if (elemClass == clazz) {
						return elem;
					}
				}
			}
			return null;
		}

		public void add(final ClassElement elem) {
			this.previousGeneration.add(elem);
		}

		public void add(final ClassElement elem, final boolean addOrder) {
			this.previousGeneration.add(elem);
			if (addOrder) {
				this.order.add(elem.model[0]);
			}
		}

		public void addOrder(final ClassElement elem) {
			this.order.add(elem.model[0]);
		}
	}

	public static ClassElement convertTypeZodEnum(final Class<?> clazz, final GeneratedTypes previous)
			throws Exception {
		final ClassElement element = new ClassElement(clazz);
		previous.add(element);
		final Object[] arr = clazz.getEnumConstants();
		final StringBuilder out = new StringBuilder();
		if (System.getenv("ARCHIDATA_GENERATE_ZOD_ENUM") != null) {
			boolean first = true;
			out.append("zod.enum([");
			for (final Object elem : arr) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append("'");
				out.append(elem.toString());
				out.append("'");
			}
			if (first) {
				out.append("]}");
			} else {
				out.append("\n\t])");
			}
		} else {
			element.isEnum = true;
			boolean first = true;
			out.append("{");
			for (final Object elem : arr) {
				if (!first) {
					out.append(",\n\t");
				} else {
					out.append("\n\t");
					first = false;
				}
				out.append(elem.toString());
				out.append(" = '");
				out.append(elem.toString());
				out.append("'");
			}
			if (first) {
				out.append("}");
			} else {
				out.append(",\n\t}");
			}
		}
		element.declaration = out.toString();
		previous.addOrder(element);
		return element;
	}

	public static String convertTypeZod(final Class<?> type, final GeneratedTypes previous) throws Exception {
		final ClassElement previousType = previous.find(type);
		if (previousType != null) {
			return previousType.zodName;
		}
		if (type.isEnum()) {
			return convertTypeZodEnum(type, previous).zodName;
		}
		if (type == List.class) {
			throw new DataAccessException("Imcompatible type of element in object for: " + type.getCanonicalName()
					+ " Unmanaged List of List ... ");
		}
		final ClassElement elemCreated = createTable(type, previous);
		if (elemCreated != null) {
			return elemCreated.zodName;
		}
		throw new DataAccessException("Imcompatible type of element in object for: " + type.getCanonicalName());
	}

	public static String convertTypeZod(final Field field, final GeneratedTypes previous) throws Exception {
		final Class<?> type = field.getType();
		final ClassElement previousType = previous.find(type);
		if (previousType != null) {
			return previousType.zodName;
		}
		if (type.isEnum()) {
			return convertTypeZodEnum(type, previous).zodName;
		}
		if (type == List.class) {
			final ParameterizedType listType = (ParameterizedType) field.getGenericType();
			final Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
			final String simpleSubType = convertTypeZod(listClass, previous);
			return "zod.array(" + simpleSubType + ")";
		}
		final ClassElement elemCreated = createTable(type, previous);
		if (elemCreated != null) {
			return elemCreated.zodName;
		}
		throw new DataAccessException("Imcompatible type of element in object for: " + type.getCanonicalName());
	}

	public static String optionalTypeZod(final Class<?> type) throws Exception {
		if (type.isPrimitive()) {
			return "";
		}
		return ".optional()";
	}

	public static void createTablesSpecificType(
			final Field elem,
			final int fieldId,
			final StringBuilder builder,
			final GeneratedTypes previous) throws Exception {
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
		builder.append(convertTypeZod(elem, previous));
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

	public static GeneratedTypes createBasicType() throws Exception {
		final GeneratedTypes previous = new GeneratedTypes();
		previous.add(new ClassElement(new Class<?>[] { Void.class, void.class }, "void", "void", null, null, true));
		// Map is binded to any ==> can not determine this complex model for now
		previous.add(new ClassElement(new Class<?>[] { Map.class }, "any", "any", null, null, true));
		previous.add(new ClassElement(new Class<?>[] { String.class }, "zod.string()", "string", null, "zod.string()",
				true));
		previous.add(new ClassElement(new Class<?>[] { InputStream.class, FormDataContentDisposition.class },
				"z.instanceof(File)", "File", null, "z.instanceof(File)", true));
		previous.add(new ClassElement(new Class<?>[] { Boolean.class, boolean.class }, "zod.boolean()", "boolean", null,
				"zod.boolean()", true));
		previous.add(new ClassElement(new Class<?>[] { UUID.class }, "ZodUUID", "UUID", "isUUID", "zod.string().uuid()",
				false), true);
		previous.add(new ClassElement(new Class<?>[] { Long.class, long.class }, "ZodLong", "Long", "isLong",
				// "zod.bigint()",
				"zod.number()", false), true);
		previous.add(new ClassElement(new Class<?>[] { Integer.class, int.class }, "ZodInteger", "Integer", "isInteger",
				"zod.number().safe()", false), true);
		previous.add(new ClassElement(new Class<?>[] { Double.class, double.class }, "ZodDouble", "Double", "isDouble",
				"zod.number()", true), true);
		previous.add(new ClassElement(new Class<?>[] { Float.class, float.class }, "ZodFloat", "Float", "isFloat",
				"zod.number()", false), true);
		previous.add(new ClassElement(new Class<?>[] { Instant.class }, "ZodInstant", "Instant", "isInstant",
				"zod.string()", false), true);
		previous.add(new ClassElement(new Class<?>[] { Date.class }, "ZodDate", "Date", "isDate", "zod.date()", false),
				true);
		previous.add(new ClassElement(new Class<?>[] { Timestamp.class }, "ZodTimestamp", "Timestamp", "isTimestamp",
				"zod.date()", false), true);
		previous.add(new ClassElement(new Class<?>[] { LocalDate.class }, "ZodLocalDate", "LocalDate", "isLocalDate",
				"zod.date()", false), true);
		previous.add(new ClassElement(new Class<?>[] { LocalTime.class }, "ZodLocalTime", "LocalTime", "isLocalTime",
				"zod.date()", false), true);
		return previous;
	}

	/** Request the generation of the TypeScript file for the "Zod" export model
	 * @param classs List of class used in the model
	 * @return A string representing the Server models
	 * @throws Exception */
	public static String createTables(final List<Class<?>> classs) throws Exception {
		return createTables(classs, createBasicType());
	}

	public static String createTables(final List<Class<?>> classs, final GeneratedTypes previous) throws Exception {
		for (final Class<?> clazz : classs) {
			createTable(clazz, previous);
		}

		final StringBuilder generatedData = new StringBuilder();
		generatedData.append("""
				/**
				 * Interface of the server (auto-generated code)
				 */
				import { z as zod } from \"zod\";

				""");
		for (final Class<?> elem : previous.order) {
			final ClassElement data = previous.find(elem);
			if (!data.nativeType) {
				if (data.comment != null) {
					generatedData.append(data.comment);
				}
				generatedData.append(createDeclaration(data));
				generatedData.append("\n\n");
			}
		}
		LOGGER.info("generated: {}", generatedData.toString());
		return generatedData.toString();
	}

	public static List<ClassElement> createTables(final Class<?>[] classs, final GeneratedTypes previous)
			throws Exception {
		final List<ClassElement> out = new ArrayList<>();
		for (final Class<?> clazz : classs) {
			if (clazz == Response.class) {
				throw new IOException("Can not generate a Zod element for an unknow type Response");
			}
			out.add(createTable(clazz, previous));
		}
		return out;
	}

	public static ClassElement createTable(final Class<?> clazz, final GeneratedTypes previous) throws Exception {
		if (clazz == null) {
			return null;
		}
		if (clazz == Response.class) {
			throw new IOException("Can not generate a Zod element for an unknow type Response");
		}
		final ClassElement alreadyExist = previous.find(clazz);
		if (previous.find(clazz) != null) {
			return alreadyExist;
		}
		if (clazz.isPrimitive()) {
			return null;
		}
		// add the current class to prevent multiple creation
		final ClassElement curentElementClass = new ClassElement(clazz);
		previous.add(curentElementClass);
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
				LOGGER.error("Create type for: {} ==> {} (ADD-ON) ==> Not managed now ....",
						AnnotationTools.getFieldName(elem), elem.getType());
				/* LOGGER.trace("Create type for: {} ==> {} (ADD-ON)", AnnotationTools.getFieldName(elem), elem.getType()); if (addOn != null) { addOn.createTables(tableName, elem, tmpOut,
				 * preActionList, postActionList, createIfNotExist, createDrop, fieldId); } else { throw new DataAccessException( "Element matked as add-on but add-on does not loaded: table:" +
				 * tableName + " field name=" + AnnotationTools.getFieldName(elem) + " type=" + elem.getType()); } fieldId++; */
			} else {
				LOGGER.trace("Create type for: {} ==> {}", AnnotationTools.getFieldName(elem), elem.getType());
				DataFactoryZod.createTablesSpecificType(elem, fieldId, internalBuilder, previous);
				fieldId++;
			}

		}
		final String description = AnnotationTools.getSchemaDescription(clazz);
		final String example = AnnotationTools.getSchemaExample(clazz);
		final StringBuilder generatedCommentedData = new StringBuilder();
		if (description != null || example != null) {
			generatedCommentedData.append("/**\n");
			if (description != null) {
				for (final String elem : description.split("\n")) {
					generatedCommentedData.append(" * ");
					generatedCommentedData.append(elem);
					generatedCommentedData.append("\n");
				}
			}
			if (example != null) {
				generatedCommentedData.append(" * Example:\n");
				generatedCommentedData.append(" * ```\n");
				for (final String elem : example.split("\n")) {
					generatedCommentedData.append(" * ");
					generatedCommentedData.append(elem);
					generatedCommentedData.append("\n");
				}
				generatedCommentedData.append(" * ```\n");
			}
			generatedCommentedData.append(" */\n");
		}
		curentElementClass.comment = generatedCommentedData.toString();
		final StringBuilder generatedData = new StringBuilder();
		final Class<?> parentClass = clazz.getSuperclass();
		if (parentClass != null && parentClass != Object.class && parentClass != Record.class) {
			final ClassElement parentDeclaration = createTable(parentClass, previous);
			generatedData.append(parentDeclaration.zodName);
			generatedData.append(".extend({");
		} else {
			generatedData.append("zod.object({");
		}
		generatedData.append(internalBuilder.toString());
		generatedData.append("\n})");
		// Remove the previous to reorder the map ==> parent must be inserted before us.
		curentElementClass.declaration = generatedData.toString();
		previous.addOrder(curentElementClass);
		return curentElementClass;
	}

	public static String createDeclaration(final ClassElement elem) {
		final StringBuilder generatedData = new StringBuilder();
		if (elem.isEnum) {
			generatedData.append("export enum ");
			generatedData.append(elem.tsTypeName);
			generatedData.append(" ");
			generatedData.append(elem.declaration);
			generatedData.append(";");
			generatedData.append("\nexport const ");
			generatedData.append(elem.zodName);
			generatedData.append(" = zod.nativeEnum(");
			generatedData.append(elem.tsTypeName);
			generatedData.append(");");
		} else {
			generatedData.append("export const ");
			generatedData.append(elem.zodName);
			generatedData.append(" = ");
			generatedData.append(elem.declaration);
			generatedData.append(";");
			generatedData.append("\nexport type ");
			generatedData.append(elem.tsTypeName);
			generatedData.append(" = zod.infer<typeof ");
			generatedData.append(elem.zodName);
			generatedData.append(">;");
		}
		// declare generic isXXX:
		generatedData.append("\nexport function ");
		generatedData.append(elem.tsCheckType);
		generatedData.append("(data: any): data is ");
		generatedData.append(elem.tsTypeName);
		generatedData.append(" {\n\ttry {\n\t\t");
		generatedData.append(elem.zodName);
		generatedData.append("""
				.parse(data);
						return true;
					} catch (e: any) {
						console.log(`Fail to parse data ${e}`);
						return false;
					}
				}
				""");
		return generatedData.toString();
	}

	public static void generatePackage(final List<Class<?>> classs, final String pathPackage) throws Exception {
		final String packageApi = createTables(classs);
		FileWriter myWriter = new FileWriter(pathPackage + File.separator + "model.ts");
		myWriter.write(packageApi.toString());
		myWriter.close();
		final String index = """
				/**
				 * Global import of the package
				 */
				export * from "./model.ts";

				""";
		myWriter = new FileWriter(pathPackage + File.separator + "index.ts");
		myWriter.write(index);
		myWriter.close();
	}

}