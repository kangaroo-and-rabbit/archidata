package jakarta.persistence;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Test stand-in for the JPA {@code @Table} (the jakarta.persistence-api dependency is gone): only its
 * package and name matter, to check that archidata rejects the JPA annotations.
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Table {
	/**
	 * The collection name.
	 * @return the name
	 */
	String name() default "";
}
