package org.atriasoft.archidata.externalRestApi.python;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModelList;
import org.atriasoft.archidata.externalRestApi.model.RestTypeRequest;
import org.atriasoft.archidata.externalRestApi.python.PyClassElement.DefinedPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;

/**
 * Generates Python API client classes.
 */
public class PyApiGeneration {
	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(PyApiGeneration.class);

	/**
	 * Converts a CamelCase method name to snake_case.
	 * @param name the CamelCase name to convert
	 * @return the snake_case version of the name
	 */
	public static String toSnakeCase(final String name) {
		return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}

	/**
	 * Generates the Python type annotation for a single model.
	 * @param model the class model to generate a type annotation for
	 * @param pyGroup the group registry for resolving type references
	 * @param imports the set of model import names to populate
	 * @param isPartial whether to generate the Update (partial) variant
	 * @return the Python type annotation string
	 */
	public static String generateTypeAnnotation(
			final ClassModel model,
			final PyClassElementGroup pyGroup,
			final Set<String> imports,
			final boolean isPartial) {

		if (model instanceof final ClassObjectModel objectModel) {
			final PyClassElement pyModel = pyGroup.find(objectModel);
			if (pyModel != null && pyModel.nativeType != DefinedPosition.NATIVE) {
				final String typeName = pyModel.getTypeName();
				imports.add(typeName);
				if (isPartial) {
					return typeName + "Update";
				}
				return typeName;
			}
			return "Any";
		}

		if (model instanceof final ClassEnumModel enumModel) {
			final PyClassElement pyModel = pyGroup.find(enumModel);
			if (pyModel != null) {
				imports.add(pyModel.getTypeName());
				return pyModel.getTypeName();
			}
			return "str";
		}

		if (model instanceof final ClassListModel listModel) {
			final String valueType = generateTypeAnnotation(listModel.valueModel, pyGroup, imports, isPartial);
			return "list[" + valueType + "]";
		}

		if (model instanceof final ClassMapModel mapModel) {
			final String keyType = generateTypeAnnotation(mapModel.keyModel, pyGroup, imports, isPartial);
			final String valueType = generateTypeAnnotation(mapModel.valueModel, pyGroup, imports, isPartial);
			return "dict[" + keyType + ", " + valueType + "]";
		}

		// Native type
		final PyClassElement pyModel = pyGroup.find(model);
		if (pyModel != null) {
			return pyModel.getTypeName();
		}

		return "Any";
	}

	/**
	 * Generates the type annotation for a list of models as a union type.
	 * @param models the list of parameter class models
	 * @param pyGroup the group registry for resolving type references
	 * @param imports the set of model import names to populate
	 * @param isPartial whether to generate the Update (partial) variant
	 * @return the Python type annotation string, possibly a union type
	 */
	public static String generateTypeAnnotations(
			final ParameterClassModelList models,
			final PyClassElementGroup pyGroup,
			final Set<String> imports,
			final boolean isPartial) {

		if (models == null || models.models() == null || models.models().isEmpty()) {
			return "None";
		}

		if (models.models().size() == 1) {
			return generateTypeAnnotation(models.models().get(0), pyGroup, imports, isPartial);
		}

		// Union type
		final List<String> types = new ArrayList<>();
		for (final ClassModel model : models.models()) {
			types.add(generateTypeAnnotation(model, pyGroup, imports, isPartial));
		}
		return String.join(" | ", types);
	}

