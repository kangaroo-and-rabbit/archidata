package org.atriasoft.archidata.externalRestApi.model;

import java.util.List;

import org.atriasoft.archidata.annotation.checker.ValidGroup;

import jakarta.validation.Valid;

public record ParameterClassModelList(
		boolean valid,
		Class<?>[] groups,
		List<ClassModel> models,
		boolean optional) {
	public ParameterClassModelList(final boolean valid, final Class<?>[] groups, final List<ClassModel> models,
			final boolean optional) {
		this.valid = valid;
		this.groups = groups;
		this.models = models;
		this.optional = optional;
	}

	public ParameterClassModelList(final Valid validParam, final ValidGroup validGroupParam,
			final List<ClassModel> parameterModel, final boolean apiInputOptional) {
		this(validParam != null, validGroupParam == null ? null : validGroupParam.value(), parameterModel,
				apiInputOptional);
	}

}
