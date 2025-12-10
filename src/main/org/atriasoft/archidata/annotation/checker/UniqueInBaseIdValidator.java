package org.atriasoft.archidata.annotation.checker;

import java.util.UUID;

import org.atriasoft.archidata.checker.ValidationContext;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.bson.types.ObjectId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueInBaseIdValidator implements ConstraintValidator<UniqueInBaseId, String> {
	String nameOfField;
	Class<?> target;

	@Override
	public void initialize(final UniqueInBaseId annotation) {
		this.nameOfField = annotation.nameOfField();
		this.target = annotation.target();
	}

	@Override
	public boolean isValid(final String value, final ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		final ValidationContext ctx = ValidationContext.current();
		if (ctx == null) {
			throw new RuntimeException("Context should be defined before call this validation!");
		}
		Condition checkOurselfCondition = null;
		if (ctx.contains("oid")) {
			final ObjectId id = ctx.get("oid", ObjectId.class);
			checkOurselfCondition = id == null ? null : new Condition(new QueryCondition("_id", "!=", id));
		} else if (ctx.contains("id")) {
			final Long id = ctx.get("id", Long.class);
			checkOurselfCondition = id == null ? null : new Condition(new QueryCondition("id", "!=", id));
		} else if (ctx.contains("uuid")) {
			final UUID id = ctx.get("uuid", UUID.class);
			checkOurselfCondition = id == null ? null : new Condition(new QueryCondition("uuid", "!=", id));
		}
		try {
			final long count = DataAccess.count(this.target,
					new Condition(new QueryCondition(this.nameOfField, "=", value)), checkOurselfCondition);
			if (count != 0) {
				return false;
			}
		} catch (final Exception e) {
			context.buildConstraintViolationWithTemplate("fail to access on the DB").addConstraintViolation();
			return false;
		}
		return true;

	}
}
