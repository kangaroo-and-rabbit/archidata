package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The DataDeleted annotation is used to manage a boolean variable that marks
 * an object as 'deleted' in the database. This annotation helps in soft deletion
 * by excluding marked objects from being automatically retrieved unless
 * explicitly specified.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to fields within a class.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data retrieval logic.
 *
 * <p>Behavior:
 * - When a field is annotated with @DataDeleted, it will not be included in the
 *   default data retrieval process from the database if its value is false.
 * - To override this behavior and access deleted items, the query must include
 *   the option AccessDeletedItems.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyEntity {
 *     public String username;
 *
 *     @DataDeleted
 *     @DataNotRead
 *     @Column(nullable = false)
 *     @DefaultValue("'0'")
 *     public Boolean deleted = null;
 * }
 * }</pre>
 *
 * In this example, objects with `deleted` set to true will not be retrieved
 * by default. To include them in the query results, the AccessDeletedItems
 * option must be specified in the query.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataDeleted {

}
