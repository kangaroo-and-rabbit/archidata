package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The DataJson annotation is used to convert fields or classes to JSON format
 * for storage in a database. This annotation allows storing complex data types
 * such as lists, maps, and other objects in SQL databases as JSON or STRING
 * (for SQLite).
 *
 * <p>Usage:
 * - Target: This annotation can be applied to both fields and classes.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data persistence logic.
 *
 * <p>Behavior:
 * - When applied to a field or class, the DataJson annotation enables the
 *   conversion of the annotated element to JSON format before storing it in
 *   the database.
 * - This is particularly useful in SQL databases where only basic data types
 *   (char, short, int, long, float, string, timestamp) can be stored directly.
 *   The DataJson annotation makes it possible to store complex data structures
 *   by converting them to JSON.
 *
 * <p>Attributes:
 * - targetEntity: Specifies the target entity class to which the JSON data
 *   should be mapped. Defaults auto-detect if not specified.
 *
 * <p>Example:
 * <pre>{@code
 * public class User {
 *     @DataJson
 *     public Map<String, Object> additionalData;
 * }
 * }</pre>
 *
 * In this example, the additionalData field can store complex data structures
 * as JSON in the database.
 */
@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface DataJson {
	Class<?> targetEntity() default Void.class;
}
