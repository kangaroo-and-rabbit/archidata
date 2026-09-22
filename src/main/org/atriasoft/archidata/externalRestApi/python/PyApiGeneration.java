package org.atriasoft.archidata.externalRestApi.python;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.atriasoft.archidata.externalRestApi.model.ClassEnumModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassPaginationModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModelList;
import org.atriasoft.archidata.externalRestApi.model.RestTypeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MediaType;

/**
 * Generates the Python client class of one REST resource ({@code api/<resource>.py}).
 *
 * <p>Mirrors {@link org.atriasoft.archidata.externalRestApi.typescript.TsApiGeneration}: one
 * class per {@code @Path} resource, one method per endpoint. Path parameters are
 * positional (in the order of the path), everything else is keyword-only; the answer is
 * validated with Pydantic and returned typed.
 */
public class PyApiGeneration {
	/** Logger for this class. */
	static final Logger LOGGER = LoggerFactory.getLogger(PyApiGeneration.class);

	private static final Pattern PATH_PARAM = Pattern.compile("\\{([^}:]+)(?::[^}]*)?\\}");
	private static final String INDENT = PyClassElement.INDENT;
	private static final String INDENT2 = INDENT + INDENT;
	private static final String INDENT3 = INDENT2 + INDENT;
	private static final String INDENT4 = INDENT3 + INDENT;

	/** Private constructor to prevent instantiation of this utility class. */
	private PyApiGeneration() {
		// Utility class
	}

	/**
	 * A generated method parameter.
	 * @param pyName the Python parameter name
	 * @param originalName the name on the wire (path, query or header key)
	 * @param annotation the type annotation, without the {@code | None} of optional parameters
	 * @param optional whether the parameter defaults to {@code None}
	 * @param doc the description written in the docstring
	 */
	private record Parameter(
			String pyName,
			String originalName,
			String annotation,
			boolean optional,
			String doc) {
		String signature() {
			if (this.optional) {
				return this.pyName + ": " + this.annotation + " | None = None";
			}
			return this.pyName + ": " + this.annotation;
		}
	}

	/**
	 * Gets the Python class name of a resource.
	 * @param element the resource
	 * @return the class name ({@code ZoneResourceApi})
	 */
	public static String getClassName(final ApiGroupModel element) {
		return element.name + "Api";
	}

	/**
	 * Gets the module name of a resource.
	 * @param element the resource
	 * @return the snake_case module name, without extension
	 */
	public static String getFileName(final ApiGroupModel element) {
		return PyClassElement.toSnakeCase(element.name);
	}

	/**
	 * Gets the attribute name of a resource in the aggregated client.
	 * @param element the resource
	 * @return the snake_case attribute name ({@code zone_resource})
	 */
	public static String getAttributeName(final ApiGroupModel element) {
		return PyClassElement.toPythonIdentifier(element.name);
	}

	/**
	 * Renders the union annotation of a parameter model list.
	 * @param models the parameter models
	 * @param group the group registry for resolving type references
	 * @param imports the import model of the file
	 * @return the annotation, {@code None} when empty
	 */
	public static String generateTypeAnnotations(
			final ParameterClassModelList models,
			final PyClassElementGroup group,
			final PyImportModel imports) {
		if (models == null || models.models() == null || models.models().isEmpty()) {
			return "None";
		}
		final List<String> types = new ArrayList<>();
		for (final ClassModel model : models.models()) {
			types.add(generateVariantAnnotation(model, models.valid(), models.groups(), group, imports));
		}
		return String.join(" | ", types);
	}

	/**
	 * Renders the annotation of a model in a validation context: an object gets the variant
	 * matching the groups ({@code ZoneCreate}), anything else its default rendering.
	 */
	private static String generateVariantAnnotation(
			final ClassModel model,
			final boolean valid,
			final Class<?>[] groups,
			final PyClassElementGroup group,
			final PyImportModel imports) {
		if (model instanceof ClassObjectModel) {
			final PyClassElement element = group.find(model);
			if (element != null && element.nativeType == PyClassElement.DefinedPosition.NORMAL) {
				final String name = element.getTypeName(valid, groups);
				imports.addModel(element, name);
				return name;
			}
		}
		return PyClassElement.generateTypeForModel(model, group, imports);
	}

