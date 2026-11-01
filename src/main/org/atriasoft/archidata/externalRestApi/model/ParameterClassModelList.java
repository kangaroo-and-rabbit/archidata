package org.atriasoft.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.atriasoft.archidata.annotation.checker.ValidGroup;

import jakarta.validation.Valid;

/**
 * Represents a list of API parameter class models with shared validation settings.
 *
 * @param valid whether the parameters should be validated
 * @param groups the validation groups to apply
 * @param models the list of class models representing the parameter types
 * @param optional whether the parameter is optional
 */
public record ParameterClassModelList(
		boolean valid,
		Class<?>[] groups,
		List<ClassModel> models,
		boolean optional) {
	/**
	 * Constructs a parameter class model list with explicit settings.
	 * @param valid whether the parameters should be validated
	 * @param groups the validation groups to apply
	 * @param models the list of class models, copied
	 * @param optional whether the parameter is optional
	 */
	public ParameterClassModelList(final boolean valid, final Class<?>[] groups, final List<ClassModel> models,
			final boolean optional) {
		this.valid = valid;
		this.groups = groups == null ? null : groups.clone();
		this.models = Collections.unmodifiableList(new ArrayList<>(models));
		this.optional = optional;
	}

	/**
	 * The validation groups to apply.
	 * @return a copy of the groups, or {@code null} if none was declared
	 */
	@Override
	public Class<?>[] groups() {
		return this.groups == null ? null : this.groups.clone();
	}

	/**
	 * Constructs a parameter class model list from Jakarta validation annotations.
	 * @param validParam the {@code @Valid} annotation, or {@code null} if not present
	 * @param validGroupParam the {@code @ValidGroup} annotation, or {@code null} if not present
	 * @param parameterModel the list of class models
	 * @param apiInputOptional whether the parameter is optional
	 */
	public ParameterClassModelList(final Valid validParam, final ValidGroup validGroupParam,
			final List<ClassModel> parameterModel, final boolean apiInputOptional) {
		this(validParam != null, validGroupParam == null ? null : validGroupParam.value(), parameterModel,
				apiInputOptional);
	}

}
