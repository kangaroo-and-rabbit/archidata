package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kar.archidata.dataAccess.DataFactoryZod;
import org.kar.archidata.dataAccess.DataFactoryZod.ClassElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

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
	boolean needGenerateProgress;
	
	// List of types returned by the API
	public List<ClassModel> returnTypes = new ArrayList<>();;
	// Name of the API (function name)
	public String name;
	// list of all parameters (/{key}/...
	public Map<String, ClassModel> parameters = new HashMap<>();
	// list of all query (?key...)
	public Map<String, ClassModel> queries = new HashMap<>();

	// Possible input type of the REST API
	public List<String> consumes = new ArrayList<>();
	// Possible output type of the REST API
	public List<String> produces = new ArrayList<>();

	private void updateReturnTypes(final Method method, final ModelGroup previousModel) {
		// get return type from the user specification:
		final Class<?>[] returnTypeModel = ApiTool.apiAnnotationGetAsyncType(method);
		
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
			return;
		}
		
		final Class<?> returnTypeModelRaw = method.getReturnType();
		LOGGER.info("Get type: {}", returnTypeModelRaw);
		if (returnTypeModelRaw == Map.class) {
			LOGGER.warn("Not manage the Map Model ... set any");
			final ParameterizedType listType = (ParameterizedType) method.getGenericReturnType();
			this.returnTypes.add(new ClassMapModel(listType, previousModel));
			return;
		}
		if (returnTypeModelRaw == List.class) {
			final ParameterizedType listType = (ParameterizedType) method.getGenericReturnType();
			this.returnTypes.add(new ClassListModel(listType, previousModel));
			return;
		}
		this.returnTypes.add(previousModel.add(returnTypeModelRaw));
	}
	
	public ApiModel(final Class<?> clazz, final Method method, final String baseRestEndPoint,
			final List<String> consume, final List<String> produce, final ModelGroup previousModel) throws Exception {
		this.originClass = clazz;
		this.orignMethod = method;

		final String methodPath = ApiTool.apiAnnotationGetPath(method);
		final String methodType = ApiTool.apiAnnotationGetTypeRequest(method);
		final String methodName = method.getName();

		this.description = ApiTool.apiAnnotationGetOperationDescription(method);
		this.consumes = ApiTool.apiAnnotationGetConsumes2(consume, method);
		this.produces = ApiTool.apiAnnotationProduces2(produce, method);
		LOGGER.trace("    [{}] {} => {}/{}", methodType, methodName, baseRestEndPoint, methodPath);
		this.needGenerateProgress = ApiTool.apiAnnotationTypeScriptProgress(method);
		
		Class<?>[] returnTypeModel = ApiTool.apiAnnotationGetAsyncType(method);
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
				this.produces = null;
			} else if (returnTypeModelRaw == Map.class) {
				LOGGER.warn("Not manage the Map Model ... set any");
				final ParameterizedType listType = (ParameterizedType) method.getGenericReturnType();
				final Type typeKey = listType.getActualTypeArguments()[0];
				final Type typeValue = listType.getActualTypeArguments()[1];
				if (typeKey == String.class) {
					if (typeValue instanceof ParameterizedType) {
						final Type typeSubKey = listType.getActualTypeArguments()[0];
						final Type typeSubValue = listType.getActualTypeArguments()[1];
						if (typeKey == String.class) {
							
						}
					}
				} else {
					LOGGER.warn("Not manage the Map Model ... set any");
					returnTypeModel = new Class<?>[] { Map.class };
					tmpReturn = DataFactoryZod.createTables(returnTypeModel, previous);
				}
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
			this.produces = null;
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
			if (ApiTool.apiAnnotationIsContext(parameter)) {
				continue;
			}
			final Class<?> parameterType = parameter.getType();
			String parameterTypeString;
			final Class<?>[] asyncType = ApiTool.apiAnnotationGetAsyncType(parameter);
			if (parameterType == List.class) {
				if (asyncType == null) {
					LOGGER.warn("Detect List param ==> not managed type ==> any[] !!!");
					parameterTypeString = "any[]";
				} else {
					final List<ClassElement> tmp = DataFactoryZod.createTables(asyncType, previous);
					for (final ClassElement elem : tmp) {
						includeModel.add(elem.model[0]);
					}
					parameterTypeString = ApiTool.convertInTypeScriptType(tmp, true);
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
				parameterTypeString = ApiTool.convertInTypeScriptType(tmp, true);
			}
			final String pathParam = ApiTool.apiAnnotationGetPathParam(parameter);
			final String queryParam = ApiTool.apiAnnotationGetQueryParam(parameter);
			final String formDataParam = ApiTool.apiAnnotationGetFormDataParam(parameter);
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
	}
}