	private static boolean isVoid(final List<ClassModel> returnTypes) {
		if (returnTypes.isEmpty()) {
			return true;
		}
		final Class<?> clazz = returnTypes.get(0).getOriginClasses();
		return returnTypes.size() == 1 && (clazz == Void.class || clazz == void.class);
	}

	private static boolean producesJson(final List<String> produces) {
		return produces == null || produces.isEmpty() || produces.contains(MediaType.APPLICATION_JSON);
	}

	private static String mimeConstant(final String mediaType) {
		if (MediaType.APPLICATION_JSON.equals(mediaType)) {
			return "HTTPMimeType.JSON";
		}
		if (MediaType.MULTIPART_FORM_DATA.equals(mediaType)) {
			return "HTTPMimeType.MULTIPART";
		}
		if (MediaType.TEXT_PLAIN.equals(mediaType)) {
			return "HTTPMimeType.TEXT_PLAIN";
		}
		if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			return "HTTPMimeType.OCTET_STREAM";
		}
		if ("text/csv".equals(mediaType)) {
			return "HTTPMimeType.CSV";
		}
		if ("image/jpeg".equals(mediaType)) {
			return "HTTPMimeType.IMAGE_JPEG";
		}
		if ("image/png".equals(mediaType)) {
			return "HTTPMimeType.IMAGE_PNG";
		}
		if (MediaType.WILDCARD.equals(mediaType)) {
			return "HTTPMimeType.ALL";
		}
		return null;
	}

	/**
	 * Orders the path parameters as they appear in the endpoint, then the others by name.
	 */
	private static List<String> orderedPathParameters(final ApiModel interfaceElement) {
		final List<String> ordered = new ArrayList<>();
		final Matcher matcher = PATH_PARAM.matcher(interfaceElement.restEndPoint);
		while (matcher.find()) {
			final String name = matcher.group(1).strip();
			if (interfaceElement.parameters.containsKey(name) && !ordered.contains(name)) {
				ordered.add(name);
			}
		}
		final List<String> others = new ArrayList<>(interfaceElement.parameters.keySet());
		others.removeAll(ordered);
		Collections.sort(others);
		ordered.addAll(others);
		return ordered;
	}

	private static String humanize(final String methodName) {
		final String spaced = PyClassElement.toSnakeCase(methodName).replace('_', ' ');
		return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1) + ".";
	}

	private static String renderDictLiteral(final String indent, final List<Parameter> parameters) {
		final StringBuilder out = new StringBuilder("{\n");
		for (final Parameter param : parameters) {
			out.append(indent).append(INDENT).append(PyClassElement.pyString(param.originalName())).append(": ")
					.append(param.pyName()).append(",\n");
		}
		out.append(indent).append("}");
		return out.toString();
	}

	/**
	 * Generates the Python module of a resource and adds it to the generation map.
	 * @param element the API group model containing the endpoint definitions
	 * @param group the group registry for resolving type references
	 * @param generation the map of file paths to generated content
	 */
	public static void generateApiFile(
			final ApiGroupModel element,
			final PyClassElementGroup group,
			final Map<Path, String> generation) {
		final PyImportModel imports = new PyImportModel();
		imports.addRestTools("RESTConfigSource");
		final List<String> methods = new ArrayList<>();
		for (final ApiModel interfaceElement : element.interfaces) {
			methods.add(generateMethod(interfaceElement, group, imports));
		}
		final String className = getClassName(element);
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"").append(element.name).append(" API client (auto-generated code).\"\"\"\n\n");
		out.append(imports.renderCommonImports());
		out.append(imports.renderApiFileImports());
		out.append("\n");
		out.append("class ").append(className).append(":\n");
		out.append(INDENT).append("\"\"\"Client of the ``").append(element.name).append("`` endpoints");
		if (element.restEndPoint != null && !element.restEndPoint.isBlank()) {
			out.append(" (``").append(element.restEndPoint).append("``)");
		}
		out.append(".\"\"\"\n\n");
		out.append(INDENT).append("def __init__(self, rest_config: RESTConfigSource) -> None:\n");
		out.append(INDENT2).append("\"\"\"Bind the client to a server configuration.\n\n");
		out.append(INDENT2).append("Args:\n");
		out.append(INDENT3).append("rest_config: The configuration, or a callable returning it (called\n");
		out.append(INDENT4).append("before every request, for tokens that change at runtime).\n\n");
		out.append(INDENT2).append("\"\"\"\n");
		out.append(INDENT2).append("self._rest_config = rest_config\n");
		for (final String method : methods) {
			out.append("\n");
			out.append(method);
		}
		generation.put(Paths.get("api").resolve(getFileName(element) + ".py"), out.toString());
	}

	/**
	 * Generates one endpoint method.
	 */
	private static String generateMethod(
			final ApiModel interfaceElement,
			final PyClassElementGroup group,
			final PyImportModel imports) {
		final String methodName = PyClassElement.toPythonIdentifier(interfaceElement.name);
		final boolean isPatch = interfaceElement.restTypeRequest == RestTypeRequest.PATCH;
		final boolean returnsVoid = isVoid(interfaceElement.returnTypes);
		final boolean isPaginated = interfaceElement.returnTypes.stream()
				.anyMatch(ClassPaginationModel.class::isInstance);
		final boolean jsonAnswer = producesJson(interfaceElement.produces);
		final boolean multiProduces = interfaceElement.produces != null && interfaceElement.produces.size() > 1;

		// -- parameters -------------------------------------------------------
		// Java lets two parameters of one endpoint map to the same Python name (`type` and
		// `type_`, a path `entityId` and a query `entity_id`, ...); the wire name is kept
		// by Parameter.originalName, so only the Python one has to be made unique.
		final Set<String> usedNames = new LinkedHashSet<>();
		usedNames.add("self");
		final List<Parameter> pathParams = new ArrayList<>();
		for (final String name : orderedPathParameters(interfaceElement)) {
			pathParams.add(new Parameter(uniqueName(name, usedNames), name,
					generateTypeAnnotations(interfaceElement.parameters.get(name), group, imports), false,
					"Path parameter ``{" + name + "}``."));
		}
		Parameter dataParam = null;
		final List<Parameter> multipartParams = new ArrayList<>();
		if (interfaceElement.unnamedElement.size() == 1) {
			String annotation = generateTypeAnnotations(interfaceElement.unnamedElement.get(0), group, imports);
			if (isPatch) {
				imports.addTyping("Any");
				annotation = annotation + " | dict[str, Any]";
			}
			dataParam = new Parameter(uniqueName("data", usedNames), null, annotation, false,
					isPatch ? "Request body: the fields to change (unset fields are not sent)." : "Request body.");
		} else if (!interfaceElement.multiPartParameters.isEmpty()) {
			for (final Entry<String, ParameterClassModelList> entry : new TreeMap<>(
					interfaceElement.multiPartParameters).entrySet()) {
				multipartParams.add(new Parameter(uniqueName(entry.getKey(), usedNames), entry.getKey(),
						generateTypeAnnotations(entry.getValue(), group, imports), entry.getValue().optional(),
						"Multipart field ``" + entry.getKey() + "``."));
			}
		}
		final List<Parameter> queryParams = new ArrayList<>();
		for (final Entry<String, ParameterClassModelList> entry : new TreeMap<>(interfaceElement.queries).entrySet()) {
			queryParams.add(new Parameter(uniqueName(entry.getKey(), usedNames), entry.getKey(),
					generateTypeAnnotations(entry.getValue(), group, imports), true,
					"Query parameter ``" + entry.getKey() + "``."));
		}
		final List<Parameter> headerParams = new ArrayList<>();
		for (final Entry<String, ParameterClassModelList> entry : new TreeMap<>(interfaceElement.headers).entrySet()) {
			headerParams.add(new Parameter(uniqueName(entry.getKey(), usedNames), entry.getKey(),
					generateTypeAnnotations(entry.getValue(), group, imports), entry.getValue().optional(),
					"Header ``" + entry.getKey() + "``."));
		}
		final List<Parameter> extraParams = new ArrayList<>();
		String offsetArgument = null;
		String limitArgument = null;
		if (isPaginated) {
			// A resource can carry its page through its own `offset` / `limit` query
			// parameters instead of @PaginationContext (the pagination headers are then
			// ignored server-side): reuse them rather than declaring a second pair.
			offsetArgument = existingIntParameter("offset", pathParams, queryParams, headerParams);
			if (offsetArgument == null) {
				offsetArgument = uniqueName("offset", usedNames);
				extraParams
						.add(new Parameter(offsetArgument, null, "int", true, "Index of the first item of the page."));
			}
			limitArgument = existingIntParameter("limit", pathParams, queryParams, headerParams);
			if (limitArgument == null) {
				limitArgument = uniqueName("limit", usedNames);
				extraParams
						.add(new Parameter(limitArgument, null, "int", true, "Maximum number of items in the page."));
			}
		}
		String acceptDefault = null;
		String acceptArgument = null;
		if (multiProduces) {
			final List<String> accepted = new ArrayList<>();
			for (final String produce : interfaceElement.produces) {
				final String constant = mimeConstant(produce);
				if (constant == null) {
					LOGGER.error("Unmanaged produced media type: {}", produce);
					continue;
				}
				accepted.add(constant);
			}
			if (!accepted.isEmpty()) {
				imports.addRestTools("HTTPMimeType");
				acceptDefault = accepted.contains("HTTPMimeType.JSON") ? "HTTPMimeType.JSON" : accepted.get(0);
				acceptArgument = uniqueName("accept", usedNames);
				extraParams.add(new Parameter(acceptArgument, null, "HTTPMimeType", false,
						"Expected answer type, one of " + String.join(", ", accepted) + "."));
			}
		}

		// -- return type ------------------------------------------------------
		final String returnType;
		if (returnsVoid) {
			returnType = "None";
		} else if (!jsonAnswer && !multiProduces) {
			returnType = "bytes";
		} else {
			returnType = generateTypeAnnotations(new ParameterClassModelList(true, new Class<?>[] { GroupRead.class },
					interfaceElement.returnTypes, false), group, imports);
		}

		// -- signature --------------------------------------------------------
		final StringBuilder out = new StringBuilder();
		out.append(INDENT).append("def ").append(methodName).append("(\n");
		out.append(INDENT2).append("self,\n");
		for (final Parameter param : pathParams) {
			out.append(INDENT2).append(param.signature()).append(",\n");
		}
		final List<Parameter> keywordParams = new ArrayList<>();
		if (dataParam != null) {
			keywordParams.add(dataParam);
		}
		keywordParams.addAll(multipartParams);
		keywordParams.addAll(queryParams);
		keywordParams.addAll(headerParams);
		keywordParams.addAll(extraParams);
		if (!keywordParams.isEmpty()) {
			out.append(INDENT2).append("*,\n");
			for (final Parameter param : keywordParams) {
				out.append(INDENT2);
				if (param.pyName().equals(acceptArgument) && acceptDefault != null) {
					out.append(acceptArgument).append(": HTTPMimeType = ").append(acceptDefault);
				} else {
					out.append(param.signature());
				}
				out.append(",\n");
			}
		}
		out.append(INDENT).append(") -> ").append(returnType).append(":\n");

		// -- docstring --------------------------------------------------------
		final StringBuilder doc = new StringBuilder();
		if (interfaceElement.description != null && !interfaceElement.description.isBlank()) {
			doc.append(interfaceElement.description.strip());
			if (!interfaceElement.description.strip().endsWith(".")) {
				doc.append(".");
			}
		} else {
			doc.append(humanize(interfaceElement.name));
		}
		doc.append("\n\n``").append(interfaceElement.restTypeRequest.name()).append(" ")
				.append(interfaceElement.restEndPoint).append("``");
		final List<Parameter> documented = new ArrayList<>(pathParams);
		documented.addAll(keywordParams);
		if (!documented.isEmpty()) {
			doc.append("\n\nArgs:\n");
			for (final Parameter param : documented) {
				doc.append(INDENT).append(param.pyName()).append(": ").append(param.doc()).append("\n");
			}
		}
		if (!returnsVoid) {
			doc.append("\nReturns:\n").append(INDENT).append("The validated answer (``").append(returnType)
					.append("``).\n");
		}
		doc.append("\nRaises:\n").append(INDENT)
				.append("RESTError: When the call fails (network, HTTP status or invalid answer).\n\n");
		out.append(PyClassElement.pyDocstring(doc.toString(), INDENT2));

		// -- body: request ----------------------------------------------------
		imports.addRestTools("RESTModel");
		imports.addRestTools("RESTRequestType");
		imports.addRestTools("HTTPRequestModel");
		final StringBuilder restModel = new StringBuilder();
		restModel.append(INDENT4).append("end_point=").append(PyClassElement.pyString(interfaceElement.restEndPoint))
				.append(",\n");
		restModel.append(INDENT4).append("request_type=HTTPRequestModel.")
				.append(interfaceElement.restTypeRequest.name()).append(",\n");
		if (acceptArgument != null) {
			restModel.append(INDENT4).append("accept=").append(acceptArgument).append(",\n");
		} else if (!returnsVoid && jsonAnswer) {
			imports.addRestTools("HTTPMimeType");
			restModel.append(INDENT4).append("accept=HTTPMimeType.JSON,\n");
		}
		String contentType = null;
		if (interfaceElement.consumes != null) {
			for (final String consume : interfaceElement.consumes) {
				contentType = mimeConstant(consume);
				if (contentType != null) {
					break;
				}
			}
		}
		if (contentType == null && (dataParam != null || !multipartParams.isEmpty())) {
			contentType = multipartParams.isEmpty() ? "HTTPMimeType.JSON" : "HTTPMimeType.MULTIPART";
		}
		if (contentType != null && (dataParam != null || !multipartParams.isEmpty())) {
			imports.addRestTools("HTTPMimeType");
			restModel.append(INDENT4).append("content_type=").append(contentType).append(",\n");
		}
		final StringBuilder request = new StringBuilder();
		request.append(INDENT3).append("rest_model=RESTModel(\n").append(restModel).append(INDENT3).append("),\n");
		request.append(INDENT3).append("rest_config=self._rest_config,\n");
		if (!pathParams.isEmpty()) {
			request.append(INDENT3).append("params=").append(renderDictLiteral(INDENT3, pathParams)).append(",\n");
		}
		if (!queryParams.isEmpty()) {
			request.append(INDENT3).append("queries=").append(renderDictLiteral(INDENT3, queryParams)).append(",\n");
		}
		if (!headerParams.isEmpty()) {
			// headers left to None are not sent
			request.append(INDENT3).append("headers={\n").append(INDENT4).append("key: value\n").append(INDENT4)
					.append("for key, value in ").append(renderDictLiteral(INDENT4, headerParams)).append(".items()\n")
					.append(INDENT4).append("if value is not None\n").append(INDENT3).append("},\n");
		}
		if (dataParam != null) {
			request.append(INDENT3).append("data=data,\n");
		} else if (!multipartParams.isEmpty()) {
			request.append(INDENT3).append("data=").append(renderDictLiteral(INDENT3, multipartParams)).append(",\n");
		}
		final String requestExpr = "RESTRequestType(\n" + request + INDENT2 + ")";

		// -- body: call -------------------------------------------------------
		if (returnsVoid) {
			imports.addRestTools("RESTRequestVoid");
			out.append(INDENT2).append("RESTRequestVoid(\n").append(INDENT3).append(indentContinuation(requestExpr))
					.append(",\n").append(INDENT2).append(")\n");
		} else if (isPaginated) {
			imports.addRestTools("RESTRequestPaginatedJson");
			final ClassPaginationModel pagination = (ClassPaginationModel) interfaceElement.returnTypes.stream()
					.filter(ClassPaginationModel.class::isInstance).findFirst().get();
			final String itemType = PyClassElement.generateTypeForModel(pagination.valueModel, group, imports);
			out.append(INDENT2).append("return RESTRequestPaginatedJson(\n").append(INDENT3)
					.append(indentContinuation(requestExpr)).append(",\n").append(INDENT3)
					.append(typeArgument(itemType, imports)).append(",\n").append(INDENT3).append("offset=")
					.append(offsetArgument).append(",\n").append(INDENT3).append("limit=").append(limitArgument)
					.append(",\n").append(INDENT2).append(")\n");
		} else if ("bytes".equals(returnType)) {
			imports.addRestTools("RESTRequestBytes");
			out.append(INDENT2).append("return RESTRequestBytes(\n").append(INDENT3)
					.append(indentContinuation(requestExpr)).append(",\n").append(INDENT2).append(")\n");
		} else if ("Any".equals(returnType)) {
			imports.addRestTools("RESTRequest");
			out.append(INDENT2).append("return RESTRequest(\n").append(INDENT3).append(indentContinuation(requestExpr))
					.append(",\n").append(INDENT2).append(").data\n");
		} else {
			imports.addRestTools("RESTRequestJson");
			out.append(INDENT2).append("return RESTRequestJson(\n").append(INDENT3)
					.append(indentContinuation(requestExpr)).append(",\n").append(INDENT3)
					.append(typeArgument(returnType, imports)).append(",\n").append(INDENT2).append(")\n");
		}
		return out.toString();
	}

	/**
	 * Turns a wire name into a Python parameter name that is not taken yet in this method.
	 * @param originalName the name on the wire
	 * @param usedNames the names already taken, extended with the returned one
	 * @return the Python parameter name
	 */
	private static String uniqueName(final String originalName, final Set<String> usedNames) {
		final String base = PyClassElement.toPythonParameter(originalName);
		String candidate = base;
		int index = 2;
		while (!usedNames.add(candidate)) {
			candidate = base + "_" + index;
			index++;
		}
		return candidate;
	}

	/**
	 * Finds a declared parameter that already carries a pagination input.
	 * @param wireName the wire name to look for ({@code offset} or {@code limit})
	 * @param groups the declared parameter lists to search
	 * @return the Python name of that parameter, or {@code null} when there is no integer one
	 */
	@SafeVarargs
	private static String existingIntParameter(final String wireName, final List<Parameter>... groups) {
		for (final List<Parameter> parameters : groups) {
			for (final Parameter parameter : parameters) {
				if (wireName.equals(parameter.originalName()) && "int".equals(parameter.annotation())) {
					return parameter.pyName();
				}
			}
		}
		return null;
	}

	/**
	 * Renders the runtime type passed to the validator. A union is not a {@code type[T]} for
	 * the type checkers, so it is cast explicitly.
	 */
	private static String typeArgument(final String annotation, final PyImportModel imports) {
		if (isTopLevelUnion(annotation)) {
			imports.addTyping("cast");
			return "cast(\"type[" + annotation + "]\", " + annotation + ")";
		}
		return annotation;
	}

	/**
	 * Checks whether an annotation is a union at its top level: {@code A | B} is, while
	 * {@code list[A | None]} is not (a cast there would be redundant, and type checkers say so).
	 * @param annotation the type annotation
	 * @return true when the union is the outermost construct
	 */
	private static boolean isTopLevelUnion(final String annotation) {
		int depth = 0;
		for (int index = 0; index < annotation.length(); index++) {
			final char current = annotation.charAt(index);
			if (current == '[') {
				depth++;
			} else if (current == ']') {
				depth--;
			} else if (current == '|' && depth == 0) {
				return true;
			}
		}
		return false;
	}

	/** Re-indents the continuation lines of an expression nested one level deeper. */
	private static String indentContinuation(final String expression) {
		final String[] lines = expression.split("\n", -1);
		final StringBuilder out = new StringBuilder(lines[0]);
		for (int i = 1; i < lines.length; i++) {
			out.append("\n").append(INDENT).append(lines[i]);
		}
		return out.toString();
	}

	/**
	 * Generates {@code api/__init__.py}: re-exports every resource class.
	 * @param apiModels the resources
	 * @param generation the map of file paths to generated content
	 */
	public static void generateApiIndex(final List<ApiGroupModel> apiModels, final Map<Path, String> generation) {
		final Map<String, String> byFile = new TreeMap<>();
		for (final ApiGroupModel element : apiModels) {
			byFile.put(getFileName(element), getClassName(element));
		}
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"Clients of the server resources (auto-generated code).\"\"\"\n\n");
		for (final Entry<String, String> entry : byFile.entrySet()) {
			PyImportModel.appendMultilineImport(out, "." + entry.getKey(), Set.of(entry.getValue()));
		}
		out.append("\n\n__all__ = [\n");
		final List<String> names = new ArrayList<>(byFile.values());
		Collections.sort(names);
		for (final String name : names) {
			out.append(INDENT).append(PyClassElement.pyString(name)).append(",\n");
		}
		out.append("]\n");
		generation.put(Paths.get("api").resolve("__init__.py"), out.toString());
	}

	/**
	 * Generates {@code client.py}: the {@code ApiClient} holding one bound client per resource.
	 * @param apiModels the resources
	 * @param generation the map of file paths to generated content
	 */
	public static void generateClient(final List<ApiGroupModel> apiModels, final Map<Path, String> generation) {
		final Map<String, ApiGroupModel> byAttribute = new LinkedHashMap<>();
		final List<ApiGroupModel> sorted = new ArrayList<>(apiModels);
		sorted.sort((a, b) -> getAttributeName(a).compareTo(getAttributeName(b)));
		for (final ApiGroupModel element : sorted) {
			byAttribute.put(getAttributeName(element), element);
		}
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"Aggregated client of the server API (auto-generated code).\"\"\"\n\n");
		final Set<String> classNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
		for (final ApiGroupModel element : byAttribute.values()) {
			classNames.add(getClassName(element));
		}
		out.append("from .api import (\n");
		for (final String name : classNames) {
			out.append(INDENT).append(name).append(",\n");
		}
		out.append(")\n");
		out.append("from .rest_tools import RESTConfigSource\n\n\n");
		out.append("class ApiClient:\n");
		out.append(INDENT).append("\"\"\"One object holding every resource client, sharing one configuration.\n\n");
		out.append(INDENT).append("Example::\n\n");
		out.append(INDENT2).append("client = ApiClient(RESTConfig(server=\"https://my.server/api\", token_api=KEY))\n");
		if (!byAttribute.isEmpty()) {
			out.append(INDENT2).append("client.").append(byAttribute.keySet().iterator().next())
					.append(".<method>(...)\n");
		}
		out.append("\n").append(INDENT)
				.append("Pass a callable instead of a ``RESTConfig`` when the token changes at runtime:\n");
		out.append(INDENT).append("it is called before every request.\n");
		out.append(INDENT).append("\"\"\"\n\n");
		out.append(INDENT).append("def __init__(self, rest_config: RESTConfigSource) -> None:\n");
		out.append(INDENT2).append("\"\"\"Bind every resource client to ``rest_config``.\"\"\"\n");
		out.append(INDENT2).append("self.rest_config = rest_config\n");
		for (final Entry<String, ApiGroupModel> entry : byAttribute.entrySet()) {
			out.append(renderClientAttribute(entry.getKey(), getClassName(entry.getValue())));
		}
		generation.put(Paths.get("client.py"), out.toString());
	}

	/**
	 * Renders one {@code self.<resource> = <Resource>Api(rest_config)} line of the client,
	 * wrapped the way a Python formatter would when it does not fit.
	 * @param attribute the attribute name
	 * @param className the resource client class
	 * @return the assignment, terminated by a new line
	 */
	private static String renderClientAttribute(final String attribute, final String className) {
		final String declaration = INDENT2 + "self." + attribute + " = ";
		final String oneLine = declaration + className + "(rest_config)";
		if (oneLine.length() <= PyClassElement.LINE_LENGTH) {
			return oneLine + "\n";
		}
		if ((declaration + className + "(").length() <= PyClassElement.LINE_LENGTH) {
			return declaration + className + "(\n" + INDENT3 + "rest_config,\n" + INDENT2 + ")\n";
		}
		return declaration + "(\n" + INDENT3 + className + "(rest_config)\n" + INDENT2 + ")\n";
	}

	/**
	 * Checks whether a model is an enum (helper for callers building unions).
	 * @param model the model
	 * @return true for an enum model
	 */
	public static boolean isEnum(final ClassModel model) {
		return model instanceof ClassEnumModel;
	}

	/**
	 * Registers the variant of a model requested with explicit validation groups.
	 * @param model the model
	 * @param valid whether validation is active
	 * @param groups the validation groups
	 * @param group the group registry
	 * @return the registered variant, or null for native types
	 */
	public static ParameterClassModel requestVariant(
			final ClassModel model,
			final boolean valid,
			final Class<?>[] groups,
			final PyClassElementGroup group) {
		final PyClassElement element = group.find(model);
		if (element == null || element.nativeType != PyClassElement.DefinedPosition.NORMAL) {
			return null;
		}
		return element.getParameterClassModel(valid, groups, model);
	}
}
