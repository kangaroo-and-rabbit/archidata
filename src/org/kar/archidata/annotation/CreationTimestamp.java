package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The CreationTimestamp annotation is used to automatically set the creation
 * date of an object in the database. This annotation ensures that the field
 * marked with @CreationTimestamp is populated with the current timestamp
 * when the object is first created.
 *
 * <p>Usage:
 * - Target: This annotation can be applied to fields within a class.
 * - Retention: The annotation is retained at runtime, allowing it to be
 *   processed by frameworks or libraries that handle data persistence logic.
 *
 * <p>Behavior:
 * - When a field is annotated with @CreationTimestamp, it will automatically
 *   be set to the current date and time when the object is inserted into the
 *   database.
 * - This annotation is typically used in conjunction with other annotations
 *   such as @Column to define database column properties.
 *
 * <p>Example:
 * <pre>{@code
 * public class MyEntity {
 *     @DataNotRead
 *     @CreationTimestamp
 *     @Column(nullable = false, insertable = false, updatable = false)
 *     @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX") // optional depend on the configuration
 *     @Nullable
 *     public Date createdAt = null;
 * }
 * }</pre>
 *
 * In this example, the createdAt field will be automatically set to the
 * current timestamp when a new User object is created in the database.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationTimestamp {

}
