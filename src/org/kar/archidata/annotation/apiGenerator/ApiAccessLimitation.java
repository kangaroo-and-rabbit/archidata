package org.kar.archidata.annotation.apiGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface ApiAccessLimitation {
	/**
	 * (Optional) The field is accessible in read (GET)
	 */
	boolean readable() default true;

	/**
	 * (Optional) The field is accessible in creation (POST)
	 */
	boolean creatable() default true;

	/**
	 * (Optional) The field is accessible in update mode (PUT, PATCH)
	 */
	boolean updatable() default true;
}
