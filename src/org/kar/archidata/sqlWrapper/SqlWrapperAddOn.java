package org.kar.archidata.sqlWrapper;

import java.lang.reflect.Field;

public interface SqlWrapperAddOn {
	/**
	 * Get the Class of the declaration annotation
	 * @return The annotation class
	 */
	Class<?> getAnnotationClass();
	/**
	 * Get the SQL type that is needed to declare for the specific Field Type.
	 * @param elem Field to declare.
	 * @return SQL type to create.
	 */
	String getSQLFieldType(Field elem);
	/**
	 * Check if the field is manage by the local add-on
	 * @param elem Field to inspect.
	 * @return True of the field is manage by the current Add-on.
	 */
	boolean isCompatibleField(Field elem);
	
}
