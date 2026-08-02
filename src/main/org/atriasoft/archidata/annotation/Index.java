package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Declares a MongoDB index on the entity. Repeatable: one annotation per index.
 *
 * <p>
 * Example:
 * {@snippet :
 * &#64;Index({ "companyId", "-createdAt" })
 * &#64;Index(value = "email", unique = true)
 * public class User extends OIDGenericData {
 * 	public ObjectId companyId;
 * 	public String email;
 * }
 * }
 *
 * <p>Field names are the structural ones — the name of {@code @Column(name = "...")} when present,
 * the same names used by {@code FilterValue}. A dotted path addresses an embedded sub-document
 * ({@code "address.city"}). A leading {@code -} makes the key descending.
 *
 * <p>The declaration is checked when the index engine resolves it: an unknown field name fails the
 * synchronization instead of silently producing a useless index.
 *
 * <p>This annotation is inherited: an index declared on a base model applies to every entity
 * extending it.
 *
 * @see Indexed
 * @see org.atriasoft.archidata.index.IndexRegistry
 */
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(IndexList.class)
@Inherited
public @interface Index {
	/**
	 * The indexed fields, in order. Prefix a name with {@code -} for a descending key.
	 * @return the ordered index keys
	 */
	String[] value();

	/**
	 * Explicit index name. Left empty, a canonical name is generated from the keys. The name is
	 * always prefixed by {@code kar_}, which is what tells the engine the index is code-managed.
	 * @return the index name, or an empty string to generate it
	 */
	String name() default "";

	/**
	 * Rejects duplicated values on the indexed keys.
	 * @return {@code true} for a unique index
	 */
	boolean unique() default false;

	/**
	 * Skips the documents that do not carry the indexed fields.
	 * @return {@code true} for a sparse index
	 */
	boolean sparse() default false;

	/**
	 * Delay, in seconds, after which a document is removed by MongoDB (TTL index). Requires a
	 * single date key. {@code -1} disables the TTL.
	 * @return the expiration delay in seconds, or {@code -1}
	 */
	long expireAfterSeconds() default -1;

	/**
	 * Restricts the index to the documents matching this filter, as a JSON document
	 * ({@code "{\"archived\": false}"}). Empty means the whole collection.
	 * @return the partial filter expression, or an empty string
	 */
	String partialFilter() default "";
}
