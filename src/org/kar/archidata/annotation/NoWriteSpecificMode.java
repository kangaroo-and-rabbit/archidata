package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The NoWriteSpecificMode annotation is used to indicate that there is no
 * specific API for write mode when generating code for other languages. This
 * annotation is particularly useful in code generators for client libraries
 * where a separate, reduced data structure for write operations is not needed.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to class types.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle code generation logic.
 *
 * <p>Behavior:
 * - When applied to a class, the NoWriteSpecificMode annotation specifies
 *   that the class does not require a separate API or data structure for
 *   write operations. This can simplify the generated code by avoiding the
 *   creation of redundant structures.
 *
 * <p>Example:
 * <pre>{@code
 * @NoWriteSpecificMode
 * public class User {
 *     public String username;
 *     public String email;
 * }
 * }</pre>
 *
 * In this example, the User class will not generate a separate API or data
 * structure for write operations in the client code.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoWriteSpecificMode {

}
