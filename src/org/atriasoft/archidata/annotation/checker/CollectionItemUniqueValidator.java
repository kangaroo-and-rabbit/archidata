package org.atriasoft.archidata.annotation.checker;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CollectionItemUniqueValidator implements ConstraintValidator<CollectionItemUnique, Object> {

	@Override
	public void initialize(final CollectionItemUnique annotation) {
		// nothing to do...
	}

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