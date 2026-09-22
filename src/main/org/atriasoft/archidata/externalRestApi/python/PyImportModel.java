package org.atriasoft.archidata.externalRestApi.python;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Collects the imports needed by one generated Python file and renders them.
 *
 * <p>Model imports are keyed by the imported element (one {@code from .file import
 * A, B} line per file); the standard-library and third-party imports are plain
 * name sets, rendered in the usual isort order: {@code __future__}, standard
 * library, third party (pydantic), then the local package.
 */
public class PyImportModel {

	/** Creates an empty import model with no registered import. */
	public PyImportModel() {}

	/** Model elements to import, with the type names requested from each one. */
	private final Map<PyClassElement, Set<String>> modelTypes = new TreeMap<>(
			Comparator.comparing(element -> element.fileName));
	private final Set<String> typingNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
	private final Set<String> datetimeNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
	private final Set<String> enumNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
	private final Set<String> pydanticNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
	private final Set<String> restToolsNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
	private final List<String> rawImports = new ArrayList<>();
	private boolean uuid = false;
	private boolean futureAnnotations = false;

	/**
	 * Requests a type of a model element.
	 * @param element the element owning the type
	 * @param typeName the type name to import (a variant, an enum, a type alias)
	 */
	public void addModel(final PyClassElement element, final String typeName) {
		this.modelTypes.computeIfAbsent(element, k -> new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER)).add(typeName);
	}

	/**
	 * Requests a name of the {@code typing} module.
	 * @param name the name to import (e.g. {@code Any})
	 */
	public void addTyping(final String name) {
		this.typingNames.add(name);
	}

	/**
	 * Requests a name of the {@code datetime} module.
	 * @param name the name to import (e.g. {@code datetime}, {@code date}, {@code time})
	 */
	public void addDatetime(final String name) {
		this.datetimeNames.add(name);
	}

	/**
	 * Requests a name of the {@code enum} module.
	 * @param name the name to import (e.g. {@code StrEnum})
	 */
	public void addEnum(final String name) {
		this.enumNames.add(name);
	}

	/**
	 * Requests a name of the {@code pydantic} package.
	 * @param name the name to import (e.g. {@code BaseModel}, {@code Field})
	 */
	public void addPydantic(final String name) {
		this.pydanticNames.add(name);
	}

	/**
	 * Requests a name of the generated {@code rest_tools} module.
	 * @param name the name to import (e.g. {@code RESTConfig})
	 */
	public void addRestTools(final String name) {
		this.restToolsNames.add(name);
	}

	/**
	 * Requests a verbatim import line (rare cases such as {@code import uuid}).
	 * @param line the full import statement
	 */
	public void addRaw(final String line) {
		if (!this.rawImports.contains(line)) {
			this.rawImports.add(line);
		}
	}

	/** Requests the {@code uuid} module. */
	public void requestUuid() {
		this.uuid = true;
	}

	/** Requests {@code from __future__ import annotations} (postponed annotations). */
	public void requestFutureAnnotations() {
		this.futureAnnotations = true;
	}

	/**
	 * Checks whether a rest_tools name is requested.
	 * @param name the name to check
	 * @return true when requested
	 */
	public boolean hasRestTools(final String name) {
		return this.restToolsNames.contains(name);
	}

	/**
	 * Returns the requested rest_tools names.
	 * @return the sorted set of names
	 */
	public Set<String> getRestToolsNames() {
		return this.restToolsNames;
	}

	/**
	 * Returns the requested model imports.
	 * @return element to type names, ordered by file name
	 */
	public Map<PyClassElement, Set<String>> getModelTypes() {
		return this.modelTypes;
	}

	private static void appendFromImport(final StringBuilder out, final String module, final Set<String> names) {
		if (names.isEmpty()) {
			return;
		}
		out.append("from ");
		out.append(module);
		out.append(" import ");
		out.append(String.join(", ", names));
		out.append("\n");
	}

	/**
	 * Renders every import except the model ones (which depend on the file location).
	 * @return the standard library and third-party import block (ends with a blank line when not empty)
	 */
	public String renderCommonImports() {
		final StringBuilder out = new StringBuilder();
		if (this.futureAnnotations) {
			out.append("from __future__ import annotations\n\n");
		}
		// isort with force-sort-within-sections (the organisation's setting): `import x`
		// and `from x import y` are interleaved, ordered by module name.
		final Map<String, String> stdlibByModule = new TreeMap<>();
		if (!this.datetimeNames.isEmpty()) {
			stdlibByModule.put("datetime", "from datetime import " + String.join(", ", this.datetimeNames) + "\n");
		}
		if (!this.enumNames.isEmpty()) {
			stdlibByModule.put("enum", "from enum import " + String.join(", ", this.enumNames) + "\n");
		}
		if (!this.typingNames.isEmpty()) {
			stdlibByModule.put("typing", "from typing import " + String.join(", ", this.typingNames) + "\n");
		}
		if (this.uuid) {
			stdlibByModule.put("uuid", "import uuid\n");
		}
		for (final String raw : this.rawImports) {
			stdlibByModule.put(raw.replace("import ", "").strip(), raw + "\n");
		}
		final StringBuilder stdlib = new StringBuilder();
		for (final String line : stdlibByModule.values()) {
			stdlib.append(line);
		}
		if (stdlib.length() != 0) {
			out.append(stdlib);
			out.append("\n");
		}
		if (!this.pydanticNames.isEmpty()) {
			appendFromImport(out, "pydantic", this.pydanticNames);
			out.append("\n");
		}
		return out.toString();
	}

	/**
	 * Renders the model imports of a model file ({@code from .file import ...}).
	 * @param self the element owning the file (never imported)
	 * @param group the group registry, for the deferred imports
	 * @return the import block: eager imports, then the {@code if TYPE_CHECKING:} block
	 */
	public String renderModelFileImports(final PyClassElement self, final PyClassElementGroup group) {
		final StringBuilder eager = new StringBuilder();
		final StringBuilder deferred = new StringBuilder();
		for (final Map.Entry<PyClassElement, Set<String>> entry : this.modelTypes.entrySet()) {
			final PyClassElement element = entry.getKey();
			if (element == self || element.nativeType == PyClassElement.DefinedPosition.NATIVE) {
				continue;
			}
			if (group.isDeferred(self, element)) {
				appendMultilineImport(deferred, "." + element.fileName, entry.getValue(), PyClassElement.INDENT);
			} else {
				appendMultilineImport(eager, "." + element.fileName, entry.getValue(), "");
			}
		}
		final StringBuilder out = new StringBuilder();
		if (!this.restToolsNames.isEmpty()) {
			appendFromImport(out, "..rest_tools", this.restToolsNames);
		}
		out.append(eager);
		if (deferred.length() != 0) {
			if (out.length() != 0) {
				out.append("\n");
			}
			out.append("if TYPE_CHECKING:\n");
			out.append(deferred);
		}
		if (out.length() != 0) {
			out.append("\n");
		}
		return out.toString();
	}

	/**
	 * Renders the local imports of an API file ({@code from ..model import ...}).
	 * @return the import block (ends with a blank line when not empty)
	 */
	public String renderApiFileImports() {
		final StringBuilder out = new StringBuilder();
		final Set<String> modelNames = new TreeSet<>(PyClassElement.IMPORT_NAME_ORDER);
		for (final Map.Entry<PyClassElement, Set<String>> entry : this.modelTypes.entrySet()) {
			if (entry.getKey().nativeType == PyClassElement.DefinedPosition.NATIVE) {
				continue;
			}
			modelNames.addAll(entry.getValue());
		}
		appendMultilineImport(out, "..model", modelNames);
		appendMultilineImport(out, "..rest_tools", this.restToolsNames);
		if (out.length() != 0) {
			out.append("\n");
		}
		return out.toString();
	}

	/**
	 * Appends a {@code from module import a, b} line, wrapped in parentheses when too long.
	 * @param out the output
	 * @param module the module
	 * @param names the imported names (already ordered)
	 */
	public static void appendMultilineImport(final StringBuilder out, final String module, final Set<String> names) {
		appendMultilineImport(out, module, names, "");
	}

	private static void appendMultilineImport(
			final StringBuilder out,
			final String module,
			final Set<String> names,
			final String indent) {
		if (names.isEmpty()) {
			return;
		}
		final String oneLine = indent + "from " + module + " import " + String.join(", ", names);
		if (oneLine.length() <= PyClassElement.LINE_LENGTH) {
			out.append(oneLine);
			out.append("\n");
			return;
		}
		out.append(indent);
		out.append("from ");
		out.append(module);
		out.append(" import (\n");
		for (final String name : names) {
			out.append(indent);
			out.append(PyClassElement.INDENT);
			out.append(name);
			out.append(",\n");
		}
		out.append(indent);
		out.append(")\n");
	}
}
