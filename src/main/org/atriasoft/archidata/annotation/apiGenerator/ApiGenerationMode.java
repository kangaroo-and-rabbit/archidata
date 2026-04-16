package org.atriasoft.archidata.annotation.apiGenerator;

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
	 * Enable the generation of specific code for read access (generates object: MyClass).
	 * @return true if a read data structure should be generated, true by default
	 */
	boolean read() default true;

	/**
	 * Enable the generation of specific code for create access (generates object: MyClassCreate).
	 * @return true if a create data structure should be generated, false by default
	 */
	boolean create() default false;

	/**
	 * Enable the generation of specific code for update access (generates object: MyClassUpdate).
	 * @return true if an update data structure should be generated, false by default
	 */
	boolean update() default false;

}
