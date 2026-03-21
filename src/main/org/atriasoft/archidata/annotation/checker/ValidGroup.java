package org.atriasoft.archidata.annotation.checker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the validation groups to apply when validating a parameter or type.
 *
 * <p>This annotation allows selecting which validation groups should be activated
 * during constraint validation.
 */
@Target({ ElementType.PARAMETER, ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGroup {
	/**
	 * The validation groups to activate.
	 * @return an array of validation group classes
	 */
	Class<?>[] value();
}
