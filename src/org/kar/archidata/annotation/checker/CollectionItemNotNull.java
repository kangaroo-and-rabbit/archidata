package org.kar.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = CollectionItemNotNullValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionItemNotNull {
	String message() default "Collection can not contain NULL item";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
