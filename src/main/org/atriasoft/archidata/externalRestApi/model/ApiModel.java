package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.core.Context;

public class ApiModel {
	static final Logger LOGGER = LoggerFactory.getLogger(ApiModel.class);

	Class<?> originClass;
	Method orignMethod;

	// Name of the REST end-point name
	public String restEndPoint;
	// Type of the request:
	public RestTypeRequest restTypeRequest;
	// Description of the API
	public String description;
	// need to generate the progression of stream (if possible)
	public boolean needGenerateProgress;

	// List of types returned by the API
	public List<ClassModel> returnTypes = new ArrayList<>();;
	// Name of the API (function name)
	public String name;
	// list of all parameters (/{key}/...
	public final Map<String, ParameterClassModelList> parameters = new HashMap<>();
	// list of all headers of the request (/{key}/...
	public final Map<String, ParameterClassModelList> headers = new HashMap<>();
	// list of all query (?key...)
	public final Map<String, ParameterClassModelList> queries = new HashMap<>();
	// when request multi-part, need to separate it.
	public final Map<String, ParameterClassModelList> multiPartParameters = new HashMap<>();
	// model of data available
	public final List<ParameterClassModelList> unnamedElement = new ArrayList<>();

	// Possible input type of the REST API
	public List<String> consumes = new ArrayList<>();
	// Possible output type of the REST API
	public List<String> produces = new ArrayList<>();

	private void updateReturnTypes(final Method method, final ModelGroup previousModel) throws Exception {
		// get return type from the user specification:
		final Class<?>[] returnTypeModel = ApiTool.apiAnnotationGetAsyncType(method);
		//LOGGER.info("Get return Type async = {}", returnTypeModel);
		if (returnTypeModel != null) {
			if (returnTypeModel.length == 0) {
				throw new IOException("Create a @AsyncType with empty elements ...");
			}
			for (final Class<?> clazz : returnTypeModel) {
				if (clazz == Void.class || clazz == void.class) {
					this.returnTypes.add(previousModel.add(Void.class));
				} else if (clazz == List.class) {
					throw new IOException("Unmanaged List.class in @AsyncType.");
				} else if (clazz == Map.class) {
					throw new IOException("Unmanaged Map.class in @AsyncType.");
				} else {
					this.returnTypes.add(previousModel.add(clazz));
				}
			}
			if (this.returnTypes.size() == 0) {
				this.produces.clear();
			}
			return;
		}

		final Class<?> returnTypeModelRaw = method.getReturnType();
		LOGGER.trace("Get return Type RAW = {}", returnTypeModelRaw.getCanonicalName());
		if (returnTypeModelRaw == Map.class) {
			LOGGER.trace("Model Map");
			final Type listType = method.getGenericReturnType();
			final ClassModel modelGenerated = ClassModel.getModel(listType, previousModel);
			this.returnTypes.add(modelGenerated);
			LOGGER.trace("Model Map ==> {}", modelGenerated);
			return;
		} else if (returnTypeModelRaw == List.class) {
			LOGGER.trace("Model List");
			final Type listType = method.getGenericReturnType();
			final ClassModel modelGenerated = ClassModel.getModel(listType, previousModel);
			this.returnTypes.add(modelGenerated);
			LOGGER.trace("Model List ==> {}", modelGenerated);
			return;
		} else {
			LOGGER.trace("Model Object");
			this.returnTypes.add(previousModel.add(returnTypeModelRaw));
		}
		LOGGER.trace("List of returns elements:");
		for (final ClassModel elem : this.returnTypes) {
			LOGGER.trace("    - {}", elem);
		}
		if (this.returnTypes.size() == 0) {
			this.produces.clear();
		}
	}

	/**
	 * Removes constraint patterns in `{param: constraint}` path segments.
	 * Keeps only `{param}`.
	 *
	 * @param path the original REST path
	 * @return cleaned path with param constraints removed
	 */
	public static String stripPathParamConstraints(final String path) {
		final boolean endsWithSlash = path.endsWith("/") && !path.equals("/");
		final String cleaned = Arrays.stream(path.split("/")).map(segment -> {
			if (segment.startsWith("{") && segment.endsWith("}")) {
				final int colonIndex = segment.indexOf(':');
				if (colonIndex != -1) {
					return "{" + segment.substring(1, colonIndex) + "}";
				}
			}
			return segment;
		}).collect(Collectors.joining("/"));
		if (cleaned.isEmpty()) {
			return "/";
		}
		return endsWithSlash && !cleaned.endsWith("/") ? cleaned + "/" : cleaned;
	}