	/**
	 * Generates the Python API client file for a resource group.
	 * @param element the API group model containing endpoint definitions
	 * @param pyGroup the group registry for resolving type references
	 * @param generation the map of file paths to generated content
	 */
	public static void generateApiFile(
			final ApiGroupModel element,
			final PyClassElementGroup pyGroup,
			final Map<Path, String> generation) {

		final StringBuilder out = new StringBuilder();
		final Set<String> modelImports = new TreeSet<>();
		final Set<String> restToolsImports = new HashSet<>();

		// Start building the class
		final StringBuilder classContent = new StringBuilder();
		final String className = element.name + "Api";

		classContent.append("\n\nclass ");
		classContent.append(className);
		classContent.append(":\n");
		classContent.append("    \"\"\"REST API client for ");
		classContent.append(element.name);
		classContent.append(" resources.\"\"\"\n\n");

		// Constructor
		classContent.append("    def __init__(self, rest_config: RESTConfig) -> None:\n");
		classContent.append("        \"\"\"Initialize the API client.\n\n");
		classContent.append("        Args:\n");
		classContent.append("            rest_config: REST client configuration.\n");
		classContent.append("        \"\"\"\n");
		classContent.append("        self._rest_config = rest_config\n");
		restToolsImports.add("RESTConfig");

		// Generate methods for each endpoint
		for (final ApiModel interfaceElement : element.interfaces) {
			classContent.append("\n");
			classContent.append(generateMethod(interfaceElement, pyGroup, modelImports, restToolsImports));
		}

		// Build final output with imports
		out.append("\"\"\"");
		out.append(element.name);
		out.append(" API client (auto-generated code).\"\"\"\n\n");
		out.append("from __future__ import annotations\n\n");

		// Typing imports
		out.append("from typing import TYPE_CHECKING\n\n");

		// Rest tools imports
		final List<String> sortedRestImports = new ArrayList<>(restToolsImports);
		Collections.sort(sortedRestImports);
		out.append("from .rest_tools import (\n");
		for (final String imp : sortedRestImports) {
			out.append("    ");
			out.append(imp);
			out.append(",\n");
		}
		out.append(")\n");

		// Model imports
		if (!modelImports.isEmpty()) {
			out.append("\nif TYPE_CHECKING:\n");
			out.append("    from .model import (\n");
			final List<String> sortedModelImports = new ArrayList<>(modelImports);
			Collections.sort(sortedModelImports);
			for (final String imp : sortedModelImports) {
				out.append("        ");
				out.append(imp);
				out.append(",\n");
			}
			out.append("    )\n");
		}

		out.append(classContent);

		final String fileName = PyClassElement.toSnakeCase(element.name) + "_api";
		generation.put(Paths.get("api").resolve(fileName + ".py"), out.toString());
	}

