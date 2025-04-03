package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The DataNotRead annotation is used to mark fields in a class that should not
 * be automatically read from the database. This annotation helps in optimizing
 * data retrieval by excluding certain fields from being fetched unless
 * explicitly specified.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to fields within a class.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data retrieval logic.
 *
 * <p>Behavior:
 * - When a field is annotated with @DataNotRead, it will not be included in the
 *   default data retrieval process from the database.
 * - To override this behavior and read all columns, including those marked with
 *   `@DataNotRead`, the query must include the option ReadAllColumn.
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
 * In this example, the sensitiveData field will not be read from the database
 * by default. To include it in the query results, the ReadAllColumn option must
 * be specified in the query.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataNotRead {

}
