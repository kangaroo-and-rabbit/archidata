package org.atriasoft.archidata.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * In NoSql entity the relation is stored in the 2 part of the entity,
 * then it is needed to define the field that store the relation data value in the remote elements.
 */
@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ManyToManyNoSQL {
	/**
	 * The entity class that is the target of the
	 * association.
	 */
	Class<?> targetEntity();

	/**
	 * The field that owns the revert value. empty if the relationship is unidirectional.
	 */
	String remoteField();
}
