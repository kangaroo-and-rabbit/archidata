package org.atriasoft.archidata.annotation.checker;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReadOnlyFieldValidator implements ConstraintValidator<ReadOnlyField, Object> {

	@Override
	public void initialize(final ReadOnlyField annotation) {
		// nothing to do...
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value != null) {
			return false;
		}
		return true;
	}
}