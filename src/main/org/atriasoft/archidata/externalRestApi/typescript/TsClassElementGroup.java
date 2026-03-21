package org.atriasoft.archidata.externalRestApi.typescript;

import java.util.List;

import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;

import jakarta.validation.Valid;

/**
 * Registry for TypeScript class elements.
 * Provides lookup functionality for model-to-element mapping.
 */
public class TsClassElementGroup {
	private final List<TsClassElement> tsElements;

	/**
	 * Gets all registered TypeScript class elements.
	 * @return the list of all registered elements
	 */
	public List<TsClassElement> getTsElements() {
		return this.tsElements;
	}

	/**
	 * Constructs a new group with the given list of TypeScript class elements.
	 * @param tsElements the list of TypeScript class elements to register
	 */
	public TsClassElementGroup(final List<TsClassElement> tsElements) {
		this.tsElements = tsElements;
	}

	/**
	 * Finds the TsClassElement for a given ClassModel.
	 * @param model the class model to look up
	 * @return the matching TsClassElement, or null if not found
	 */
	public TsClassElement find(final ClassModel model) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(model)) {
				return elem;
			}
		}
		return null;
	}

	/**
	 * Finds a ParameterClassModel by looking up the compatible element using Jakarta validation annotations.
	 * @param validParam the Valid annotation (may be null)
	 * @param validGroupParam the ValidGroup annotation (may be null)
	 * @param parameterModel the class model to look up
	 * @return the ParameterClassModel from the matching element, or null if not found
	 */
	public ParameterClassModel find(
			final Valid validParam,
			final ValidGroup validGroupParam,
			final ClassModel parameterModel) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(parameterModel)) {
				return elem.getParameterClassModel(validParam, validGroupParam, parameterModel);
			}
		}
		return null;
	}

	/**
	 * Finds a ParameterClassModel by looking up the compatible element using validation context.
	 * @param valid whether validation is active
	 * @param validGroup the validation groups
	 * @param parameterModel the class model to look up
	 * @return the ParameterClassModel from the matching element, or null if not found
	 */
	public ParameterClassModel find(final boolean valid, final Class<?>[] validGroup, final ClassModel parameterModel) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(parameterModel)) {
				return elem.getParameterClassModel(valid, validGroup, parameterModel);
			}
		}
		return null;
	}
}
