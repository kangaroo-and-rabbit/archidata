package org.kar.archidata.annotation.addOn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.kar.archidata.annotation.SQLAddOn;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@SQLAddOn
public @interface SQLTableExternalLink {
	public static String AUTOMATIC = "__auto__";
	
	// If automatic table name, the table name is: parentTableName_externalTableName__link
	String tableName() default AUTOMATIC;
	
	// If automatic table name, the name of the foreign table is manage with the variable name.
	String externalTableName() default AUTOMATIC;
	
	// If the external link have a field to filter with a specific value (name of the field)
	String filterField() default AUTOMATIC;
	
	// If the external link have a field to filter with a specific value (value of the field)
	String filterValue() default AUTOMATIC;
	
}
