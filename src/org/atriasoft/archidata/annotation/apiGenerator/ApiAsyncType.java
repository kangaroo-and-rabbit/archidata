package org.atriasoft.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** In case of the update parameter with String input to detect null element. */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAsyncType {
	// Possible class values.
	Class<?>[] value();

	// direct copy value in the TypeScript (separate with type by a |
	String[] tsComplement() default {};
}
