package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Value written in the database at insertion when the field is {@code null}.
 *
 * <p>The value is a string converted to the type of the field (number, boolean, enum, string,
 * date...); the surrounding single quotes are optional and removed ({@code "'todo'"},
 * {@code "'42'"}). It is written at insertion only; on an update, a {@code null} field that
 * carries {@code @InitValue} is left unchanged in the database instead of being removed. It is not
 * a Java initial value: the object keeps {@code null} until it is read back. The soft-delete marker
 * ({@link DataDeleted}) is always written at insertion, {@code false} without this annotation.
 *
 * <p>Example:
 * {@snippet lang = java :
 * public class Task extends OIDGenericData {
 * 	@InitValue("'todo'")
 * 	public String state;
 * 	@InitValue("'0'")
 * 	public Integer retryCount;
 * }
 * }
 *
 * <p>Replaces {@code jakarta.ws.rs.DefaultValue} on entity fields, which archidata no longer
 * reads. The name differs on purpose: a wrong import would otherwise compile and be ignored. The
 * JAX-RS {@code @DefaultValue} stays the right annotation on the parameters of a REST resource
 * (handled by Jersey).
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface InitValue {
	/**
	 * Value written at insertion when the field is {@code null}, as a quoted string.
	 *
	 * @return the initial value
	 */
	String value();
}
