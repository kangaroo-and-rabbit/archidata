package org.atriasoft.archidata.annotation.apiGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Unified documentation annotation for the archidata API generators.
 *
 * <p>Replaces the use of Swagger's {@code @Schema} (on classes and fields) and
 * {@code @Operation} (on methods) for documentation purposes. The archidata
 * code generators (TypeScript, Python, OpenAPI ...) read this annotation to
 * produce descriptions, examples, and grouping information.
 *
 * <p>When this annotation is absent the generators fall back to
 * {@code @Schema} / {@code @Operation} with a deprecation warning.
 *
 * <p>Usage examples:
 * <pre>{@code
 * @ApiDoc(description = "A data stream track", example = "{\"type\":\"VIDEO\"}")
 * public class DataStream { ... }
 *
 * @ApiDoc(description = "Unique Id of the object", example = "123456")
 * private Long id;
 *
 * @ApiDoc(description = "Upload data in the system", group = "SYSTEM")
 * public ObjectId uploadMedia(...) { ... }
 * }</pre>
 */
@Retention(RUNTIME)
@Target({ TYPE, FIELD, METHOD })
public @interface ApiDoc {
	/**
	 * Description of the class, field, or API method.
	 * @return the description text, empty by default
	 */
	String description() default "";

	/**
	 * Example value (applicable to classes and fields).
	 * @return the example text, empty by default
	 */
	String example() default "";

	/**
	 * Logical group or tag for API methods.
	 * @return the group name, empty by default
	 */
	String group() default "";

	/**
	 * If true, the element is hidden from generated API documentation.
	 * @return whether the element should be hidden, false by default
	 */
	boolean hidden() default false;
}
