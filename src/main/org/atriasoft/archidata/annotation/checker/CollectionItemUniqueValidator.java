package org.atriasoft.archidata.annotation.checker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link CollectionItemUnique} constraint.
 *
 * <p>Checks that all elements in a collection are unique by comparing the collection
 * size to a set constructed from its elements.
 */
public class CollectionItemUniqueValidator implements ConstraintValidator<CollectionItemUnique, Object> {

	/** {@inheritDoc} */
	@Override
	public void initialize(final CollectionItemUnique annotation) {
		// nothing to do...
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (value instanceof final Collection<?> tmpCollection) {
			final Set<Object> uniqueValues = new HashSet<>(tmpCollection);
			if (uniqueValues.size() != tmpCollection.size()) {
				return false;
			}
		}
		return true;
	}
}