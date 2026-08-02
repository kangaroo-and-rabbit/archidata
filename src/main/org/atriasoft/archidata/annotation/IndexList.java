package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Container of the repeatable {@link Index} annotation. Never written by hand: declare several
 * {@code @Index} on the class instead.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface IndexList {
	/**
	 * The declared indexes.
	 * @return the indexes of the entity
	 */
	Index[] value();
}
