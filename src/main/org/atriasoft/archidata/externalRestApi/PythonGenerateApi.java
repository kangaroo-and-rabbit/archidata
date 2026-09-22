package org.atriasoft.archidata.externalRestApi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ClassListModel;
import org.atriasoft.archidata.externalRestApi.model.ClassMapModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ClassPaginationModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.externalRestApi.python.PyApiGeneration;
import org.atriasoft.archidata.externalRestApi.python.PyClassElement;
import org.atriasoft.archidata.externalRestApi.python.PyClassElement.DefinedPosition;
import org.atriasoft.archidata.externalRestApi.python.PyClassElement.ImportSpec;
import org.atriasoft.archidata.externalRestApi.python.PyClassElementGroup;
import org.atriasoft.archidata.externalRestApi.python.PyImportModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates a Python API client package from {@link AnalyzeApi} introspection data.
 *
 * <p>Mirror of {@link TsGenerateApi}: the output is the content of one Python package
 * (the caller picks its directory, hence its import name):
 * <pre>
 * &lt;package&gt;/
 * ├── __init__.py        re-exports model, api, rest_tools and ApiClient
 * ├── py.typed           PEP 561 marker
 * ├── rest_tools.py      REST runtime (urllib3 + Pydantic), copied from the resources
 * ├── client.py          ApiClient: one bound client per resource
 * ├── api/               one module + class per {@code @Path} resource
 * └── model/             one module per model: Pydantic models, enums, type aliases
 * </pre>
 * Requires Python 3.11+, {@code pydantic>=2} and {@code urllib3>=2}.
 */
public class PythonGenerateApi {
	private static final Logger LOGGER = LoggerFactory.getLogger(PythonGenerateApi.class);

	/** Private constructor to prevent instantiation of this utility class. */
	private PythonGenerateApi() {
		// Utility class
	}

	/**
	 * Generates the Python package in a directory. Unchanged files are not rewritten;
	 * obsolete {@code .py} files trigger a warning.
	 *
	 * @param api the analyzed API model to generate from
	 * @param pathPackage the package directory (its name is the Python import name)
	 * @throws Exception if generation or file writing fails
	 */
	public static void generateApi(final AnalyzeApi api, final Path pathPackage) throws Exception {
		generateApi(api, pathPackage, false);
	}

	/**
	 * Generates the Python package in a directory. Unchanged files are not rewritten.
	 *
	 * @param api the analyzed API model to generate from
	 * @param pathPackage the package directory (its name is the Python import name)
	 * @param deleteObsoleteFiles if true, delete obsolete .py files; if false, log a warning
	 * @throws Exception if generation or file writing fails
	 */
	public static void generateApi(final AnalyzeApi api, final Path pathPackage, final boolean deleteObsoleteFiles)
			throws Exception {
		final Map<Path, String> generation = generateApi(api);
		GenerationWriter.writeGeneratedFiles(generation, pathPackage, ".py", deleteObsoleteFiles);
	}

	/**
	 * Generates the Python package as an in-memory map of relative file paths to content.
	 * @param api the analyzed API model to generate from
	 * @return a map of relative file paths (inside the package) to their Python content
	 * @throws Exception if generation fails
	 */
	public static Map<Path, String> generateApi(final AnalyzeApi api) throws Exception {
		final Map<Path, String> generation = new HashMap<>();
		final List<PyClassElement> elements = generateApiModel(api);
		final PyClassElementGroup group = new PyClassElementGroup(elements);
		// -----------------------------------------------------------
		// -- The API first: it registers the model variants it needs
		// -----------------------------------------------------------
		for (final ApiGroupModel element : api.apiModels) {
			PyApiGeneration.generateApiFile(element, group, generation);
		}
		PyApiGeneration.generateApiIndex(api.apiModels, generation);
		PyApiGeneration.generateClient(api.apiModels, generation);
		// -----------------------------------------------------------
		// -- Then the models
		// -----------------------------------------------------------
		propagateVariantsToParents(group);
		computeDeferredImports(group);
		for (final PyClassElement element : elements) {
			element.generateFile(group, generation);
		}
		createModelIndex(group, generation);
		// -----------------------------------------------------------
		// -- The runtime and the package root
		// -----------------------------------------------------------
		TsGenerateApi.copyResourceFile("python/rest_tools.py", Paths.get("rest_tools.py"), generation);
		createIndex(generation);
		generation.put(Paths.get("py.typed"), "");
		return generation;
	}

