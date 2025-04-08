package org.atriasoft.archidata.annotation.checker;

import java.util.Collection;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CollectionItemNotNullValidator implements ConstraintValidator<CollectionItemNotNull, Object> {

	@Override
	public void initialize(final CollectionItemNotNull annotation) {
		// nothing to do...
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		if (value instanceof final Collection<?> tmpCollection) {
			final Object[] elements = tmpCollection.toArray();
			for (final Object element : elements) {
				if (element == null) {
					return false;
					//throw new InputException(baseName + fieldName + '[' + iii + ']', "Collection can not contain NULL item");
				}
			}
		}
		return true;
	}
}