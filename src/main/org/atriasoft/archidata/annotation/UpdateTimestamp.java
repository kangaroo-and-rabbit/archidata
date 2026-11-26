package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The UpdateTimestamp annotation is used to automatically update the timestamp
 * of an object in the database whenever it is modified. This annotation ensures
 * that the field marked with @UpdateTimestamp is set to the current timestamp
 * each time the object is updated.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to fields within a class.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data persistence logic.
 *
 * <p>Behavior:
 * - When a field is annotated with @UpdateTimestamp, it will automatically
 *   be set to the current date and time when the object is inserted, and updated
 *   whenever the object is modified in the database.
 * - The base classes ({@code GenericTiming} and its subclasses) already declare
 *   it: an entity extending them does not need to repeat it.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyEntity {
 *     @UpdateTimestamp
 *     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
 *     @Nullable
 *     public Date updatedAt = null;
 * }
 * }</pre>
 *
 * In this example, the updatedAt field will be automatically set to the
 * current timestamp when the MyEntity object is inserted and whenever it is modified in the database.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateTimestamp {

}
