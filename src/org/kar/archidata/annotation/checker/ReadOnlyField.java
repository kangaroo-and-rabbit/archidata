package org.kar.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = ReadOnlyFieldValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnlyField {
	String message() default "Field can not be set, it is a read-only field.";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}