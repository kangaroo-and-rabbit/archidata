package org.atriasoft.archidata.annotation.checker;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckForeignKeyValidator implements ConstraintValidator<CheckForeignKey, Object> {
	Class<?> target = null;
	private final static Logger LOGGER = LoggerFactory.getLogger(CheckForeignKeyValidator.class);

	@Override
	public void initialize(final CheckForeignKey annotation) {
		this.target = annotation.target();
	}

	@Override
	public boolean isValid(final Object value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		try {
			final long count = DataAccess.count(this.target, value);
			if (count != 1) {
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