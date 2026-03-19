package org.atriasoft.archidata.externalRestApi.python;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Registry for Python class elements.
 * Provides lookup functionality for model-to-element mapping.
 */
public class PyClassElementGroup {

	private final List<PyClassElement> elements;

	public PyClassElementGroup(final List<PyClassElement> elements) {
		this.elements = elements;
	}

	/**
	 * Find the PyClassElement for a given ClassModel.
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
	 * Get all registered elements.
	 */
	public List<PyClassElement> getPyElements() {
		return this.elements;
	}
}