	public ApiModel(final Class<?> clazz, final Method method, final String baseRestEndPoint,
			final List<String> consume, final List<String> produce, final ModelGroup previousModel) throws Exception {
		this.originClass = clazz;
		this.orignMethod = method;

		String tmpPath = ApiTool.apiAnnotationGetPath(method);
		if (tmpPath == null) {
			tmpPath = "";
		}
		this.restEndPoint = stripPathParamConstraints(baseRestEndPoint + "/" + tmpPath);
		this.restTypeRequest = ApiTool.apiAnnotationGetTypeRequest2(method);
		this.name = method.getName();

		this.description = ApiTool.apiAnnotationGetOperationDescription(method);
		this.consumes = ApiTool.apiAnnotationGetConsumes2(consume, method);
		this.produces = ApiTool.apiAnnotationProduces2(produce, method);
		LOGGER.trace("    [{}] {} => {}/{}", baseRestEndPoint, this.name, this.restEndPoint);
		this.needGenerateProgress = ApiTool.apiAnnotationTypeScriptProgress(method);

		updateReturnTypes(method, previousModel);
		LOGGER.trace("         return: {}", this.returnTypes.size());
		for (final ClassModel elem : this.returnTypes) {
			LOGGER.trace("             - {}", elem);
		}

		// LOGGER.info(" Parameters:");
		for (final Parameter parameter : method.getParameters()) {
			// Security context are internal parameter (not available from API)
			if (ApiTool.apiAnnotationIsContext(parameter)) {
				continue;
			}
			final Class<?> parameterType = parameter.getType();
			final List<ClassModel> parameterModel = new ArrayList<>();
			final Class<?>[] asyncType = ApiTool.apiAnnotationGetAsyncType(parameter);
			if (asyncType != null) {
				for (final Class<?> elem : asyncType) {
					final ClassModel modelGenerated = ClassModel.getModel(elem, previousModel);
					parameterModel.add(modelGenerated);
				}
			} else if (parameterType == List.class) {
				final Type parameterrizedType = parameter.getParameterizedType();
				final ClassModel modelGenerated = ClassModel.getModel(parameterrizedType, previousModel);
				parameterModel.add(modelGenerated);
			} else if (parameterType == Map.class) {
				final Type parameterrizedType = parameter.getParameterizedType();
				final ClassModel modelGenerated = ClassModel.getModel(parameterrizedType, previousModel);
				parameterModel.add(modelGenerated);
			} else {
				parameterModel.add(previousModel.add(parameterType));
			}
			final Context contextAnnotation = AnnotationTools.get(parameter, Context.class);
			final HeaderParam headerParam = AnnotationTools.get(parameter, HeaderParam.class);
			final Valid validParam = AnnotationTools.get(parameter, Valid.class);
			final ValidGroup validGroupParam = AnnotationTools.get(parameter, ValidGroup.class);
			final String pathParam = ApiTool.apiAnnotationGetPathParam(parameter);
			final String queryParam = ApiTool.apiAnnotationGetQueryParam(parameter);
			final String formDataParam = ApiTool.apiAnnotationGetFormDataParam(parameter);
			final boolean apiInputOptional = ApiTool.apiAnnotationGetApiInputOptional(parameter);
			if (queryParam != null) {
				if (!this.queries.containsKey(queryParam)) {
					this.queries.put(queryParam,
							new ParameterClassModelList(validParam, validGroupParam, parameterModel, apiInputOptional));
				}
			} else if (pathParam != null) {
				if (!this.parameters.containsKey(pathParam)) {
					this.parameters.put(pathParam,
							new ParameterClassModelList(validParam, validGroupParam, parameterModel, apiInputOptional));
				}
			} else if (formDataParam != null) {
				if (!this.multiPartParameters.containsKey(formDataParam)) {
					this.multiPartParameters.put(formDataParam,
							new ParameterClassModelList(validParam, validGroupParam, parameterModel, apiInputOptional));
				}
			} else if (contextAnnotation != null) {
				// out of scope parameters
			} else if (headerParam != null) {
				if (!this.headers.containsKey(headerParam.value())) {
					this.headers.put(headerParam.value(),
							new ParameterClassModelList(validParam, validGroupParam, parameterModel, apiInputOptional));
				}
			} else {
				this.unnamedElement.add(
						new ParameterClassModelList(validParam, validGroupParam, parameterModel, apiInputOptional));
			}
		}
		if (this.unnamedElement.size() > 1) {
			throw new IOException("Can not parse the API, enmpty element is more than 1 in " + this.name);
		}
	}
}
