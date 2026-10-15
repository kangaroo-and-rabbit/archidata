package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Name of the field in the MongoDB document, when it differs from the Java field name.
 *
 * <p>This annotation only renames the field in the database: it carries no constraint. Use Bean
 * Validation for the checks ({@code @NotNull}, {@code @Size}...) and {@link Indexed}/{@link Index}
 * for the unicity. The name is read on the Java field (on a record, put it on the component,
 * which propagates it to the field). The JSON name used by the REST API is not affected (Jackson
 * keeps the Java name).
 *
 * <p>Example:
 * {@snippet lang = java :
 * public class User extends OIDGenericData {
 * 	@Field("full_name")
 * 	public String fullName;
 * }
 * }
 *
 * <p>Filters and sorts written with method references ({@code Fields.of(User::getFullName)})
 * resolve this name. Replaces {@code jakarta.persistence.Column(name = "...")}, which archidata
 * no longer reads.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Field {
	/**
	 * Name of the field in the MongoDB document.
	 *
	 * @return the database field name
	 */
	String value();
}
