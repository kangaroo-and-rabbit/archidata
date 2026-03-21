package org.atriasoft.archidata.annotation.apiGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that specifies access limitations for a field in API operations.
 *
 * <p>Controls whether a field is exposed during read, create, or update operations
 * in the generated API.
 *
 * @deprecated since v0.30.0, scheduled for removal
 */
@Retention(RUNTIME)
@Target(FIELD)
@Deprecated(since = "v0.30.0", forRemoval = true)
public @interface ApiAccessLimitation {
	/**
	 * Whether the field is accessible in read (GET) operations.
	 * @return true if the field is readable, true by default
	 */
	boolean readable() default true;

	/**
	 * Whether the field is accessible in creation (POST) operations.
	 * @return true if the field is creatable, true by default
	 */
	boolean creatable() default true;

	/**
	 * Whether the field is accessible in update (PUT, PATCH) operations.
	 * @return true if the field is updatable, true by default
	 */
	boolean updatable() default true;
}
