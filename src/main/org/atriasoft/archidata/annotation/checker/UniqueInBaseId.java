package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint that checks whether a value is unique in the database for a given entity field.
 *
 * <p>Excludes the current entity (identified by its ID from the validation context) from the uniqueness check,
 * allowing updates to pass validation when the value has not changed.
 */
@Constraint(validatedBy = UniqueInBaseIdValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueInBaseId {

	/**
	 * The error message to use when validation fails.
	 * @return the error message
	 */
	String message() default "Value already exists in the DB.";

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
	 * Name of the field in the target class to check for uniqueness.
	 * @return the field name, defaults to "name"
	 */
	String nameOfField() default "name";

	/**
	 * The target entity class used to query the database.
	 * @return the target class
	 */
	Class<?> target();
}
