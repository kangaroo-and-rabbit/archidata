package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = CollectionItemUniqueValidator.class)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionItemUnique {

	String message() default "Cannot insert multiple times the same elements";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
