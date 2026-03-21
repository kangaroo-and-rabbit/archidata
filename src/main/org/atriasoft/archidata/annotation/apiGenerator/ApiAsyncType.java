package org.atriasoft.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies asynchronous type information for API parameters or methods.
 *
 * <p>Used in code generation to handle update parameters with String input
 * where null element detection is needed.
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAsyncType {
	/**
	 * The possible class types for the async parameter.
	 * @return an array of class types
	 */
	Class<?>[] value();

	/**
	 * Direct copy values to include in TypeScript output, separated by a pipe ({@code |}) from the main type.
	 * @return an array of TypeScript type complements, empty by default
	 */
	String[] tsComplement() default {};
}
