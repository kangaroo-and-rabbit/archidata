package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.kar.archidata.dataAccess.options.CheckFunctionInterface;
import org.kar.archidata.dataAccess.options.CheckFunctionVoid;

@Target({ ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface DataJson {
	Class<? extends CheckFunctionInterface> checker() default CheckFunctionVoid.class;

	Class<?> targetEntity() default Void.class;
}
