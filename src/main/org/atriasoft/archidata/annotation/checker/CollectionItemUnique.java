package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint that ensures all items in a collection are unique.
 *
 * <p>Null values are considered valid; use {@code @NotNull} in combination
 * if null collections should also be rejected.
 */
@Constraint(validatedBy = CollectionItemUniqueValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionItemUnique {

	/**
	 * The error message to use when validation fails.
	 * @return the error message
	 */
	String message() default "Cannot insert multiple times the same elements";

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
