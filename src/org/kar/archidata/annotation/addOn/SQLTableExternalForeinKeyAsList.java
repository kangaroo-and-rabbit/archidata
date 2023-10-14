package org.kar.archidata.annotation.addOn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.kar.archidata.annotation.SQLAddOn;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@SQLAddOn
public @interface SQLTableExternalForeinKeyAsList {
	
}
