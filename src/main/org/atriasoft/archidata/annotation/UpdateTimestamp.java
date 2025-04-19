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
 *   be updated to the current date and time whenever the object is modified
 *   in the database.
 * - This annotation is typically used in conjunction with other annotations
 *   such as @Column to define database column properties.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyEntity {
 *     @UpdateTimestamp
 *     @Column(nullable = false, insertable = false, updatable = false)
 *     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
 *     @Nullable
 *     public Date updatedAt = null;
 * }
 * }</pre>
 *
 * In this example, the updatedAt field will be automatically set to the
 * current timestamp whenever the User object is modified in the database.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateTimestamp {

}
