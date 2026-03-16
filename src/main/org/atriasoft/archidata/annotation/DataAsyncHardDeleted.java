package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a boolean field as an asynchronous hard-delete flag.
 *
 * <p>When {@code deleteHard} is called on an entity with this annotation,
 * instead of physically removing the record, the framework sets this field
 * to {@code true} (and the {@code @DataDeleted} field too, if present).
 * The actual physical removal is deferred to an asynchronous cleanup process.
 *
 * <p>To force immediate physical removal, pass the {@code ForceHardDelete}
 * query option.
 *
 * <p>
 * Example:
 * <pre>{@code
 * public class MyEntity extends OIDGenericDataSoftDelete {
 *
 *     @DataNotRead
 *     @DataAsyncHardDeleted
 *     public Boolean hardDeleted = null;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAsyncHardDeleted {

}
