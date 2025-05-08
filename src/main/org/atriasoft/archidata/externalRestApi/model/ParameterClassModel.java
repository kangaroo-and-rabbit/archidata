package org.atriasoft.archidata.externalRestApi.model;

import org.atriasoft.archidata.annotation.checker.ValidGroup;

import jakarta.validation.Valid;

public record ParameterClassModel(
		boolean valid,
		Class<?>[] groups,
		ClassModel model) {
	public ParameterClassModel(final boolean valid, final Class<?>[] groups, final ClassModel model) {
		this.valid = valid;
		this.groups = groups;
		this.model = model;
	}

	public ParameterClassModel(final Valid validParam, final ValidGroup validGroupParam,
			final ClassModel parameterModel) {
		this(validParam != null, validGroupParam == null ? null : validGroupParam.value(), parameterModel);
	}

}
