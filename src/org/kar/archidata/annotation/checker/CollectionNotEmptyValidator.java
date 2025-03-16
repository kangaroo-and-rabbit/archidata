package org.kar.archidata.annotation.checker;

import java.util.Collection;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CollectionNotEmptyValidator implements ConstraintValidator<CollectionNotEmpty, Object> {

	@Override
	public void initialize(final CollectionNotEmpty annotation) {
		// nothing to do...
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (value instanceof final Collection<?> tmpCollection) {
			if (tmpCollection.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}