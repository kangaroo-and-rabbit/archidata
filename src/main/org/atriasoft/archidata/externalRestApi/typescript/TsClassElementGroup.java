package org.atriasoft.archidata.externalRestApi.typescript;

import java.util.List;

import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ParameterClassModel;

import jakarta.validation.Valid;

public class TsClassElementGroup {
	private final List<TsClassElement> tsElements;

	public List<TsClassElement> getTsElements() {
		return this.tsElements;
	}

	public TsClassElementGroup(final List<TsClassElement> tsElements) {
		this.tsElements = tsElements;
	}

	public TsClassElement find(final ClassModel model) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(model)) {
				return elem;
			}
		}
		return null;
	}

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

	public ParameterClassModel find(final boolean valid, final Class<?>[] validGroup, final ClassModel parameterModel) {
		for (final TsClassElement elem : this.tsElements) {
			if (elem.isCompatible(parameterModel)) {
				return elem.getParameterClassModel(valid, validGroup, parameterModel);
			}
		}
		return null;
	}
}
