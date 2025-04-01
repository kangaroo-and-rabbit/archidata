package org.kar.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = CheckForeignKeyValidator.class)
@Target({ ElementType.TYPE, //
		ElementType.METHOD, //
		ElementType.FIELD, //
		ElementType.ANNOTATION_TYPE, //
		ElementType.CONSTRUCTOR, //
		ElementType.PARAMETER, //
		ElementType.TYPE_USE, //
})

@Retention(RetentionPolicy.RUNTIME)
public @interface CheckForeignKey {
	Class<?> target();

	String message() default "Foreign-key does not exist in the DB";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