	/**
	 * A child variant ({@code ZoneCreate}) extends the same variant of its parent
	 * ({@code OIDGenericDataCreate}): the parent must generate it, whatever the file order.
	 */
	private static void propagateVariantsToParents(final PyClassElementGroup group) {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (final PyClassElement element : group.getPyElements()) {
				if (element.nativeType != DefinedPosition.NORMAL
						|| !(element.models.get(0) instanceof final ClassObjectModel model)
						|| model.getExtendsClass() == null) {
					continue;
				}
				final PyClassElement parent = group.find(model.getExtendsClass());
				if (parent == null || parent.nativeType != DefinedPosition.NORMAL) {
					continue;
				}
				if (element.requestedModels.isEmpty()) {
					element.getParameterClassModel(model);
				}
				for (final ParameterClassModel variant : new ArrayList<>(element.requestedModels)) {
					final int before = parent.requestedModels.size();
					parent.getParameterClassModel(variant.valid(), variant.groups(), model.getExtendsClass());
					if (parent.requestedModels.size() != before) {
						changed = true;
					}
				}
			}
		}
	}

	private static void collectReferencedElements(
			final ClassModel model,
			final PyClassElementGroup group,
			final Set<PyClassElement> out) {
		if (model instanceof final ClassListModel listModel) {
			collectReferencedElements(listModel.valueModel, group, out);
		} else if (model instanceof final ClassMapModel mapModel) {
			collectReferencedElements(mapModel.keyModel, group, out);
			collectReferencedElements(mapModel.valueModel, group, out);
		} else if (model instanceof final ClassPaginationModel paginationModel) {
			collectReferencedElements(paginationModel.valueModel, group, out);
		} else {
			final PyClassElement element = group.find(model);
			if (element != null && element.nativeType != DefinedPosition.NATIVE) {
				out.add(element);
			}
		}
	}

	private static boolean reaches(
			final PyClassElement from,
			final PyClassElement target,
			final Map<PyClassElement, Set<PyClassElement>> edges,
			final Set<PyClassElement> visited) {
		if (from == target) {
			return true;
		}
		if (!visited.add(from)) {
			return false;
		}
		for (final PyClassElement next : edges.getOrDefault(from, Set.of())) {
			if (reaches(next, target, edges, visited)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Python imports are executed eagerly: two model modules importing each other fail to
	 * load. Inheritance edges must stay eager (a base class is needed at class creation),
	 * so they are taken first; then each field reference that would close a cycle is
	 * deferred (declared under {@code if TYPE_CHECKING:}, resolved by
	 * {@code model_rebuild()} in {@code model/__init__.py}).
	 */
	private static void computeDeferredImports(final PyClassElementGroup group) {
		final Map<PyClassElement, Set<PyClassElement>> eager = new HashMap<>();
		final List<PyClassElement> objects = new ArrayList<>();
		for (final PyClassElement element : group.getPyElements()) {
			if (element.nativeType == DefinedPosition.NORMAL
					&& element.models.get(0) instanceof final ClassObjectModel model) {
				objects.add(element);
				if (model.getExtendsClass() != null) {
					final PyClassElement parent = group.find(model.getExtendsClass());
					if (parent != null && parent.nativeType == DefinedPosition.NORMAL) {
						eager.computeIfAbsent(element, k -> new LinkedHashSet<>()).add(parent);
					}
				}
			}
		}
		objects.sort((a, b) -> a.fileName.compareTo(b.fileName));
		for (final PyClassElement element : objects) {
			final ClassObjectModel model = (ClassObjectModel) element.models.get(0);
			final Set<PyClassElement> referenced = new LinkedHashSet<>();
			for (final FieldProperty field : model.getFields()) {
				collectReferencedElements(field.model(), group, referenced);
			}
			for (final PyClassElement target : referenced) {
				if (target == element || eager.getOrDefault(element, Set.of()).contains(target)) {
					continue;
				}
				if (reaches(target, element, eager, new LinkedHashSet<>())) {
					LOGGER.debug("Deferred import {} -> {} (import cycle)", element.fileName, target.fileName);
					group.defer(element, target);
				} else {
					eager.computeIfAbsent(element, k -> new LinkedHashSet<>()).add(target);
				}
			}
		}
	}

	/**
	 * Names {@code rest_tools.py} exports, re-exported at the root of the package. Kept in
	 * step with the {@code __all__} of that resource: a test of the generated package checks
	 * that neither list drifts (a star import would hide the drift, and the linters reject it).
	 */
	private static final List<String> REST_TOOLS_EXPORTS = List.of("HTTPMimeType", "HTTPRequestModel",
			"ModelResponseHttp", "MultipartFile", "Pagination", "RESTConfig", "RESTConfigProvider", "RESTConfigSource",
			"RESTError", "RESTModel", "RESTRequest", "RESTRequestBytes", "RESTRequestJson", "RESTRequestPaginatedJson",
			"RESTRequestType", "RESTRequestVoid", "RESTUrl", "resolve_rest_config");

	/**
	 * Generates the root {@code __init__.py}: the three sub-packages, the aggregated client and
	 * the REST runtime. Models and resource clients stay reachable through
	 * {@code <package>.model} and {@code <package>.api}, which carry their own {@code __all__}:
	 * re-exporting several hundred names here would say nothing more and read far worse.
	 */
	private static void createIndex(final Map<Path, String> generation) {
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"Python client of the server API (auto-generated code).\n\n");
		out.append("Models live in ``.model``, one client class per resource in ``.api``, and\n");
		out.append("``ApiClient`` binds them all to one server configuration.\n");
		out.append("\"\"\"\n\n");
		out.append("from . import api, model, rest_tools\n");
		out.append("from .client import ApiClient\n");
		final Set<String> imported = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
		imported.addAll(REST_TOOLS_EXPORTS);
		PyImportModel.appendMultilineImport(out, ".rest_tools", imported);
		// `__all__` follows the exact spelling, unlike an import list: that is the order
		// the linters check it in.
		out.append("\n\n__all__ = [\n");
		final Set<String> names = new TreeSet<>();
		names.addAll(REST_TOOLS_EXPORTS);
		names.add("ApiClient");
		names.add("api");
		names.add("model");
		names.add("rest_tools");
		for (final String name : names) {
			out.append(PyClassElement.INDENT).append(PyClassElement.pyString(name)).append(",\n");
		}
		out.append("]\n");
		generation.put(Paths.get("__init__.py"), out.toString());
	}

	private static void createModelIndex(final PyClassElementGroup group, final Map<Path, String> generation) {
		final Map<String, Set<String>> byFile = new TreeMap<>();
		final Set<String> allNames = new TreeSet<>();
		final List<String> toRebuild = new ArrayList<>();
		for (final PyClassElement element : group.getPyElements()) {
			if (element.nativeType == DefinedPosition.NATIVE || element.generatedNames.isEmpty()) {
				continue;
			}
			final Set<String> names = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
			names.addAll(element.generatedNames);
			byFile.put(element.fileName, names);
			allNames.addAll(names);
			if (group.hasDeferredImports(element)) {
				toRebuild.addAll(names);
			}
		}
		final StringBuilder out = new StringBuilder();
		out.append("\"\"\"Models of the server API (auto-generated code).\"\"\"\n\n");
		for (final Map.Entry<String, Set<String>> entry : byFile.entrySet()) {
			PyImportModel.appendMultilineImport(out, "." + entry.getKey(), entry.getValue());
		}
		out.append("\n\n__all__ = [\n");
		for (final String name : allNames) {
			out.append(PyClassElement.INDENT).append(PyClassElement.pyString(name)).append(",\n");
		}
		out.append("]\n");
		if (!toRebuild.isEmpty()) {
			toRebuild.sort(String::compareTo);
			out.append("\n# These models reference other models under `if TYPE_CHECKING:` (import cycle):\n");
			out.append("# now that every model is imported, resolve their forward references.\n");
			for (final String name : toRebuild) {
				out.append(name).append(".model_rebuild()\n");
			}
		}
		generation.put(Paths.get("model").resolve("__init__.py"), out.toString());
	}

	private static void addNative(
			final AnalyzeApi api,
			final List<PyClassElement> elements,
			final List<Class<?>> classes,
			final String typeName,
			final List<ImportSpec> usageImports) {
		final List<ClassModel> models = api.getCompatibleModels(classes);
		if (models != null) {
			elements.add(new PyClassElement(models, typeName, null, usageImports, null, DefinedPosition.NATIVE));
		}
	}

	private static List<PyClassElement> generateApiModel(final AnalyzeApi api) throws Exception {
		// needed for the error mapping of rest_tools.py
		api.addModel(RestErrorResponse.class);
		final List<PyClassElement> elements = new ArrayList<>();
		addNative(api, elements, List.of(Void.class, void.class), "None", null);
		addNative(api, elements, List.of(Object.class), "Any", List.of(new ImportSpec("typing", "Any")));
		// Map is bound to any ==> can not determine this complex model for now
		addNative(api, elements, List.of(Map.class), "dict[str, Any]", List.of(new ImportSpec("typing", "Any")));
		addNative(api, elements, List.of(String.class), "str", null);
		addNative(api, elements, List.of(InputStream.class, FormDataContentDisposition.class, ContentDisposition.class),
				"MultipartFile", List.of(new ImportSpec("rest_tools", "MultipartFile")));
		addNative(api, elements, List.of(Boolean.class, boolean.class), "bool", null);
		addNative(api, elements, List.of(UUID.class), "uuid.UUID", List.of(new ImportSpec("uuid", null)));
		addNative(api, elements, List.of(Long.class, long.class, Short.class, short.class, Integer.class, int.class),
				"int", null);
		addNative(api, elements, List.of(Double.class, double.class, Float.class, float.class), "float", null);
		addNative(api, elements, List.of(Instant.class, Date.class), "datetime",
				List.of(new ImportSpec("datetime", "datetime")));
		addNative(api, elements, List.of(LocalDate.class), "date", List.of(new ImportSpec("datetime", "date")));
		addNative(api, elements, List.of(LocalTime.class), "time", List.of(new ImportSpec("datetime", "time")));
		List<ClassModel> models = api.getCompatibleModels(List.of(Document.class));
		if (models != null) {
			elements.add(new PyClassElement(models, "Document", "dict[str, Any]", null,
					List.of(new ImportSpec("typing", "Any")), DefinedPosition.BASIC));
		}
		models = api.getCompatibleModels(List.of(ObjectId.class));
		if (models != null) {
			elements.add(new PyClassElement(models, "ObjectId",
					"Annotated[\n    str,\n    Field(pattern=r\"^[a-fA-F0-9]{24}$\", description=\"MongoDB ObjectId\"),\n]",
					null, List.of(new ImportSpec("typing", "Annotated"), new ImportSpec("pydantic", "Field")),
					DefinedPosition.BASIC));
		}
		for (final ClassModel model : api.getAllModel()) {
			boolean alreadyExist = false;
			for (final PyClassElement elem : elements) {
				if (elem.isCompatible(model)) {
					alreadyExist = true;
					break;
				}
			}
			if (alreadyExist) {
				continue;
			}
			elements.add(new PyClassElement(model));
		}
		return elements;
	}
}
