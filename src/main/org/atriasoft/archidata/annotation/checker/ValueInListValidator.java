package org.atriasoft.archidata.annotation.checker;

import java.util.Arrays;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link ValueInList} constraint.
 *
 * <p>Checks whether the given string value is contained in the predefined list
 * of allowed values specified by the annotation.
 */
public class ValueInListValidator implements ConstraintValidator<ValueInList, String> {
	List<String> values;

	/** {@inheritDoc} */
	@Override
	public void initialize(final ValueInList annotation) {
		this.values = Arrays.asList(annotation.value());
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return this.values.contains(value);

	}
}
