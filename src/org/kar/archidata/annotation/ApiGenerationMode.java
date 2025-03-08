package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The ApiGenerationMode annotation is used to indicate the generation mode for
 * API operations when producing code for other languages. This annotation is
 * particularly useful in code generators for client libraries where specific
 * data structures for read, create, and update operations may or may not be needed.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to class types.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle code generation logic.
 *
 * <p>Behavior:
 * - When applied to a class, the ApiGenerationMode annotation specifies
 *   which API operations (read, create, update) should generate specific
 *   data structures. This can simplify the generated code by avoiding the
 *   creation of unnecessary structures.
 *
 * <p>Example:
 * <pre>{@code
 * @ApiGenerationMode(creatable=false, updatable=false)
 * public class User {
 *     public String username;
 *     public String email;
 * }
 * }</pre>
 *
 * In this example, the User class will not generate separate data structures
 * for create and update operations in the client code, only for read operations.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiGenerationMode {
	/**
	 * (Optional) Enable the generation of specific code for read access
	 * (generate object: MyClass).
	 */
	boolean read() default true;

	/**
	 * (Optional) Enable the generation of specific code for create access
	 * (generate object: MyClassCreate).
	 */
	boolean create() default false;

	/**
	 * (Optional) Enable the generation of specific code for update access
	 * (generate object: MyClassUpdate).
	 */
	boolean update() default false;
}
