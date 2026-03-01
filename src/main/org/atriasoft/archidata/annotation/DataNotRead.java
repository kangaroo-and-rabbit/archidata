package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The DataNotRead annotation is used to mark fields in a class that should not
 * be automatically read from the database. The field is still written to the
 * database during insert and update operations.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to fields within a class.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data retrieval logic.
 *
 * <p>Behavior:
 * - The field IS written to the database on insert and update (like a normal field).
 * - The field is NOT included in the default data retrieval process.
 * - To read the field, the query must include the {@code ReadAllColumn} option.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyEntity {
 *     public String username;
 *
 *     @DataNotRead
 *     private String sensitiveData;
 * }
 * }</pre>
 *
 * In this example, the sensitiveData field will be stored in the database but
 * will not be read by default. To include it in the query results, the
 * ReadAllColumn option must be specified in the query.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataNotRead {

}
