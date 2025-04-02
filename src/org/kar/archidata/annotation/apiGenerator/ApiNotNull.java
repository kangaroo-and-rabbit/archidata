package org.kar.archidata.annotation.apiGenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to explicitly define the nullability of a parameter in an API.
 *
 * This annotation allows marking a parameter as required (non-null) or optional (nullable),
 * overriding any other nullability considerations. It is useful in API generation frameworks
 * to ensure precise validation and documentation of method parameters.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to field.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle code generation logic.
 *
 * <p>Behavior:
 * - When applied to a parameter, it explicitly marks it as optional or required in the API.
 *   This annotation overrides all other considerations regarding nullability.
 *
 * <p>Example:
 * <pre>{@code
 * public class User {
 *     @ReadOnlyField
 *     @ApiNotNull
 *     public String username;
 *     public String email;
 * }
 * }</pre>
 *
 * In this example, the `username` field in the `User` class is explicitly marked as non-null in the generated API.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiNotNull {
	/**
	 * (Optional) Specifies whether the API element can be null.
	 * If set to `true`, the element is required (non-null).
	 * If set to `false`, the element is optional (nullable).
	 */
	boolean value() default true;
}