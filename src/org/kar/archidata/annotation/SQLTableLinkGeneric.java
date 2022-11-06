package org.kar.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;




@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLTableLinkGeneric {
	public enum ModelLink {
		NONE,
		INTERNAL,
		EXTERNAL
	};
	ModelLink value() default ModelLink.EXTERNAL;
}
