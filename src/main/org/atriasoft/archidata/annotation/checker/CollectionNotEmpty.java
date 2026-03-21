package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint that ensures a collection is not empty.
 *
 * <p>Null values are considered valid; use {@code @NotNull} in combination
 * if null collections should also be rejected.
 */
@Constraint(validatedBy = CollectionNotEmptyValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionNotEmpty {

	/**
	 * The error message to use when validation fails.
	 * @return the error message
	 */
	String message() default "Collection can not be empty";

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
}
