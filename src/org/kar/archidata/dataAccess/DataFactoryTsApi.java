package org.kar.archidata.dataAccess;

import java.io.File;
import java.io.FileWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.AsyncType;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

public class DataFactoryTsApi {
	static final Logger LOGGER = LoggerFactory.getLogger(DataFactoryTsApi.class);

	public static String convertTypeZodSimpleType(final Class<?> type, final Map<String, String> previousClassesGenerated, final List<String> order) throws Exception {
		if (type == UUID.class) {
			return "string";
		}
		if (type == Long.class) {
			return "Bigint";
		}
		if (type == long.class) {
			return "Bigint";
		}
		if (type == Integer.class || type == int.class) {
			return "number";
		}
		if (type == Boolean.class || type == boolean.class) {
			return "boolean";
		}
		if (type == double.class || type == float.class || type == Double.class || type == Float.class) {
			return "number";
		}
		if (type == Instant.class) {
			return "string";
		}
		if (type == Date.class || type == Timestamp.class) {
			return "string";
		}
		if (type == LocalDate.class) {
			return "string";
		}
		if (type == LocalTime.class) {
			return "string";
		}
		if (type == String.class) {
			return "string";
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
		// createTable(type, previousClassesGenerated, order);
		return "Zod" + type.getSimpleName();
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

	public static void createTablesSpecificType(final Field elem, final int fieldId, final StringBuilder builder, final Map<String, String> previousClassesGenerated, final List<String> order)
			throws Exception {
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

	/** Request the generation of the TypeScript file for the "Zod" export model
	 * @param classs List of class used in the model
	 * @return A string representing the Server models
	 * @throws Exception */
	public static String createApi(final List<Class<?>> classs, final Set<Class<?>> classNeeded) throws Exception {
		final List<String> apis = new ArrayList<>();
		for (final Class<?> clazz : classs) {
			final String api = createSingleApi(clazz, classNeeded);
			apis.add(api);
		}
		final StringBuilder generatedDataElems = new StringBuilder();
		for (final String elem : apis) {
			generatedDataElems.append(elem);
			generatedDataElems.append("\n\n");
		}
		final StringBuilder generatedData = new StringBuilder();
		generatedData.append("""
				/**
				 * API of the server (auto-generated code)
				 */
				import {""");
		for (final Class<?> elem : classNeeded) {
			generatedData.append(elem.getSimpleName());
			generatedData.append(", ");
		}
		generatedData.append("} from \"./model.ts\"\n\n");
		return generatedData.toString() + generatedDataElems.toString();
	}

	public static String apiAnnotationGetPath(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static String apiAnnotationGetOperationDescription(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Operation.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Operation) annotation[0]).description();
	}

	public static String apiAnnotationGetPath(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static String apiAnnotationGetTypeRequest(final Method element) throws Exception {
		if (element.getDeclaredAnnotationsByType(GET.class).length == 1) {
			return "GET";
		}
		if (element.getDeclaredAnnotationsByType(POST.class).length == 1) {
			return "POST";
		}
		if (element.getDeclaredAnnotationsByType(PUT.class).length == 1) {
			return "PUT";
		}
		if (element.getDeclaredAnnotationsByType(PATCH.class).length == 1) {
			return "PATCH";
		}
		if (element.getDeclaredAnnotationsByType(DELETE.class).length == 1) {
			return "DELETE";
		}
		return null;
	}

	public static String apiAnnotationGetPathParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(PathParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((PathParam) annotation[0]).value();
	}

	public static String apiAnnotationGetQueryParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(QueryParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((QueryParam) annotation[0]).value();
	}

	public static Class<?> apiAnnotationGetAsyncType(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(AsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((AsyncType) annotation[0]).value();
	}

	public static List<String> apiAnnotationGetConsumes(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Consumes.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Consumes) annotation[0]).value());
	}

	public static boolean apiAnnotationIsContext(final Parameter element) throws Exception {
		return element.getDeclaredAnnotationsByType(Context.class).length != 0;
	}

	public static String createSingleApi(final Class<?> clazz, final Set<Class<?>> classNeeded) throws Exception {
		final StringBuilder builder = new StringBuilder();
		// the basic path has no specific elements...
		final String basicPath = apiAnnotationGetPath(clazz);
		final String classSimpleName = clazz.getSimpleName();

		builder.append("export namespace ");
		builder.append(classSimpleName);
		builder.append("API {\n");
		LOGGER.info("Parse Class for path: {} => {}", classSimpleName, basicPath);
		for (final Method method : clazz.getDeclaredMethods()) {
			final String methodName = method.getName();
			final String methodPath = apiAnnotationGetPath(method);
			final String methodType = apiAnnotationGetTypeRequest(method);
			final String methodDescription = apiAnnotationGetOperationDescription(method);
			final List<String> consumes = apiAnnotationGetConsumes(method);
			if (consumes != null && consumes.contains(MediaType.MULTIPART_FORM_DATA)) {
				LOGGER.error("    [{}] {} => {}/{} ==> Multipart is not managed ...", methodType, methodName, basicPath, methodPath);
				continue;
			}
			LOGGER.trace("    [{}] {} => {}/{}", methodType, methodName, basicPath, methodPath);
			final Class<?> returnType = method.getReturnType();
			if (methodDescription != null) {
				LOGGER.trace("         description: {}", methodDescription);
			}
			LOGGER.trace("         return: {}", returnType.getSimpleName());
			final Map<String, Class<?>> queryParams = new HashMap<>();
			final Map<String, Class<?>> pathParams = new HashMap<>();
			final List<Class<?>> emptyElement = new ArrayList<>();
			// LOGGER.info(" Parameters:");
			for (final Parameter parameter : method.getParameters()) {
				if (apiAnnotationIsContext(parameter)) {
					continue;
				}
				final Class<?> parameterType = parameter.getType();
				final String pathParam = apiAnnotationGetPathParam(parameter);
				final String queryParam = apiAnnotationGetQueryParam(parameter);
				if (queryParam != null) {
					queryParams.put(queryParam, parameterType);
				} else if (pathParam != null) {
					pathParams.put(pathParam, parameterType);
				} else {
					final Class<?> asyncType = apiAnnotationGetAsyncType(parameter);
					if (asyncType != null) {
						emptyElement.add(asyncType);
					} else {
						emptyElement.add(parameterType);
					}
					// LOGGER.info(" - {} ", parameterType.getSimpleName());
				}
			}
			if (!queryParams.isEmpty()) {
				LOGGER.trace("         Query parameter:");
				for (final Entry<String, Class<?>> queryEntry : queryParams.entrySet()) {
					LOGGER.trace("             - {}: {}", queryEntry.getKey(), queryEntry.getValue().getSimpleName());
				}
			}
			if (!pathParams.isEmpty()) {
				LOGGER.trace("         Path parameter:");
				for (final Entry<String, Class<?>> pathEntry : pathParams.entrySet()) {
					LOGGER.trace("             - {}: {}", pathEntry.getKey(), pathEntry.getValue().getSimpleName());

				}
			}
			if (emptyElement.size() > 1) {
				LOGGER.error("         Fail to parse: Too much element in the model for the data ...");
				continue;
			} else if (emptyElement.size() == 1) {
				LOGGER.trace("         data type: {}", emptyElement.get(0).getSimpleName());
			}
			// ALL is good can generate the Elements

			if (methodDescription != null) {
				builder.append("\n\t/**\n\t * ");
				builder.append(methodDescription);
				builder.append("\n\t */");
			}
			builder.append("\n\texport function ");
			builder.append(methodName);
			builder.append("({");
			builder.append("options,");
			builder.append(" serverUrl,");
			if (!queryParams.isEmpty()) {
				builder.append(" queries,");
			}
			if (!pathParams.isEmpty()) {
				builder.append(" params,");
			}
			if (emptyElement.size() == 1) {
				builder.append(" data,");
			}
			builder.append(" } : {");
			builder.append("\n\t\t\toptions: any,");
			builder.append("\n\t\t\tserverUrl: string,");
			if (!queryParams.isEmpty()) {
				builder.append("\n\t\t\tqueries: {");
				for (final Entry<String, Class<?>> queryEntry : queryParams.entrySet()) {
					classNeeded.add(queryEntry.getValue());
					builder.append("\n\t\t\t\t");
					builder.append(queryEntry.getKey());
					builder.append(": ");
					builder.append(queryEntry.getValue().getSimpleName());
					builder.append(",");
				}
				builder.append("\n\t\t\t},");
			}
			if (!pathParams.isEmpty()) {
				builder.append("\n\t\t\tparams: {");
				for (final Entry<String, Class<?>> pathEntry : pathParams.entrySet()) {
					classNeeded.add(pathEntry.getValue());
					builder.append("\n\t\t\t\t");
					builder.append(pathEntry.getKey());
					builder.append(": ");
					builder.append(pathEntry.getValue().getSimpleName());
					builder.append(",");
				}
				builder.append("\n\t\t\t},");
			}
			if (emptyElement.size() == 1) {
				builder.append("\n\t\t\tdata: ");
				classNeeded.add(emptyElement.get(0));
				builder.append(emptyElement.get(0).getSimpleName());
				builder.append(",");
			}
			builder.append("\n\t\t}) : Promise<");
			if (returnType == Void.class) {
				builder.append("void");
			} else {
				classNeeded.add(returnType);
				builder.append(returnType.getSimpleName());
			}
			builder.append("> {");
			builder.append("\n\t\treturn new Promise((resolve, reject) => {");
			/* fetch('https://example.com?' + new URLSearchParams({ foo: 'value', bar: 2, })) */
			builder.append("\n\t\t});");
			builder.append("\n\t};");
		}

		builder.append("\n}\n");
		return builder.toString();
	}

	public static void generatePackage(final List<Class<?>> classApi, final List<Class<?>> classModel, final String pathPackage) throws Exception {
		final Set<Class<?>> classNeeded = new HashSet<>(classModel);
		final String data = createApi(classApi, classNeeded);
		FileWriter myWriter = new FileWriter(pathPackage + File.separator + "api.ts");
		myWriter.write(data);
		myWriter.close();
		final String packageApi = DataFactoryZod.createTables(new ArrayList<>(classNeeded));
		myWriter = new FileWriter(pathPackage + File.separator + "model.ts");
		myWriter.write(packageApi.toString());
		myWriter.close();
		final String index = """
				/**
				 * Global import of the package
				 */
				export * from "./model.ts";
				export * from "./api.ts";

				""";
		myWriter = new FileWriter(pathPackage + File.separator + "index.ts");
		myWriter.write(index);
		myWriter.close();

		return;
	}

}