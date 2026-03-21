package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint that checks whether a foreign key value exists in the database.
 *
 * <p>When applied to a field or parameter, the validator queries the database
 * to verify that the referenced entity exists in the target class table.
 */
@Constraint(validatedBy = CheckForeignKeyValidator.class)
@Target({ ElementType.TYPE, //
		ElementType.METHOD, //
		ElementType.FIELD, //
		ElementType.PARAMETER, //
		ElementType.TYPE_USE, //
})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckForeignKey {
	/**
	 * The target entity class that the foreign key references.
	 * @return the target class
	 */
	Class<?> target();

	/**
	 * The error message to use when validation fails.
	 * @return the error message
	 */
	String message() default "Foreign-key does not exist in the DB";

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
