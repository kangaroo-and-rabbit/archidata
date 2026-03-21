package org.atriasoft.archidata.externalRestApi.python;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Registry for Python class elements.
 * Provides lookup functionality for model-to-element mapping.
 */
public class PyClassElementGroup {

	private final List<PyClassElement> elements;

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
}
