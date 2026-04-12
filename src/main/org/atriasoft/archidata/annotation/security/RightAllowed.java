package org.atriasoft.archidata.annotation.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.atriasoft.archidata.filter.PartRight;

/**
 * Annotation for fine-grained resource-level access control.
 *
 * <p>
 * When applied to a resource method, the authentication filter verifies that the
 * authenticated user has the required access level for the specified right.
 * Can be used alongside {@link jakarta.annotation.security.RolesAllowed} for
 * combined role and right verification (AND logic).
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}RightAllowed(right = "articles", access = PartRight.READ)
 * public List&lt;Article&gt; getArticles() { ... }
 * </pre>
 */
@Retention(RUNTIME)
@Target({ METHOD })
public @interface RightAllowed {
	/**
	 * The right name to check (e.g., "articles", "users").
	 *
	 * @return the right name
	 */
	String right();

	/**
	 * The required access level for this right.
	 *
	 * @return the required {@link PartRight} access level
	 */
	PartRight access();
}
