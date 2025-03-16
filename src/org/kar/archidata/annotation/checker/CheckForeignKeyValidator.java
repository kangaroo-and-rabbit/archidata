package org.kar.archidata.annotation.checker;

import java.util.Collection;

import org.kar.archidata.dataAccess.DataAccess;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckForeignKeyValidator implements ConstraintValidator<CheckForeignKey, Object> {
	Class<?> target = null;

	@Override
	public void initialize(final CheckForeignKey annotation) {
		this.target = annotation.target();
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value != null) {
			return true;
		}
		if (value instanceof final Collection<?> tmpCollection) {
			final Object[] elements = tmpCollection.toArray();
			for (final Object element : elements) {
				if (element == null) {
					continue;
				}
				try {
					final long count = DataAccess.count(this.target, element);
					if (count != 1) {
						return false;

					}
				} catch (final Exception e) {
					// TODO ...
					return false;
				}
			}
		}
		return true;
	}
}