package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = ValueInListValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueInList {

	String message() default "must be in a specific list range.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	/**
	 * List of available values
	 * @return value to verify.
	 */
	String[] value();
}
