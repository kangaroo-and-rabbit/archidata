package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint that checks whether a string value is contained in a predefined list of allowed values.
 */
@Constraint(validatedBy = ValueInListValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValueInList {

	/**
	 * The error message to use when validation fails.
	 * @return the error message
	 */
	String message() default "must be in a specific list range.";

	/**
	 * The validation groups this constraint belongs to.
	 * @return an array of validation group classes
	 */
	Class<?>[] groups() default {};

	/**
	 * The payload associated with this constraint.
	 * @return an array of payload classes
	 */
	Class<? extends Payload>[] payload() default {};

	/**
	 * List of allowed values that the annotated element must match.
	 * @return the array of allowed values
	 */
	String[] value();
}
