package org.atriasoft.archidata.externalRestApi.python;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Registry of the Python class elements of one generation.
 *
 * <p>Besides the model-to-element lookup, it records the imports that must be
 * deferred (declared under {@code if TYPE_CHECKING:}) to break the import
 * cycles between model files: Python executes imports eagerly, so two model
 * modules importing each other at load time would fail.
 */
public class PyClassElementGroup {

	private final List<PyClassElement> elements;
	private final Map<PyClassElement, Set<PyClassElement>> deferredImports = new HashMap<>();

	/**
	 * Constructs a new group with the given list of Python class elements.
	 * @param elements the list of Python class elements to register
	 */
	public PyClassElementGroup(final List<PyClassElement> elements) {
		this.elements = elements;
	}

	/**
	 * Finds the PyClassElement for a given ClassModel.
	 * @param model the class model to look up
	 * @return the matching PyClassElement, or null if not found
	 */
	public PyClassElement find(final ClassModel model) {
		for (final PyClassElement element : this.elements) {
			if (element.isCompatible(model)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Gets all registered Python class elements.
	 * @return the list of all registered elements
	 */
	public List<PyClassElement> getPyElements() {
		return this.elements;
	}

	/**
	 * Records that {@code importer} must import {@code imported} lazily (type-checking only).
	 * @param importer the element whose file holds the import
	 * @param imported the element that would close an import cycle
	 */
	public void defer(final PyClassElement importer, final PyClassElement imported) {
		this.deferredImports.computeIfAbsent(importer, k -> new LinkedHashSet<>()).add(imported);
	}

	/**
	 * Checks whether the import of {@code imported} by {@code importer} is deferred.
	 * @param importer the element whose file holds the import
	 * @param imported the imported element
	 * @return true when the import must be declared under {@code if TYPE_CHECKING:}
	 */
	public boolean isDeferred(final PyClassElement importer, final PyClassElement imported) {
		final Set<PyClassElement> deferred = this.deferredImports.get(importer);
		return deferred != null && deferred.contains(imported);
	}

	/**
	 * Checks whether an element holds at least one deferred import (its models need a rebuild).
	 * @param importer the element to check
	 * @return true when the element's file has a {@code TYPE_CHECKING} import block
	 */
	public boolean hasDeferredImports(final PyClassElement importer) {
		final Set<PyClassElement> deferred = this.deferredImports.get(importer);
		return deferred != null && !deferred.isEmpty();
	}
}
