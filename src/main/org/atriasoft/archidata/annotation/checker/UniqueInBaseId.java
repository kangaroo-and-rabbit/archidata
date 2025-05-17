package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = UniqueInBaseIdValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueInBaseId {

	String message() default "Value already exists in the DB.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	/**
	 * Name Of the field in the class
	 * @return Field name.
	 */
	String nameOfField() default "name";

	/**
	 * Need to determine the Class to access to the DB
	 * @return the class to access on the DB
	 */
	Class<?> target();
}
