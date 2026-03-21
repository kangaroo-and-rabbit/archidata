package org.atriasoft.archidata.annotation.checker;

import java.util.Collection;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link CollectionNotEmpty} constraint.
 *
 * <p>Checks that a collection, if not null, contains at least one element.
 */
public class CollectionNotEmptyValidator implements ConstraintValidator<CollectionNotEmpty, Object> {

	/** {@inheritDoc} */
	@Override
	public void initialize(final CollectionNotEmpty annotation) {
		// nothing to do...
	}

	/** {@inheritDoc} */
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