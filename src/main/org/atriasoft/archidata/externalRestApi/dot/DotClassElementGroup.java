package org.atriasoft.archidata.externalRestApi.dot;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Registry for Graphviz DOT class elements.
 * Provides lookup functionality for model-to-element mapping.
 */
public class DotClassElementGroup {
	private final List<DotClassElement> dotElements;

	/**
	 * Gets all registered DOT class elements.
	 * @return the list of all registered elements
	 */
	public List<DotClassElement> getDotElements() {
		return this.dotElements;
	}

	/**
	 * Constructs a new group with the given list of DOT class elements.
	 * @param tsElements the list of DOT class elements to register
	 */
	public DotClassElementGroup(final List<DotClassElement> tsElements) {
		this.dotElements = tsElements;
	}

	/**
	 * Finds the DotClassElement for a given ClassModel.
	 * @param model the class model to look up
	 * @return the matching DotClassElement, or null if not found
	 */
	public DotClassElement find(final ClassModel model) {
		for (final DotClassElement elem : this.dotElements) {
			if (elem.isCompatible(model)) {
				return elem;
			}
		}
		return null;
	}

}
