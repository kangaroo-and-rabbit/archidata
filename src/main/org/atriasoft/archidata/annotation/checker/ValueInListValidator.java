package org.atriasoft.archidata.annotation.checker;

import java.util.Arrays;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValueInListValidator implements ConstraintValidator<ValueInList, String> {
	List<String> values;

	@Override
	public void initialize(final ValueInList annotation) {
		this.values = Arrays.asList(annotation.value());
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return this.values.contains(value);

	}
}
