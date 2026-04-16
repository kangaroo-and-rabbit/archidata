package org.atriasoft.archidata.annotation.checker;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for the {@link CheckForeignKey} constraint.
 *
 * <p>Checks whether the given foreign key value exists in the database
 * by querying the target entity class.
 */
public class CheckForeignKeyValidator implements ConstraintValidator<CheckForeignKey, Object> {
	Class<?> target = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(CheckForeignKeyValidator.class);

	/** {@inheritDoc} */
	@Override
	public void initialize(final CheckForeignKey annotation) {
		this.target = annotation.target();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		try {
			final boolean exists = DataAccess.existsById(this.target, value);
			if (!exists) {
				return false;
			}
		} catch (final Exception e) {
			LOGGER.error("Fail to access to the DB");
			context.buildConstraintViolationWithTemplate("fail to access on the DB").addConstraintViolation();
			return false;
		}
		return true;
	}
}