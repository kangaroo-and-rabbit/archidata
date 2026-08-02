package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declares a single-key MongoDB index on the annotated property — the short form of an
 * {@link Index} naming that single field.
 *
 * <p>
 * Example:
 * {@snippet :
 * public class User extends OIDGenericData {
 * 	&#64;Indexed(unique = true)
 * 	public String email;
 *
 * 	&#64;Indexed(expireAfterSeconds = 3600)
 * 	public Date sessionExpireAt;
 * }
 * }
 *
 * <p>Note that {@code @Column(unique = true)} does <b>not</b> create any index: {@code @Column}
 * carries JPA information archidata does not act upon. Unicity must be declared here (or with
 * {@link Index}) to be enforced by the database.
 *
 * @see Index
 * @see org.atriasoft.archidata.index.IndexRegistry
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface Indexed {
	/**
	 * Sort order of the key: {@code true} for ascending (default), {@code false} for descending.
	 * @return {@code true} for an ascending key
	 */
	boolean ascending() default true;

	/**
	 * Explicit index name. Left empty, a canonical name is generated. The name is always prefixed
	 * by {@code kar_}, which is what tells the engine the index is code-managed.
	 * @return the index name, or an empty string to generate it
	 */
	String name() default "";

	/**
	 * Rejects duplicated values on the indexed field.
	 * @return {@code true} for a unique index
	 */
	boolean unique() default false;

	/**
	 * Skips the documents that do not carry the field.
	 * @return {@code true} for a sparse index
	 */
	boolean sparse() default false;

	/**
	 * Delay, in seconds, after which a document is removed by MongoDB (TTL index). Requires a date
	 * field. {@code -1} disables the TTL.
	 * @return the expiration delay in seconds, or {@code -1}
	 */
	long expireAfterSeconds() default -1;

	/**
	 * Restricts the index to the documents matching this filter, as a JSON document. Empty means
	 * the whole collection.
	 * @return the partial filter expression, or an empty string
	 */
	String partialFilter() default "";
}
