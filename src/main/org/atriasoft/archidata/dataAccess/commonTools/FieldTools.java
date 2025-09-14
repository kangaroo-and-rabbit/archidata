package org.atriasoft.archidata.dataAccess.commonTools;

import java.lang.reflect.Field;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.exception.DataAccessException;

public class FieldTools {

	/**
	 * Set a specific Field at the null value.
	 * @param clazz Class to update in the DB.
	 * @param fieldName Name of the field to reset.
	 */
	public static <T> void setFieldAtNull(final Class<T> clazz, final String fieldName) throws Exception {
		final Field field = clazz.getField(fieldName);
		if (field == null) {
			throw new DataAccessException(
					"Fail to find the file Name:'" + fieldName + "' in class: " + clazz.getCanonicalName());
		}
		final Field primaryKeyField = AnnotationTools.getPrimaryKeyField(clazz);
		if (primaryKeyField == null) {
			throw new DataAccessException(
					"Fail to find the primary field not found in class: " + clazz.getCanonicalName());
		}
		field.setAccessible(true);
		final List<T> data = DataAccess.gets(clazz);
		for (final T elem : data) {
			field.set(elem, null);
			final Object primaryKey = primaryKeyField.get(elem);
			DataAccess.update(elem, primaryKey);
		}
	}
}