	/**
	 * Generate a single API method.
	 */
	private static String generateMethod(
			final ApiModel interfaceElement,
			final PyClassElementGroup pyGroup,
			final Set<String> modelImports,
			final Set<String> restToolsImports) {

		final StringBuilder out = new StringBuilder();
		final String methodName = toSnakeCase(interfaceElement.name);
		final boolean isPartial = interfaceElement.restTypeRequest == RestTypeRequest.PATCH;

		// Determine return type
		String returnType = "None";
		boolean returnsList = false;
		boolean returnsVoid = true;

		if (!interfaceElement.returnTypes.isEmpty()) {
			final ClassModel returnModel = interfaceElement.returnTypes.get(0);
			if (returnModel.getOriginClasses() != Void.class && returnModel.getOriginClasses() != void.class) {
				returnsVoid = false;
				if (returnModel instanceof ClassListModel) {
					returnsList = true;
					returnType = generateTypeAnnotation(returnModel, pyGroup, modelImports, false);
				} else {
					returnType = generateTypeAnnotation(returnModel, pyGroup, modelImports, false);
				}
			}
		}

		// Method signature
		out.append("    def ");
		out.append(methodName);
		out.append("(\n");
		out.append("        self,\n");

		// Path parameters
		for (final Entry<String, ParameterClassModelList> param : interfaceElement.parameters.entrySet()) {
			out.append("        ");
			out.append(toSnakeCase(param.getKey()));
			out.append(": ");
			out.append(generateTypeAnnotations(param.getValue(), pyGroup, modelImports, false));
			out.append(",\n");
		}

		// Query parameters
		if (!interfaceElement.queries.isEmpty()) {
			out.append("        *,\n"); // Force keyword-only after this
			for (final Entry<String, ParameterClassModelList> query : interfaceElement.queries.entrySet()) {
				out.append("        ");
				out.append(toSnakeCase(query.getKey()));
				out.append(": ");
				out.append(generateTypeAnnotations(query.getValue(), pyGroup, modelImports, false));
				out.append(" | None = None,\n");
			}
		}

		// Request body (data parameter)
		if (interfaceElement.unnamedElement.size() == 1) {
			if (interfaceElement.queries.isEmpty()) {
				out.append("        *,\n");
			}
			out.append("        data: ");
			final String dataType = generateTypeAnnotations(interfaceElement.unnamedElement.get(0), pyGroup,
					modelImports, isPartial);
			out.append(dataType);
			out.append(",\n");
			// Also add Create variant for creation endpoints
			if (interfaceElement.restTypeRequest == RestTypeRequest.POST) {
				// Import the Create variant
				for (final ClassModel model : interfaceElement.unnamedElement.get(0).models()) {
					if (model instanceof ClassObjectModel) {
						final PyClassElement pyModel = pyGroup.find(model);
						if (pyModel != null) {
							modelImports.add(pyModel.getTypeName() + "Create");
						}
					}
				}
			}
			if (isPartial) {
				// Import the Update variant
				for (final ClassModel model : interfaceElement.unnamedElement.get(0).models()) {
					if (model instanceof ClassObjectModel) {
						final PyClassElement pyModel = pyGroup.find(model);
						if (pyModel != null) {
							modelImports.add(pyModel.getTypeName() + "Update");
						}
					}
				}
			}
		} else if (!interfaceElement.multiPartParameters.isEmpty()) {
			if (interfaceElement.queries.isEmpty()) {
				out.append("        *,\n");
			}
			// Multipart data as dict
			out.append("        data: dict[str, Any],\n");
			modelImports.add("Any");
		}

		out.append("    ) -> ");
		out.append(returnType);
		out.append(":\n");

		// Docstring
		out.append("        \"\"\"");
		if (interfaceElement.description != null) {
			out.append(interfaceElement.description);
		} else {
			out.append(methodName.replace("_", " ").substring(0, 1).toUpperCase());
			out.append(methodName.replace("_", " ").substring(1));
		}
		out.append(".\n");

		// Document parameters
		if (!interfaceElement.parameters.isEmpty() || !interfaceElement.queries.isEmpty()
				|| interfaceElement.unnamedElement.size() == 1) {
			out.append("\n        Args:\n");
			for (final Entry<String, ParameterClassModelList> param : interfaceElement.parameters.entrySet()) {
				out.append("            ");
				out.append(toSnakeCase(param.getKey()));
				out.append(": Path parameter.\n");
			}
			for (final Entry<String, ParameterClassModelList> query : interfaceElement.queries.entrySet()) {
				out.append("            ");
				out.append(toSnakeCase(query.getKey()));
				out.append(": Query parameter.\n");
			}
			if (interfaceElement.unnamedElement.size() == 1) {
				out.append("            data: Request body.\n");
			}
		}

		if (!returnsVoid) {
			out.append("\n        Returns:\n");
			out.append("            ");
			out.append(returnType);
			out.append("\n");
		}

		out.append("\n        Raises:\n");
		out.append("            RestErrorResponse: If the request fails.\n");
		out.append("        \"\"\"\n");

		// Method body - build REST request
		out.append("        rest_model = RESTModel(\n");
		out.append("            end_point=\"");
		out.append(interfaceElement.restEndPoint);
		out.append("\",\n");
		out.append("            request_type=HTTPRequestModel.");
		out.append(interfaceElement.restTypeRequest.name());
		out.append(",\n");
		restToolsImports.add("RESTModel");
		restToolsImports.add("HTTPRequestModel");

		// Content type
		if (interfaceElement.consumes != null && !interfaceElement.consumes.isEmpty()) {
			for (final String consume : interfaceElement.consumes) {
				if (MediaType.APPLICATION_JSON.equals(consume)) {
					out.append("            content_type=HTTPMimeType.JSON,\n");
					restToolsImports.add("HTTPMimeType");
					break;
				} else if (MediaType.MULTIPART_FORM_DATA.equals(consume)) {
					out.append("            content_type=HTTPMimeType.MULTIPART,\n");
					restToolsImports.add("HTTPMimeType");
					break;
				}
			}
		}

		// Accept type
		if (interfaceElement.produces != null && !interfaceElement.produces.isEmpty()) {
			for (final String produce : interfaceElement.produces) {
				if (MediaType.APPLICATION_JSON.equals(produce)) {
					out.append("            accept=HTTPMimeType.JSON,\n");
					restToolsImports.add("HTTPMimeType");
					break;
				}
			}
		}

		out.append("        )\n\n");

		// Build params dict
		if (!interfaceElement.parameters.isEmpty()) {
			out.append("        params = {\n");
			for (final Entry<String, ParameterClassModelList> param : interfaceElement.parameters.entrySet()) {
				out.append("            \"");
				out.append(param.getKey());
				out.append("\": ");
				out.append(toSnakeCase(param.getKey()));
				out.append(",\n");
			}
			out.append("        }\n\n");
		}

		// Build queries dict
		if (!interfaceElement.queries.isEmpty()) {
			out.append("        queries = {\n");
			out.append("            k: v\n");
			out.append("            for k, v in {\n");
			for (final Entry<String, ParameterClassModelList> query : interfaceElement.queries.entrySet()) {
				out.append("                \"");
				out.append(query.getKey());
				out.append("\": ");
				out.append(toSnakeCase(query.getKey()));
				out.append(",\n");
			}
			out.append("            }.items()\n");
			out.append("            if v is not None\n");
			out.append("        }\n\n");
		}

		// Build request
		out.append("        request = RESTRequestType(\n");
		out.append("            rest_model=rest_model,\n");
		out.append("            rest_config=self._rest_config,\n");
		restToolsImports.add("RESTRequestType");

		if (!interfaceElement.parameters.isEmpty()) {
			out.append("            params=params,\n");
		}
		if (!interfaceElement.queries.isEmpty()) {
			out.append("            queries=queries if queries else None,\n");
		}
		if (interfaceElement.unnamedElement.size() == 1 || !interfaceElement.multiPartParameters.isEmpty()) {
			// Check if data is a Pydantic model
			out.append(
					"            data=data.model_dump(by_alias=True, exclude_none=True) if hasattr(data, 'model_dump') else data,\n");
		}

		out.append("        )\n\n");

		// Execute request
		if (returnsVoid) {
			out.append("        RESTRequestVoid(request)\n");
			restToolsImports.add("RESTRequestVoid");
		} else {
			// Determine the model type for validation
			final ClassModel returnModel = interfaceElement.returnTypes.get(0);
			if (returnModel instanceof final ClassListModel listModel) {
				final PyClassElement pyModel = pyGroup.find(listModel.valueModel);
				if (pyModel != null && pyModel.nativeType != DefinedPosition.NATIVE) {
					out.append("        from .model import ");
					out.append(pyModel.getTypeName());
					out.append("\n");
					out.append("        return RESTRequestJson(request, ");
					out.append(pyModel.getTypeName());
					out.append(", is_list=True)\n");
				} else {
					out.append("        return RESTRequest(request).data\n");
					restToolsImports.add("RESTRequest");
				}
			} else if (returnModel instanceof final ClassObjectModel objectModel) {
				final PyClassElement pyModel = pyGroup.find(objectModel);
				if (pyModel != null && pyModel.nativeType != DefinedPosition.NATIVE) {
					out.append("        from .model import ");
					out.append(pyModel.getTypeName());
					out.append("\n");
					out.append("        return RESTRequestJson(request, ");
					out.append(pyModel.getTypeName());
					out.append(")\n");
				} else {
					out.append("        return RESTRequest(request).data\n");
					restToolsImports.add("RESTRequest");
				}
			} else {
				out.append("        return RESTRequest(request).data\n");
				restToolsImports.add("RESTRequest");
			}
			restToolsImports.add("RESTRequestJson");
		}

		return out.toString();
	}
}
