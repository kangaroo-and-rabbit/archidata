package org.atriasoft.archidata.externalRestApi.typescript;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;
import org.atriasoft.archidata.externalRestApi.typescript.TsClassElement.DefinedPosition;

import jakarta.validation.Valid;

/**
 * Registry for TypeScript class elements.
 * Provides lookup functionality for model-to-element mapping.
 */
public class TsClassElementGroup {
	private final List<TsClassElement> tsElements;
	private final boolean zodEnum;

	/**
	 * Gets all registered TypeScript class elements.
	 * @return the immutable list of all registered elements
	 */
	public List<TsClassElement> getTsElements() {
		return this.tsElements;
	}

	/**
	 * Constructs a new group with the given list of TypeScript class elements.
	 * @param tsElements the list of TypeScript class elements to register, copied
	 */
	public TsClassElementGroup(final List<TsClassElement> tsElements) {
		this(tsElements, false);
	}

	/**
	 * Constructs a new group with the given list of TypeScript class elements.
	 * @param tsElements the list of TypeScript class elements to register, copied
	 * @param zodEnum true to generate the enums as {@code zod.enum([...])} instead of a TypeScript enum
	 */
	public TsClassElementGroup(final List<TsClassElement> tsElements, final boolean zodEnum) {
		this.tsElements = List.copyOf(tsElements);
		this.zodEnum = zodEnum;
	}

	/**
	 * Tells how the enums are generated.
	 * @return true when the enums are generated as {@code zod.enum([...])}
	 */
	public boolean isZodEnum() {
		return this.zodEnum;
	}

	/**
	 * Registers on every parent the variants its children use: a child variant ({@code ChildCreate}) extends the same
	 * variant of its parent ({@code ParentCreate}), which the file of the parent must therefore export, whatever the
	 * order the files are generated. Call it once the API files have registered the variants they need and before the
	 * model files are generated.
	 */
	public void propagateVariantsToParents() {
		boolean changed = true;
		while (changed) {
			changed = false;
			for (final TsClassElement element : this.tsElements) {
				if (element.nativeType != DefinedPosition.NORMAL
						|| !(element.models.get(0) instanceof final ClassObjectModel model)
						|| model.getExtendsClass() == null) {
					continue;
				}
				final TsClassElement parent = find(model.getExtendsClass());
				if (parent == null || parent.nativeType != DefinedPosition.NORMAL) {
					continue;
				}
				// A child without any registered variant generates its default one
				final List<ParameterClassModel> variants = element.requestedModels.isEmpty()
						? List.of(new ParameterClassModel(model))
						: new ArrayList<>(element.requestedModels);
				for (final ParameterClassModel variant : variants) {
					final int before = parent.requestedModels.size();
					parent.getParameterClassModel(variant.valid(), variant.groups(), model.getExtendsClass());
					if (parent.requestedModels.size() != before) {
						changed = true;
					}
				}
			}
		}
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
