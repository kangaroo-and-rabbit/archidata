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
	 * @deprecated not used by archidata: no generator reads it, the TypeScript type comes from {@link #value()} only.
	 */
	@Deprecated(since = "0.55.0", forRemoval = true)
	String[] tsComplement() default {};
}
