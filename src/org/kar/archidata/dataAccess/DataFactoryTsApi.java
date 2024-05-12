package org.kar.archidata.dataAccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.kar.archidata.annotation.AsyncType;
import org.kar.archidata.annotation.TypeScriptProgress;
import org.kar.archidata.catcher.RestErrorResponse;
import org.kar.archidata.dataAccess.DataFactoryZod.ClassElement;
import org.kar.archidata.dataAccess.DataFactoryZod.GeneratedTypes;
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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class DataFactoryTsApi {
	static final Logger LOGGER = LoggerFactory.getLogger(DataFactoryTsApi.class);

	record APIModel(
			String data,
			String className) {}

	/** Request the generation of the TypeScript file for the "Zod" export model
	 * @param classs List of class used in the model
	 * @throws Exception */
	public static List<String> createApi(
			final List<Class<?>> classs,
			final GeneratedTypes previous,
			final String pathPackage) throws Exception {
		final List<String> apis = new ArrayList<>();
		final String globalheader = """
				/**
				 * API of the server (auto-generated code)
				 */
				import {
				  HTTPMimeType,
				  HTTPRequestModel,
				  ModelResponseHttp,
				  RESTCallbacks,
				  RESTConfig,
				  RESTRequestJson,
				  RESTRequestJsonArray,
				  RESTRequestVoid
				} from "./rest-tools"
				import {""";

		for (final Class<?> clazz : classs) {
			final Set<Class<?>> includeModel = new HashSet<>();
			final Set<Class<?>> includeCheckerModel = new HashSet<>();
			final APIModel api = createSingleApi(clazz, includeModel, includeCheckerModel, previous);
			final StringBuilder generatedData = new StringBuilder();
			generatedData.append(globalheader);
			final List<String> includedElements = new ArrayList<>();
			for (final Class<?> elem : includeModel) {
				if (elem == null) {
					continue;
				}
				final ClassElement classElement = DataFactoryZod.createTable(elem, previous);
				if (classElement.nativeType) {
					continue;
				}
				includedElements.add(classElement.tsTypeName);
			}
			Collections.sort(includedElements);
			for (final String elem : includedElements) {
				generatedData.append("\n  ");
				generatedData.append(elem);
				generatedData.append(",");
			}
			for (final Class<?> elem : includeCheckerModel) {
				if (elem == null) {
					continue;
				}
				final ClassElement classElement = DataFactoryZod.createTable(elem, previous);
				if (classElement.nativeType) {
					continue;
				}
				generatedData.append("\n  ");
				generatedData.append(classElement.tsCheckType);
				generatedData.append(",");
			}
			generatedData.append("\n} from \"./model\"\n");
			generatedData.append(api.data());

			String fileName = api.className();
			fileName = fileName.replaceAll("([A-Z])", "-$1").toLowerCase();
			fileName = fileName.replaceAll("^\\-*", "");
			apis.add(fileName);
			final FileWriter myWriter = new FileWriter(pathPackage + File.separator + fileName + ".ts");
			myWriter.write(generatedData.toString());
			myWriter.close();
		}
		return apis;
	}

	public static String apiAnnotationGetPath(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Path.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((Path) annotation[0]).value();
	}

	public static List<String> apiAnnotationProduces(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static List<String> apiAnnotationProduces(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Produces.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Produces) annotation[0]).value());
	}

	public static boolean apiAnnotationTypeScriptProgress(final Method element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(TypeScriptProgress.class);
		if (annotation.length == 0) {
			return false;
		}
		return true;
	}

	public static List<String> apiAnnotationProduces(final Class<?> clazz, final Method method) throws Exception {
		final List<String> data = apiAnnotationProduces(method);
		if (data != null) {
			return data;
		}
		return apiAnnotationProduces(clazz);
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

	public static String apiAnnotationGetFormDataParam(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(FormDataParam.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((FormDataParam) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Parameter element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(AsyncType.class);
		if (annotation.length == 0) {
			return null;
		}
		return ((AsyncType) annotation[0]).value();
	}

	public static Class<?>[] apiAnnotationGetAsyncType(final Method element) throws Exception {
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

	public static List<String> apiAnnotationGetConsumes(final Class<?> element) throws Exception {
		final Annotation[] annotation = element.getDeclaredAnnotationsByType(Consumes.class);
		if (annotation.length == 0) {
			return null;
		}
		return Arrays.asList(((Consumes) annotation[0]).value());
	}

	public static List<String> apiAnnotationGetConsumes(final Class<?> clazz, final Method method) throws Exception {
		final List<String> data = apiAnnotationGetConsumes(method);
		if (data != null) {
			return data;
		}
		return apiAnnotationGetConsumes(clazz);
	}

	public static boolean apiAnnotationIsContext(final Parameter element) throws Exception {
		return element.getDeclaredAnnotationsByType(Context.class).length != 0;
	}

	public static String convertInTypeScriptType(final List<ClassElement> tmp, final boolean isList) {
		String out = "";
		for (final ClassElement elem : tmp) {
			if (out.length() != 0) {
				out += " | ";
			}
			out += elem.tsTypeName;
			if (isList) {
				out += "[]";
			}
		}
		return out;
	}

	public static String convertInTypeScriptCheckType(final List<ClassElement> tmp) {
		String out = "";
		for (final ClassElement elem : tmp) {
			if (out.length() != 0) {
				out += " | ";
			}
			out += elem.tsCheckType;
		}
		return out;
	}

	record OrderedElement(
			String methodName,
			Method method) {}

	public static APIModel createSingleApi(
			final Class<?> clazz,
			final Set<Class<?>> includeModel,
			final Set<Class<?>> includeCheckerModel,
			final GeneratedTypes previous) throws Exception {
		final StringBuilder builder = new StringBuilder();
		// the basic path has no specific elements...
		final String basicPath = apiAnnotationGetPath(clazz);
		final String classSimpleName = clazz.getSimpleName();

		builder.append("export namespace ");
		builder.append(classSimpleName);
		builder.append(" {\n");
		LOGGER.info("Parse Class for path: {} => {}", classSimpleName, basicPath);

		final List<OrderedElement> orderedElements = new ArrayList<>();
		for (final Method method : clazz.getDeclaredMethods()) {
			final String methodName = method.getName();
			orderedElements.add(new OrderedElement(methodName, method));
		}
		final Comparator<OrderedElement> comparator = Comparator.comparing(OrderedElement::methodName);
		Collections.sort(orderedElements, comparator);

		for (final OrderedElement orderedElement : orderedElements) {
			final Method method = orderedElement.method();
			final String methodName = orderedElement.methodName();
			final String methodPath = apiAnnotationGetPath(method);
			final String methodType = apiAnnotationGetTypeRequest(method);
			if (methodType == null) {
				LOGGER.error("    [{}] {} => {}/{} ==> No methode type @PATH, @GET ...", methodType, methodName,
						basicPath, methodPath);
				continue;
			}
			final String methodDescription = apiAnnotationGetOperationDescription(method);
			final List<String> consumes = apiAnnotationGetConsumes(clazz, method);
			List<String> produces = apiAnnotationProduces(clazz, method);
			LOGGER.trace("    [{}] {} => {}/{}", methodType, methodName, basicPath, methodPath);
			if (methodDescription != null) {
				LOGGER.trace("         description: {}", methodDescription);
			}
			final boolean needGenerateProgress = apiAnnotationTypeScriptProgress(method);
			Class<?>[] returnTypeModel = apiAnnotationGetAsyncType(method);
			boolean isUnmanagedReturnType = false;
			boolean returnModelIsArray = false;
			List<ClassElement> tmpReturn;
			if (returnTypeModel == null) {
				Class<?> returnTypeModelRaw = method.getReturnType();
				LOGGER.info("Get type: {}", returnTypeModelRaw);
				if (returnTypeModelRaw == Response.class) {
					LOGGER.info("Get type: {}", returnTypeModelRaw);
				}
				if (returnTypeModelRaw == Response.class || returnTypeModelRaw == void.class
						|| returnTypeModelRaw == Void.class) {
					if (returnTypeModelRaw == Response.class) {
						isUnmanagedReturnType = true;
					}
					returnTypeModel = new Class<?>[] { Void.class };
					tmpReturn = new ArrayList<>();
					produces = null;
				} else if (returnTypeModelRaw == Map.class) {
					LOGGER.warn("Not manage the Map Model ... set any");
					returnTypeModel = new Class<?>[] { Map.class };
					tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
				} else if (returnTypeModelRaw == List.class) {
					final ParameterizedType listType = (ParameterizedType) method.getGenericReturnType();
					returnTypeModelRaw = (Class<?>) listType.getActualTypeArguments()[0];
					returnModelIsArray = true;
					returnTypeModel = new Class<?>[] { returnTypeModelRaw };
					tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
				} else {
					returnTypeModel = new Class<?>[] { returnTypeModelRaw };
					tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
				}
			} else if (returnTypeModel.length >= 0 && (returnTypeModel[0] == Response.class
					|| returnTypeModel[0] == Void.class || returnTypeModel[0] == void.class)) {
				if (returnTypeModel[0] == Response.class) {
					isUnmanagedReturnType = true;
				}
				returnTypeModel = new Class<?>[] { Void.class };
				tmpReturn = new ArrayList<>();
				produces = null;
			} else if (returnTypeModel.length > 0 && returnTypeModel[0] == Map.class) {
				LOGGER.warn("Not manage the Map Model ...");
				returnTypeModel = new Class<?>[] { Map.class };
				tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
			} else {
				tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
			}
			for (final ClassElement elem : tmpReturn) {
				includeModel.add(elem.model[0]);
				includeCheckerModel.add(elem.model[0]);
			}
			LOGGER.trace("         return: {}", tmpReturn.size());
			for (final ClassElement elem : tmpReturn) {
				LOGGER.trace("             - {}", elem.tsTypeName);
			}
			final Map<String, String> queryParams = new HashMap<>();
			final Map<String, String> pathParams = new HashMap<>();
			final Map<String, String> formDataParams = new HashMap<>();
			final List<String> emptyElement = new ArrayList<>();
			// LOGGER.info(" Parameters:");
			for (final Parameter parameter : method.getParameters()) {
				// Security context are internal parameter (not available from API)
				if (apiAnnotationIsContext(parameter)) {
					continue;
				}
				final Class<?> parameterType = parameter.getType();
				String parameterTypeString;
				final Class<?>[] asyncType = apiAnnotationGetAsyncType(parameter);
				if (parameterType == List.class) {
					if (asyncType == null) {
						LOGGER.warn("Detect List param ==> not managed type ==> any[] !!!");
						parameterTypeString = "any[]";
					} else {
						final List<ClassElement> tmp = DataFactoryZod.createTables(asyncType, previous);
						for (final ClassElement elem : tmp) {
							includeModel.add(elem.model[0]);
						}
						parameterTypeString = convertInTypeScriptType(tmp, true);
					}
				} else if (asyncType == null) {
					final ClassElement tmp = DataFactoryZod.createTable(parameterType, previous);
					includeModel.add(tmp.model[0]);
					parameterTypeString = tmp.tsTypeName;
				} else {
					final List<ClassElement> tmp = DataFactoryZod.createTables(asyncType, previous);
					for (final ClassElement elem : tmp) {
						includeModel.add(elem.model[0]);
					}
					parameterTypeString = convertInTypeScriptType(tmp, true);
				}
				final String pathParam = apiAnnotationGetPathParam(parameter);
				final String queryParam = apiAnnotationGetQueryParam(parameter);
				final String formDataParam = apiAnnotationGetFormDataParam(parameter);
				if (queryParam != null) {
					queryParams.put(queryParam, parameterTypeString);
				} else if (pathParam != null) {
					pathParams.put(pathParam, parameterTypeString);
				} else if (formDataParam != null) {
					formDataParams.put(formDataParam, parameterTypeString);
				} else if (asyncType != null) {
					final List<ClassElement> tmp = DataFactoryZod.createTables(asyncType, previous);
					parameterTypeString = "";
					for (final ClassElement elem : tmp) {
						includeModel.add(elem.model[0]);
						if (parameterTypeString.length() != 0) {
							parameterTypeString += " | ";
						}
						parameterTypeString += elem.tsTypeName;
					}
					emptyElement.add(parameterTypeString);
				} else if (parameterType == List.class) {
					parameterTypeString = "any[]";
					final Class<?> plop = parameterType.arrayType();
					LOGGER.info("ArrayType = {}", plop);
					emptyElement.add(parameterTypeString);
				} else {
					final ClassElement tmp = DataFactoryZod.createTable(parameterType, previous);
					includeModel.add(tmp.model[0]);
					emptyElement.add(tmp.tsTypeName);
				}
			}
			if (!queryParams.isEmpty()) {
				LOGGER.trace("         Query parameter:");
				for (final Entry<String, String> queryEntry : queryParams.entrySet()) {
					LOGGER.trace("             - {}: {}", queryEntry.getKey(), queryEntry.getValue());
				}
			}
			if (!pathParams.isEmpty()) {
				LOGGER.trace("         Path parameter:");
				for (final Entry<String, String> pathEntry : pathParams.entrySet()) {
					LOGGER.trace("             - {}: {}", pathEntry.getKey(), pathEntry.getValue());
				}
			}
			if (emptyElement.size() > 1) {
				LOGGER.error("         Fail to parse: Too much element in the model for the data ...");
				continue;
			} else if (emptyElement.size() == 1 && formDataParams.size() != 0) {
				LOGGER.error("         Fail to parse: Incompatible form data & direct data ...");
				continue;
			} else if (emptyElement.size() == 1) {
				LOGGER.trace("         data type: {}", emptyElement.get(0));
			}
			// ALL is good can generate the Elements

			if (methodDescription != null) {
				builder.append("\n\t/**\n\t * ");
				builder.append(methodDescription);
				builder.append("\n\t */");
			}
			if (isUnmanagedReturnType) {
				builder.append(
						"\n\t// TODO: unmanaged \"Response\" type: please specify @AsyncType or considered as 'void'.");
			}
			builder.append("\n\texport function ");
			builder.append(methodName);
			builder.append("({\n\t\t\trestConfig,");
			if (!queryParams.isEmpty()) {
				builder.append("\n\t\t\tqueries,");
			}
			if (!pathParams.isEmpty()) {
				builder.append("\n\t\t\tparams,");
			}
			if (produces != null && produces.size() > 1) {
				builder.append("\n\t\t\tproduce,");
			}
			if (emptyElement.size() == 1 || formDataParams.size() != 0) {
				builder.append("\n\t\t\tdata,");
			}
			if (needGenerateProgress) {
				builder.append("\n\t\t\tcallback,");
			}
			builder.append("\n\t\t}: {");
			builder.append("\n\t\trestConfig: RESTConfig,");
			if (!queryParams.isEmpty()) {
				builder.append("\n\t\tqueries: {");
				for (final Entry<String, String> queryEntry : queryParams.entrySet()) {
					builder.append("\n\t\t\t");
					builder.append(queryEntry.getKey());
					builder.append("?: ");
					builder.append(queryEntry.getValue());
					builder.append(",");
				}
				builder.append("\n\t\t},");
			}
			if (!pathParams.isEmpty()) {
				builder.append("\n\t\tparams: {");
				for (final Entry<String, String> pathEntry : pathParams.entrySet()) {
					builder.append("\n\t\t\t");
					builder.append(pathEntry.getKey());
					builder.append(": ");
					builder.append(pathEntry.getValue());
					builder.append(",");
				}
				builder.append("\n\t\t},");
			}
			if (emptyElement.size() == 1) {
				builder.append("\n\t\tdata: ");
				builder.append(emptyElement.get(0));
				builder.append(",");
			} else if (formDataParams.size() != 0) {
				builder.append("\n\t\tdata: {");
				for (final Entry<String, String> pathEntry : formDataParams.entrySet()) {
					builder.append("\n\t\t\t");
					builder.append(pathEntry.getKey());
					builder.append(": ");
					builder.append(pathEntry.getValue());
					builder.append(",");
				}
				builder.append("\n\t\t},");
			}
			if (produces != null && produces.size() > 1) {
				builder.append("\n\t\tproduce: ");
				String isFist = null;
				for (final String elem : produces) {
					String lastElement = null;

					if (MediaType.APPLICATION_JSON.equals(elem)) {
						lastElement = "HTTPMimeType.JSON";
					}
					if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						lastElement = "HTTPMimeType.MULTIPART";
					}
					if (DataExport.CSV_TYPE.equals(elem)) {
						lastElement = "HTTPMimeType.CSV";
					}
					if (lastElement != null) {
						if (isFist == null) {
							isFist = lastElement;
						} else {
							builder.append(" | ");
						}
						builder.append(lastElement);
					} else {
						LOGGER.error("Unmanaged model type: {}", elem);
					}
				}
				builder.append(",");
			}
			if (needGenerateProgress) {
				builder.append("\n\t\tcallback?: RESTCallbacks,");
			}
			builder.append("\n\t}): Promise<");
			if (tmpReturn.size() == 0 //
					|| tmpReturn.get(0).tsTypeName == null //
					|| tmpReturn.get(0).tsTypeName.equals("void")) {
				builder.append("void");
			} else {
				builder.append(convertInTypeScriptType(tmpReturn, returnModelIsArray));
			}
			builder.append("> {");
			if (tmpReturn.size() == 0 //
					|| tmpReturn.get(0).tsTypeName == null //
					|| tmpReturn.get(0).tsTypeName.equals("void")) {
				builder.append("\n\t\treturn RESTRequestVoid({");
			} else if (returnModelIsArray) {
				builder.append("\n\t\treturn RESTRequestJsonArray({");
			} else {
				builder.append("\n\t\treturn RESTRequestJson({");
			}
			builder.append("\n\t\t\trestModel: {");
			builder.append("\n\t\t\t\tendPoint: \"");
			builder.append(basicPath);
			if (methodPath != null) {
				builder.append("/");
				builder.append(methodPath);
			}
			builder.append("\",");
			builder.append("\n\t\t\t\trequestType: HTTPRequestModel.");
			builder.append(methodType);
			builder.append(",");
			if (consumes != null) {
				for (final String elem : consumes) {
					if (MediaType.APPLICATION_JSON.equals(elem)) {
						builder.append("\n\t\t\t\tcontentType: HTTPMimeType.JSON,");
						break;
					} else if (MediaType.MULTIPART_FORM_DATA.equals(elem)) {
						builder.append("\n\t\t\t\tcontentType: HTTPMimeType.MULTIPART,");
						break;
					} else if (MediaType.TEXT_PLAIN.equals(elem)) {
						builder.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
						break;
					}
				}
			} else if ("DELETE".equals(methodType)) {
				builder.append("\n\t\t\t\tcontentType: HTTPMimeType.TEXT_PLAIN,");
			}
			if (produces != null) {
				if (produces.size() > 1) {
					builder.append("\n\t\t\t\taccept: produce,");
				} else {
					for (final String elem : produces) {
						if (MediaType.APPLICATION_JSON.equals(elem)) {
							builder.append("\n\t\t\t\taccept: HTTPMimeType.JSON,");
							break;
						}
					}
				}
			}
			builder.append("\n\t\t\t},");
			builder.append("\n\t\t\trestConfig,");
			if (!pathParams.isEmpty()) {
				builder.append("\n\t\t\tparams,");
			}
			if (!queryParams.isEmpty()) {
				builder.append("\n\t\t\tqueries,");
			}
			if (emptyElement.size() == 1) {
				builder.append("\n\t\t\tdata,");
			} else if (formDataParams.size() != 0) {
				builder.append("\n\t\t\tdata,");
			}
			if (needGenerateProgress) {
				builder.append("\n\t\t\tcallback,");
			}
			builder.append("\n\t\t}");
			if (tmpReturn.size() != 0 && tmpReturn.get(0).tsTypeName != null
					&& !tmpReturn.get(0).tsTypeName.equals("void")) {
				builder.append(", ");
				// TODO: correct this it is really bad ...
				builder.append(convertInTypeScriptCheckType(tmpReturn));
			}
			builder.append(");");
			builder.append("\n\t};");
		}
		builder.append("\n}\n");
		return new APIModel(builder.toString(), classSimpleName);
	}

	public static void generatePackage(
			final List<Class<?>> classApi,
			final List<Class<?>> classModel,
			final String pathPackage) throws Exception {
		final GeneratedTypes previous = DataFactoryZod.createBasicType();
		DataFactoryZod.createTable(RestErrorResponse.class, previous);
		final List<String> listApi = createApi(classApi, previous, pathPackage);
		final String packageApi = DataFactoryZod.createTables(new ArrayList<>(classModel), previous);
		FileWriter myWriter = new FileWriter(pathPackage + File.separator + "model");
		myWriter.write(packageApi.toString());
		myWriter.close();

		final StringBuilder index = new StringBuilder("""
				/**
				 * Global import of the package
				 */
				export * from "./model";
				""");
		for (final String api : listApi) {
			index.append("export * from \"./").append(api).append("\";\n");
		}
		myWriter = new FileWriter(pathPackage + File.separator + "index.ts");
		myWriter.write(index.toString());
		myWriter.close();
		final InputStream ioStream = DataFactoryTsApi.class.getClassLoader().getResourceAsStream("rest-tools.ts");
		if (ioStream == null) {
			throw new IllegalArgumentException("rest-tools.ts is not found");
		}
		final BufferedReader buffer = new BufferedReader(new InputStreamReader(ioStream));
		myWriter = new FileWriter(pathPackage + File.separator + "rest-tools.ts");
		String line;
		while ((line = buffer.readLine()) != null) {
			myWriter.write(line);
			myWriter.write("\n");
		}
		ioStream.close();
		myWriter.close();
		return;
	}

}
